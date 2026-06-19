package com.lonewolf.wavvy.ui.auth

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

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
            // Enable and persist cookies across sessions
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
                        val url = request?.url?.toString()
                        if (url != null && url.startsWith(redirectUri)) {
                            checkRedirect(url, redirectUri, onTokenCaptured, onErrorReceived)
                            return true
                        }
                        return false
                    }
                }

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    supportZoom()

                    val defaultUserAgent = userAgentString
                    val customUserAgent = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36"
                    userAgentString = customUserAgent
                }

                loadUrl(authUrl)
            }
        }
    )
}

// Intercepts framework parameters during execution
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
            val uri = Uri.parse(url)
            val error = uri.getQueryParameter("error")
            if (error != null) {
                onErrorReceived()
            }
        }
    }
}
