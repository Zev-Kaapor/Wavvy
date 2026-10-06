package com.wavvy.app.features.auth.ui

// Android utilities and WebKit components
import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
// Compose layouts and foundations
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
// Material 3 components
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
// Core utilities and coroutines
import androidx.core.net.toUri
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.GradientButton
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.features.auth.data.AccountClient
import com.wavvy.app.features.auth.data.AccountSession
import com.wavvy.app.features.auth.data.MusicOrigin
import com.wavvy.app.features.auth.ui.components.RoundBackButton
import java.util.Locale

// Sign in page of Google that sends the user to YouTube Music, and the host and cookies that prove the sign in worked
private const val LoginUrl = "https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com%2F"
private const val MusicHost = "music.youtube.com"
private val SessionCookies = listOf("SAPISID=", "__Secure-3PAPISID=")

// What the screen is doing
private enum class Phase { Web, Working, Error }

// Google sign in inside the app, only to bring the profile photo
@Composable
fun GoogleLoginScreen(
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dimens = WavvyTheme.dimens
    val scope = rememberCoroutineScope()
    var phase by rememberSaveable { mutableStateOf(Phase.Web) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    // Leaving without signing in clears the session of the page, so no half done sign in stays in the app
    val cancel: () -> Unit = {
        AccountSession.clearWebSession()
        onBack()
    }

    // The page goes back first, and the screen only after there is no page left
    BackHandler {
        val page = webView
        if (phase == Phase.Web && page != null && page.canGoBack()) page.goBack() else cancel()
    }

    // Reads the account from the session and keeps its photo, the session stays so the photo can be checked again later
    fun finishWith(cookies: String) {
        phase = Phase.Working
        scope.launch {
            val account = AccountClient.fetchAccount(cookies).getOrNull()
            val saved = account != null && AccountSession.apply(context, account).isSuccess

            if (saved) onSuccess() else phase = Phase.Error
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar, it also covers the area under the cutout
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(WavvyTheme.colors.loginBar)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
        ) {
            RoundBackButton(
                onClick = cancel,
                modifier = Modifier.padding(horizontal = dimens.screenPadding, vertical = dimens.spaceSmall)
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)),
            contentAlignment = Alignment.Center
        ) {
            when (phase) {
                Phase.Web -> SignInPage(
                    onPageReady = { webView = it },
                    onSession = ::finishWith,
                    onFailure = { phase = Phase.Error },
                    modifier = Modifier.fillMaxSize()
                )

                Phase.Working -> CircularProgressIndicator()

                Phase.Error -> Column(
                    modifier = Modifier
                        .widthIn(max = AuthDimens.ContentMaxWidth)
                        .padding(horizontal = dimens.screenPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.login_error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(dimens.spaceLarge))

                    GradientButton(text = stringResource(R.string.login_retry), onClick = { phase = Phase.Web })
                }
            }
        }
    }
}

// Web page of the sign in, it calls onSession when the user lands on YouTube Music already signed in
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun SignInPage(
    onPageReady: (WebView) -> Unit,
    onSession: (String) -> Unit,
    onFailure: () -> Unit,
    modifier: Modifier = Modifier
) {
    val language = remember { Locale.getDefault().toLanguageTag() }
    var delivered by remember { mutableStateOf(false) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            CookieManager.getInstance().apply { setAcceptCookie(true) }

            WebView(context).apply {
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = false
                    allowContentAccess = false
                }

                webViewClient = object : WebViewClient() {
                    // Only secure pages open, other schemes such as app links are ignored
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean =
                        request?.url?.scheme != "https"

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        CookieManager.getInstance().flush()

                        val onMusic = url?.toUri()?.host == MusicHost
                        val cookies = CookieManager.getInstance().getCookie(MusicOrigin).orEmpty()
                        if (onMusic && !delivered && SessionCookies.any { it in cookies }) {
                            delivered = true
                            onSession(cookies)
                        }
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        if (request?.isForMainFrame == true) onFailure()
                    }
                }

                onPageReady(this)
                loadUrl("$LoginUrl&hl=$language")
            }
        },
        onRelease = { it.destroy() }
    )
}
