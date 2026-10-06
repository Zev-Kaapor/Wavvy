package com.wavvy.app.core.playback.potoken

// Android context, threads and WebView
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.MainThread
import androidx.collection.ArrayMap
// Coroutines
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
// Networking
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
// Java time and concurrency
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Hidden WebView that runs BotGuard and mints the tokens YouTube asks for before it serves a stream, adapted from Metrolist (GPL-3.0)
class PoTokenWebView private constructor(
    context: Context,
    // Resumed exactly once, when the page is ready or failed to start
    private val continuation: Continuation<PoTokenWebView>
) {
    private val webView = WebView(context)
    private val scope = MainScope()

    // Start errors can come from several places, only the first one resumes the start
    private val initResumed = AtomicBoolean(false)

    @Volatile
    private var closed = false

    // Set when the page died or stopped answering, so the next call builds a new one
    @Volatile
    var isDead: Boolean = false
        private set

    // Waiting calls, each with its own key so two calls for the same video never mix
    private val poTokenContinuations = Collections.synchronizedMap(ArrayMap<String, Continuation<String>>())
    private val requestCounter = AtomicLong()
    private val exceptionHandler = CoroutineExceptionHandler { _, error -> onInitializationErrorCloseAndCancel(error) }
    private lateinit var expirationInstant: Instant

    init {
        val settings = webView.settings
        //noinspection SetJavaScriptEnabled the page is JavaScript only
        settings.javaScriptEnabled = true
        settings.userAgentString = UserAgent
        // The page itself never goes to the internet, the app does the requests
        settings.blockNetworkLoads = true

        webView.addJavascriptInterface(this, JsInterface)

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                val text = message.message()
                if (text.contains("Uncaught")) {
                    val described = "\"$text\", source: ${message.sourceId()} (${message.lineNumber()})"
                    if (initResumed.get()) {
                        // After the start an error comes from the scripts of Google, a new page fixes it
                        isDead = true
                        val exception = PoTokenException(described)
                        close()
                        popAllPoTokenContinuations().forEach { (_, waiting) ->
                            runCatching { waiting.resumeWithException(exception) }
                        }
                    } else {
                        val exception = BadWebViewException(described)
                        onInitializationErrorCloseAndCancel(exception)
                        popAllPoTokenContinuations().forEach { (_, waiting) ->
                            runCatching { waiting.resumeWithException(exception) }
                        }
                    }
                }
                return super.onConsoleMessage(message)
            }
        }

        webView.webViewClient = object : WebViewClient() {
            // The system killed the page, a new one is built on the next call
            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                isDead = true
                val exception = PoTokenException("WebView render process gone")
                onInitializationErrorCloseAndCancel(exception)
                popAllPoTokenContinuations().forEach { (_, waiting) ->
                    runCatching { waiting.resumeWithException(exception) }
                }
                return true
            }
        }
    }

    // Loads the page, which then calls downloadAndRunBotguard
    private fun loadHtmlAndObtainBotguard() {
        scope.launch(exceptionHandler) {
            val html = withContext(Dispatchers.IO) {
                webView.context.assets.open(PageAsset).bufferedReader().use { it.readText() }
            }
            val data = html.replaceFirst("</script>", "\n$JsInterface.downloadAndRunBotguard()</script>")
            webView.loadDataWithBaseURL(YouTubeOrigin, data, "text/html", "utf-8", null)
        }
    }

    // Asks YouTube for the challenge and runs it in the page
    @JavascriptInterface
    fun downloadAndRunBotguard() {
        makeBotguardServiceRequest(CreateUrl, "[ \"$RequestKey\" ]") { responseBody ->
            val challenge = parseChallengeData(responseBody)
            webView.evaluateJavascript(
                """try {
                    data = $challenge
                    runBotGuard(data).then(function (result) {
                        this.webPoSignalOutput = result.webPoSignalOutput
                        $JsInterface.onRunBotguardResult(result.botguardResponse)
                    }, function (error) {
                        $JsInterface.onJsInitializationError(error + "\n" + error.stack)
                    })
                } catch (error) {
                    $JsInterface.onJsInitializationError(error + "\n" + error.stack)
                }""",
                null
            )
        }
    }

    // The page could not start
    @JavascriptInterface
    fun onJsInitializationError(error: String) {
        onInitializationErrorCloseAndCancel(buildExceptionForJsError(error))
    }

    // Trades the BotGuard answer for an integrity token and builds the minter in the page
    @JavascriptInterface
    fun onRunBotguardResult(botguardResponse: String) {
        makeBotguardServiceRequest(GenerateItUrl, "[ \"$RequestKey\", \"$botguardResponse\" ]") { responseBody ->
            try {
                val (integrityToken, expirationSeconds) = parseIntegrityTokenData(responseBody)
                // A margin before the real expiration, so a token is never used at its last second
                expirationInstant = Instant.now().plusSeconds(expirationSeconds).minus(ExpirationMarginMinutes, ChronoUnit.MINUTES)

                webView.evaluateJavascript(
                    """try {
                        this.integrityToken = $integrityToken
                        createPoTokenMinter(webPoSignalOutput, integrityToken).then(function() {
                            $JsInterface.onMinterCreated()
                        }).catch(function(error) {
                            $JsInterface.onJsInitializationError(error + "\n" + (error.stack || ''))
                        })
                    } catch (error) {
                        $JsInterface.onJsInitializationError(error + "\n" + error.stack)
                    }""",
                    null
                )
            } catch (error: Exception) {
                onInitializationErrorCloseAndCancel(PoTokenException("parseIntegrityTokenData failed: ${error.message}"))
            }
        }
    }

    // The page is ready to mint tokens
    @JavascriptInterface
    fun onMinterCreated() {
        if (initResumed.compareAndSet(false, true)) continuation.resume(this)
    }

    // Mints a token tied to a video or to the visitor
    suspend fun generatePoToken(identifier: String): String {
        if (isDead || closed) throw PoTokenException("PoToken WebView is dead or closed")

        val requestKey = "$identifier#${requestCounter.incrementAndGet()}"
        return try {
            withTimeout(GenerateTimeoutMs) { generatePoTokenInternal(identifier, requestKey) }
        } catch (error: TimeoutCancellationException) {
            // A page that never answers is stuck, the next call builds a new one
            isDead = true
            popPoTokenContinuation(requestKey)
            throw PoTokenException("poToken generation timed out")
        }
    }

    // Runs the minter of the page for one call
    private suspend fun generatePoTokenInternal(identifier: String, requestKey: String): String =
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { waiting ->
                poTokenContinuations[requestKey] = waiting
                webView.evaluateJavascript(
                    """(function() {
                        var requestKey = "$requestKey"
                        try {
                            var u8Identifier = ${stringToU8(identifier)}
                            obtainPoToken(u8Identifier).then(function(poTokenU8) {
                                $JsInterface.onObtainPoTokenResult(requestKey, poTokenU8.join(","))
                            }).catch(function(error) {
                                $JsInterface.onObtainPoTokenError(requestKey, error + "\n" + (error.stack || ''))
                            })
                        } catch (error) {
                            $JsInterface.onObtainPoTokenError(requestKey, error + "\n" + error.stack)
                        }
                    })()""",
                    null
                )
            }
        }

    // The minter failed for one call, the page itself is still fine
    @JavascriptInterface
    fun onObtainPoTokenError(requestKey: String, error: String) {
        popPoTokenContinuation(requestKey)?.resumeWithException(PoTokenException(error))
    }

    // The minter answered one call
    @JavascriptInterface
    fun onObtainPoTokenResult(requestKey: String, poTokenU8: String) {
        val poToken = try {
            u8ToBase64(poTokenU8)
        } catch (error: Throwable) {
            popPoTokenContinuation(requestKey)?.resumeWithException(error)
            return
        }
        popPoTokenContinuation(requestKey)?.resume(poToken)
    }

    // True when the integrity token of the page is too old
    val isExpired: Boolean
        get() = Instant.now().isAfter(expirationInstant)

    // Takes one waiting call out
    private fun popPoTokenContinuation(key: String): Continuation<String>? = poTokenContinuations.remove(key)

    // Takes every waiting call out
    private fun popAllPoTokenContinuations(): Map<String, Continuation<String>> {
        val result = poTokenContinuations.toMap()
        poTokenContinuations.clear()
        return result
    }

    // Sends a request to the BotGuard service of YouTube and hands the answer on
    private fun makeBotguardServiceRequest(url: String, data: String, handleResponseBody: (String) -> Unit) {
        scope.launch(exceptionHandler) {
            val request = okhttp3.Request.Builder()
                .post(data.toRequestBody())
                .headers(
                    mapOf(
                        "User-Agent" to UserAgent,
                        "Accept" to "application/json",
                        "Content-Type" to "application/json+protobuf",
                        "x-goog-api-key" to GoogleApiKey,
                        "x-user-agent" to "grpc-web-javascript/0.1"
                    ).toHeaders()
                )
                .url(url)
                .build()
            val (code, body) = withContext(Dispatchers.IO) {
                httpClient.newCall(request).execute().use { response ->
                    response.code to if (response.isSuccessful) response.body.string() else null
                }
            }
            if (body.isNullOrEmpty()) {
                onInitializationErrorCloseAndCancel(PoTokenException("Invalid botguard response (code=$code)"))
            } else {
                handleResponseBody(body)
            }
        }
    }

    // Closes the page and fails the start when it was still waiting
    private fun onInitializationErrorCloseAndCancel(error: Throwable) {
        close()
        if (initResumed.compareAndSet(false, true)) runCatching { continuation.resumeWithException(error) }
    }

    // Frees the page, from any thread
    fun close() {
        if (closed) return
        closed = true
        scope.cancel()

        if (Looper.myLooper() == Looper.getMainLooper()) destroyWebView() else Handler(Looper.getMainLooper()).post { destroyWebView() }
    }

    // A page that crashed may throw while it is torn down, which is ignored
    @MainThread
    private fun destroyWebView() {
        runCatching {
            webView.clearHistory()
            webView.clearCache(true)
            webView.loadUrl("about:blank")
            webView.onPause()
            webView.removeAllViews()
            webView.destroy()
        }
    }

    companion object {
        private const val YouTubeOrigin = "https://www.youtube.com"
        private const val CreateUrl = "$YouTubeOrigin/api/jnn/v1/Create"
        private const val GenerateItUrl = "$YouTubeOrigin/api/jnn/v1/GenerateIT"
        private const val GoogleApiKey = "AIzaSyDyT5W0Jh49F30Pqqtyfdf7pDLFKLJoAnw"
        private const val RequestKey = "O43z0dpjhgX20SCx4KAo"
        private const val UserAgent =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.3"
        private const val JsInterface = "PoTokenWebView"
        private const val PageAsset = "po_token.html"
        private const val ExpirationMarginMinutes = 10L

        // Starting needs two network trips and the script, minting takes well under a second
        private const val InitTimeoutMs = 45_000L
        private const val GenerateTimeoutMs = 15_000L

        private val httpClient = OkHttpClient()

        // Builds a new page and waits until it is ready to mint
        suspend fun getNewPoTokenGenerator(context: Context): PoTokenWebView {
            var created: PoTokenWebView? = null
            try {
                return withTimeout(InitTimeoutMs) {
                    withContext(Dispatchers.Main) {
                        suspendCancellableCoroutine { waiting ->
                            val page = PoTokenWebView(context, waiting)
                            created = page
                            page.loadHtmlAndObtainBotguard()
                        }
                    }
                }
            } catch (error: TimeoutCancellationException) {
                closeQuietly(created)
                throw PoTokenException("PoTokenWebView init timed out")
            } catch (error: CancellationException) {
                closeQuietly(created)
                throw error
            }
        }

        // Closes a page that never finished starting, so a late answer cannot resume it
        private suspend fun closeQuietly(page: PoTokenWebView?) {
            if (page == null) return
            withContext(NonCancellable + Dispatchers.Main) {
                page.initResumed.set(true)
                page.close()
            }
        }
    }
}
