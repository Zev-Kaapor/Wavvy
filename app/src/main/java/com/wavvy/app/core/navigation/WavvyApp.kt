package com.wavvy.app.core.navigation

// Android WebKit and Compose animation
import android.webkit.CookieManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
// Material 3 components
import androidx.compose.material3.MaterialTheme
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
// UI utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
// Coroutines and reactive flows
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.core.designsystem.theme.ThemeMode
import com.wavvy.app.core.designsystem.theme.WavvyMotion
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.features.auth.data.AccountClient
import com.wavvy.app.features.auth.data.Entry
import com.wavvy.app.features.auth.data.EntryStore
import com.wavvy.app.features.auth.data.MusicOrigin
import com.wavvy.app.features.auth.data.ProfilePhotoStore
import com.wavvy.app.features.auth.ui.GoogleLoginScreen
import com.wavvy.app.features.auth.ui.LocalProfilePhoto
import com.wavvy.app.features.auth.ui.LoginScreen
import com.wavvy.app.features.auth.ui.WelcomeScreen

// Where the user is, before the app opens
private enum class Stage { Welcome, Login, GoogleLogin, Main }

// Welcome, then the login, then the app, with a short fade between them, straight to the app when the user is already in
@Composable
fun WavvyApp() {
    val context = LocalContext.current
    val entryStore = remember { EntryStore(context) }
    val scope = rememberCoroutineScope()

    // Empty until the saved entry is read, so the welcome does not flash for a user who is already in
    var stage by rememberSaveable { mutableStateOf<Stage?>(null) }

    LaunchedEffect(stage == null) {
        if (stage == null) {
            stage = if (entryStore.entry.first() != null) Stage.Main else Stage.Welcome
        }
    }

    // Counts the times the photo on the device changed, so it is read again
    var photoVersion by remember { mutableIntStateOf(0) }

    // Photo of the account, read each time the app opens or the user comes in
    val profilePhoto by produceState<ImageBitmap?>(initialValue = null, stage == Stage.Main, photoVersion) {
        value = if (stage == Stage.Main) ProfilePhotoStore.load(context) else null
    }

    // Each time the app opens, a user signed in with Google gets the photo checked, a failed check keeps the current photo
    LaunchedEffect(stage == Stage.Main) {
        if (stage == Stage.Main && entryStore.entry.first() == Entry.Google) {
            val cookies = CookieManager.getInstance().getCookie(MusicOrigin)
            val account = cookies?.takeIf { it.isNotBlank() }?.let { AccountClient.fetchAccount(it).getOrNull() }
            if (account != null && ProfilePhotoStore.save(context, account.photoUrl).getOrDefault(false)) photoVersion++
        }
    }

    BackHandler(enabled = stage == Stage.Login) { stage = Stage.Welcome }

    CompositionLocalProvider(LocalProfilePhoto provides profilePhoto) {
        Crossfade(
            targetState = stage,
            animationSpec = tween(WavvyMotion.ScreenFadeMillis),
            label = "AppStage"
        ) { current ->
            when (current) {
                null -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                )

                // The welcome and the login are always dark, over a photo
                Stage.Welcome -> WavvyTheme(themeMode = ThemeMode.DARK) {
                    WelcomeScreen(onStart = { stage = Stage.Login })
                }

                Stage.Login -> WavvyTheme(themeMode = ThemeMode.DARK) {
                    LoginScreen(
                        onGoogleClick = { stage = Stage.GoogleLogin },
                        onSkipClick = {
                            scope.launch { entryStore.save(Entry.Guest) }
                            stage = Stage.Main
                        },
                        onBackClick = { stage = Stage.Welcome }
                    )
                }

                Stage.GoogleLogin -> GoogleLoginScreen(
                    onSuccess = {
                        scope.launch { entryStore.save(Entry.Google) }
                        stage = Stage.Main
                    },
                    onBack = { stage = Stage.Login }
                )

                Stage.Main -> MainScaffold()
            }
        }
    }
}
