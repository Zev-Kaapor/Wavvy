package com.wavvy.app.features.auth.ui.components

// Android utilities and WebKit components
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
// Compose UI integration
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri

// Embedded authentication webview
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EmbeddedAuthWebView(
    authUrl: String,
    redirectUri: String,
    onTokenCaptured: (String) -> Unit,
    onErrorReceived: () -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            CookieManager.getInstance().apply {
                setAcceptCookie(true)
            }

            WebView(context).apply {
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        url?.let { checkRedirect(it, redirectUri, onTokenCaptured, onErrorReceived) }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        CookieManager.getInstance().flush()
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val url = request?.url?.toString() ?: return false
                        // Only intercept final redirect to music.youtube.com, let Google login flow normally
                        if (url.startsWith(redirectUri)) {
                            checkRedirect(url, redirectUri, onTokenCaptured, onErrorReceived)
                            return true
                        }
                        return false
                    }
                }

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    javaScriptCanOpenWindowsAutomatically = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    supportZoom()
                    // Clean Chrome UA - removes WebView detection signals
                    userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                }

                // Clear X-Requested-With on initial load to avoid Google blocking embedded WebViews
                val headers = mutableMapOf<String, String>()
                headers["X-Requested-With"] = ""
                loadUrl(authUrl, headers)
            }
        }
    )
}

// Redirect URL handler
private fun checkRedirect(
    url: String,
    redirectUri: String,
    onTokenCaptured: (String) -> Unit,
    onErrorReceived: () -> Unit
) {
    if (url.startsWith(redirectUri)) {
        val cookieManager = CookieManager.getInstance()
        val cookies = cookieManager.getCookie(url)

        if (!cookies.isNullOrBlank() && cookies.contains("SAPISID")) {
            cookieManager.flush()
            onTokenCaptured(cookies)
        } else {
            val uri = url.toUri()
            val error = uri.getQueryParameter("error")
            if (error != null) {
                onErrorReceived()
            }
        }
    }
}
