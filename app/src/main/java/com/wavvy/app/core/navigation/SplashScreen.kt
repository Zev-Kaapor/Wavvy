package com.wavvy.app.core.navigation

// Compose animations
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and lifecycle hooks
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Asynchronous coroutine utilities
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
// Local data storage and design system
import com.wavvy.app.core.data.local.SettingsStorage
import com.wavvy.app.core.designsystem.WordmarkLayoutSpec
import com.wavvy.app.features.auth.data.AuthRepositoryImpl

// Splash screen constants
private const val SPLASH_FONT_SIZE = 42f
private const val DOCKED_FONT_SIZE = 32f

// Splash screen view
@Composable
fun SplashScreen(
    onInitializationComplete: (isLoggedIn: Boolean) -> Unit
) {
    val alphaAnimation = remember { Animatable(0f) }
    val fontSizeAnimation = remember { Animatable(SPLASH_FONT_SIZE) }
    val context = LocalContext.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .displayCutoutPadding()
    ) {
        val centeredOffset = remember(maxHeight) { maxHeight / 2 - 24.dp }
        val offsetAnimation = remember { Animatable(centeredOffset, Dp.VectorConverter) }
        val dockedOffset = remember(maxHeight) { WordmarkLayoutSpec.dockedOffset(maxHeight) }

        // Startup animation and auth check
        LaunchedEffect(Unit) {
            val repository = AuthRepositoryImpl(context)

            // Fade in animation
            alphaAnimation.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500)
            )

            // Hold duration
            delay(1500.milliseconds)

            val token = repository.getSessionToken()
            val settingsStorage = SettingsStorage(context)
            val savedAccounts = repository.savedAccountsManager.getSavedAccounts()
            val isGuestActive = settingsStorage.isGuestActive()

            val isLoggedIn = (!token.isNullOrEmpty() && savedAccounts.isNotEmpty()) || isGuestActive

            if (!isLoggedIn) {
                // Dock wordmark animation
                coroutineScope {
                    launch {
                        offsetAnimation.animateTo(
                            targetValue = dockedOffset,
                            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
                        )
                    }
                    launch {
                        fontSizeAnimation.animateTo(
                            targetValue = DOCKED_FONT_SIZE,
                            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
                        )
                    }
                }
            }

            onInitializationComplete(isLoggedIn)
        }

        // Branding text
        Text(
            text = "Wavvy",
            fontSize = fontSizeAnimation.value.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset { IntOffset(x = 0, y = offsetAnimation.value.roundToPx()) }
                .alpha(alphaAnimation.value)
        )
    }
}
