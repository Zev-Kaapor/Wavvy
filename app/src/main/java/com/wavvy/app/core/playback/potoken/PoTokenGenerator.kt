package com.wavvy.app.core.playback.potoken

// Android context and web storage
import android.content.Context
import android.webkit.CookieManager
// Coroutines
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

// Keeps one token page alive and gets the two tokens of a stream from it, adapted from Metrolist (GPL-3.0)
class PoTokenGenerator(context: Context) {
    private val appContext = context.applicationContext

    // Devices without a working WebView play without tokens
    private val webViewSupported by lazy { runCatching { CookieManager.getInstance() }.isSuccess }
    private var webViewBadImpl = false

    private val lock = Mutex()
    private var sessionId: String? = null
    private var streamingPot: String? = null
    private var generator: PoTokenWebView? = null

    // Tokens for a video and a visitor, empty when the WebView cannot make them in time, so playback tries the clients that do not need them
    suspend fun getWebClientPoToken(videoId: String, sessionId: String): PoTokenResult? {
        if (!webViewSupported || webViewBadImpl) return null

        return try {
            withTimeout(TimeoutMs) { getWebClientPoToken(videoId, sessionId, forceRecreate = false) }
        } catch (error: TimeoutCancellationException) {
            clearGenerator()
            null
        } catch (error: CancellationException) {
            throw error
        } catch (error: BadWebViewException) {
            webViewBadImpl = true
            null
        }
    }

    // Frees the token page
    suspend fun close() {
        clearGenerator()
    }

    // Closes the page and forgets its tokens
    private suspend fun clearGenerator() {
        lock.withLock {
            runCatching { withContext(Dispatchers.Main) { generator?.close() } }
            generator = null
            streamingPot = null
            sessionId = null
        }
    }

    // Builds a new page when there is none, it expired, died or belongs to another visitor, then mints the token of the video
    private suspend fun getWebClientPoToken(videoId: String, sessionId: String, forceRecreate: Boolean): PoTokenResult {
        val (page, pot, recreated) = lock.withLock {
            val current = generator
            val shouldRecreate = forceRecreate || current == null || current.isExpired || current.isDead || this.sessionId != sessionId

            if (shouldRecreate) {
                withContext(Dispatchers.Main) { generator?.close() }

                // Forgotten before the steps that can fail, so a failure is never paired with old tokens
                generator = null
                streamingPot = null
                this.sessionId = null

                val newGenerator = PoTokenWebView.getNewPoTokenGenerator(appContext)

                // The token of the visitor has to be minted once before any token of a video
                val newStreamingPot = try {
                    newGenerator.generatePoToken(sessionId)
                } catch (error: Throwable) {
                    runCatching { newGenerator.close() }
                    throw error
                }

                generator = newGenerator
                streamingPot = newStreamingPot
                this.sessionId = sessionId
            }

            Triple(generator!!, streamingPot!!, shouldRecreate)
        }

        val playerPot = try {
            page.generatePoToken(videoId)
        } catch (error: Throwable) {
            // A page that was just built cannot be helped, an older one is built again once
            if (recreated) throw error
            return getWebClientPoToken(videoId = videoId, sessionId = sessionId, forceRecreate = true)
        }

        return PoTokenResult(playerRequestPoToken = pot, streamingDataPoToken = playerPot)
    }

    private companion object {
        // A cold start of the page takes a few seconds, after this playback goes on without tokens
        const val TimeoutMs = 8_000L
    }
}
