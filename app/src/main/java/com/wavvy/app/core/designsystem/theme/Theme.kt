package com.wavvy.app.core.designsystem.theme

// Android lifecycle and components
import android.app.Activity
// Compose animations and foundations
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
// Compose design system
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
// Compose runtime and graphics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Brand palette
private val PureBlack = Color(0xFF000000)
private val RichBlack = Color(0xFF0C0C12)
private val GhostWhite = Color(0xFFF4F4F7)
private val PremiumWhite = Color(0xFFFAFAFC)
private val MutedSlate = Color(0xFF252530)

val ElectricCyan = Color(0xFF00B2FF)
private val DeepCyan = Color(0xFF0088CC)
private val VibrantPurple = Color(0xFF7C4DFF)
val googleSignInBackground = Color(0xFF0E0E0E)

// Light theme definition
private val LightColors = lightColorScheme(
    primary = Color(0xFF1A1A24),
    onPrimary = PremiumWhite,
    tertiary = DeepCyan,
    background = GhostWhite,
    onBackground = Color(0xFF0A0A0F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A24),
    surfaceVariant = Color(0xFFE2E2E8),
    onSurfaceVariant = Color(0xFF48484A),
    primaryContainer = Color(0xFFDFDFE5),
    secondaryContainer = Color(0xFFD1D1D8),
    error = Color(0xFFFF3B30),
    onError = PremiumWhite
)

// Dark theme definition
private val DarkColors = darkColorScheme(
    primary = PremiumWhite,
    onPrimary = PureBlack,
    tertiary = ElectricCyan,
    background = PureBlack,
    onBackground = PremiumWhite,
    surface = RichBlack,
    onSurface = PremiumWhite,
    surfaceVariant = RichBlack,
    onSurfaceVariant = Color(0xFFA1A1AA),
    primaryContainer = RichBlack,
    secondaryContainer = MutedSlate,
    error = Color(0xFFFF453A),
    onError = PremiumWhite
)

// Global gradients
object CustomGradients {
    val WavvyLight = listOf(Color(0xFF0088CC), Color(0xFF00A2EE), Color(0xFF4A69FF))
    val WavvyDark = listOf(Color(0xFF00B2FF), Color(0xFF0088FF), Color(0xFF6200EE))
}

// Genre-specific visual identities
object GenreGradients {
    private val StartOffset = Offset(0f, 0f)
    private val EndOffset = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)

    private fun staticGradient(start: Color, center: Color, end: Color): Brush {
        return Brush.linearGradient(
            colors = listOf(start, center, end),
            start = StartOffset,
            end = EndOffset
        )
    }

    // Core & Heavy
    val pop = staticGradient(Color(0xFFFF2A6D), Color(0xFF9B00E8), Color(0xFF6200EE))
    val rock = staticGradient(Color(0xFF2E2E3A), Color(0xFF1C1C24), Color(0xFF0C0C12))
    val metal = staticGradient(Color(0xFF4F4F5A), Color(0xFF2D2D35), Color(0xFF121214))
    val punk = staticGradient(Color(0xFFFF0055), Color(0xFFC0003A), Color(0xFF6B001C))
    val hardrock = staticGradient(Color(0xFF880E4F), Color(0xFF4A001F), Color(0xFF1A000A))

    // Urban & Rhythm
    val hiphop = staticGradient(Color(0xFFFF9100), Color(0xFFFF5500), Color(0xFFD50000))
    val rnb = staticGradient(Color(0xFF7B1FA2), Color(0xFF4A148C), Color(0xFF26004D))
    val trap = staticGradient(Color(0xFF455A64), Color(0xFF263238), Color(0xFF10171A))
    val phonk = staticGradient(Color(0xFF4A148C), Color(0xFF2A085C), Color(0xFF0F0026))

    // Electronic & Vibe
    val electronic = staticGradient(Color(0xFF00B2FF), Color(0xFF0066FF), Color(0xFF311B92))
    val indie = staticGradient(Color(0xFF00E676), Color(0xFF00BFA5), Color(0xFF00796B))
    val lofi = staticGradient(Color(0xFF9575CD), Color(0xFF673AB7), LazyValueHolder.LofiEnd)
    val ambient = staticGradient(Color(0xFF00E5FF), Color(0xFF00B8D4), Color(0xFF006064))

    // Classy genres
    val jazz = staticGradient(Color(0xFF5C6BC0), Color(0xFF3F51B5), Color(0xFF1A237E))
    val soul = staticGradient(Color(0xFFFF8F00), Color(0xFFD84315), Color(0xFF4E342E))

    // World & Regional
    val flamenco = staticGradient(Color(0xFFFF5252), Color(0xFFD32F2F), Color(0xFF7F0000))
    val arabic = staticGradient(Color(0xFFFFB300), Color(0xFFF57C00), Color(0xFFE65100))
    val greek = staticGradient(Color(0xFF4FC3F7), Color(0xFF0288D1), Color(0xFF0A47A1))
    val mpb = staticGradient(Color(0xFF81C784), Color(0xFF388E3C), Color(0xFF1B5E20))
    val funk = staticGradient(Color(0xFFE040FB), Color(0xFF9C27B0), Color(0xFF4A148C))
    val sertanejo = staticGradient(Color(0xFFA1887F), Color(0xFF795548), Color(0xFF4E342E))
    val pagode = staticGradient(Color(0xFFFFA726), Color(0xFFF57C00), Color(0xFFE65100))
    val rapNacional = staticGradient(Color(0xFF78909C), Color(0xFF455A64), Color(0xFF263238))
    val reggaeton = staticGradient(Color(0xFFFF4081), Color(0xFFE91E63), Color(0xFF880E4F))
    val afrobeat = staticGradient(Color(0xFFFFC107), Color(0xFFFF8F00), Color(0xFFD84315))
    val reggae = staticGradient(Color(0xFF4CAF50), Color(0xFFFFEB3B), Color(0xFFF44336))

    // Asian scene
    val kpop = staticGradient(Color(0xFFFF4081), Color(0xFFD500F9), Color(0xFF651FFF))
    val jpop = staticGradient(Color(0xFFFF80AB), Color(0xFFFF4081), Color(0xFFC2185B))
    val cpop = staticGradient(Color(0xFFFF1744), Color(0xFFD50000), Color(0xFFFF9100))
    val hindustani = staticGradient(Color(0xFFFF9800), Color(0xFFE65100), Color(0xFF9C27B0))

    // Aesthetic movements
    val vaporwave = staticGradient(Color(0xFFF48FB1), Color(0xFFE1BEE7), Color(0xFF00B2FF))
    val synthwave = staticGradient(Color(0xFFFF007F), Color(0xFF7B1FA2), Color(0xFF00B2FF))
    val citypop = staticGradient(Color(0xFFFF8A80), Color(0xFF64B5F6), Color(0xFF0D47A1))
    val darkwave = staticGradient(Color(0xFF4A148C), Color(0xFF1A0033), Color(0xFF000000))
    val dreamcore = staticGradient(Color(0xFFCE93D8), Color(0xFF8E24AA), Color(0xFF311B92))
    val chillwave = staticGradient(Color(0xFF80CBC4), Color(0xFF4DB6AC), Color(0xFF004D40))
}

// Lazy initialization properties
private object LazyValueHolder {
    val LofiEnd = Color(0xFF311B92)
}

// Feature chips colors
object DiscoveryChipColors {
    val trending = Color(0xFF00A620)
    val top50 = Color(0xFF9C27B0)
    val releases = Color(0xFFFF1744)
    val mixes = Color(0xFFFFAB00)
    val community = Color(0xFF2979FF)
    val radios = Color(0xFFFF6D00)
    val playlists = Color(0xFF00B2FF)
}

// Playback indicators
object MusicStateColors {
    val playing = Color(0xFF00B2FF)
    val paused = Color(0xFF8E8E93)
    val liked = Color(0xFFFF2D55)
    val downloaded = Color(0xFF00E676)
}

// Theme extensions resolved by dynamic color scheme luminance
@Suppress("UnusedReceiverParameter")
val MaterialTheme.WavvyGradient @Composable @ReadOnlyComposable get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) CustomGradients.WavvyDark else CustomGradients.WavvyLight

@Suppress("UnusedReceiverParameter")
val MaterialTheme.accentCyan @Composable @ReadOnlyComposable get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) ElectricCyan else DeepCyan

// Main theme composable
@Composable
fun WavvyTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val targetColorScheme = if (darkTheme) DarkColors else LightColors
    val animationSpec = tween<Color>(durationMillis = 400)

    val animatedPrimary = animateColorAsState(targetColorScheme.primary, animationSpec, label = "primary").value
    val animatedOnPrimary = animateColorAsState(targetColorScheme.onPrimary, animationSpec, label = "onPrimary").value
    val animatedTertiary = animateColorAsState(targetColorScheme.tertiary, animationSpec, label = "tertiary").value
    val animatedBackground = animateColorAsState(targetColorScheme.background, animationSpec, label = "background").value
    val animatedOnBackground = animateColorAsState(targetColorScheme.onBackground, animationSpec, label = "onBackground").value
    val animatedSurface = animateColorAsState(targetColorScheme.surface, animationSpec, label = "surface").value
    val animatedOnSurface = animateColorAsState(targetColorScheme.onSurface, animationSpec, label = "onSurface").value
    val animatedSurfaceVariant = animateColorAsState(targetColorScheme.surfaceVariant, animationSpec, label = "surfaceVariant").value
    val animatedOnSurfaceVariant = animateColorAsState(targetColorScheme.onSurfaceVariant, animationSpec, label = "onSurfaceVariant").value
    val animatedPrimaryContainer = animateColorAsState(targetColorScheme.primaryContainer, animationSpec, label = "primaryContainer").value
    val animatedSecondaryContainer = animateColorAsState(targetColorScheme.secondaryContainer, animationSpec, label = "secondaryContainer").value
    val animatedError = animateColorAsState(targetColorScheme.error, animationSpec, label = "error").value
    val animatedOnError = animateColorAsState(targetColorScheme.onError, animationSpec, label = "onError").value

    val animatedColorScheme = targetColorScheme.copy(
        primary = animatedPrimary,
        onPrimary = animatedOnPrimary,
        tertiary = animatedTertiary,
        background = animatedBackground,
        onBackground = animatedOnBackground,
        surface = animatedSurface,
        onSurface = animatedOnSurface,
        surfaceVariant = animatedSurfaceVariant,
        onSurfaceVariant = animatedOnSurfaceVariant,
        primaryContainer = animatedPrimaryContainer,
        secondaryContainer = animatedSecondaryContainer,
        error = animatedError,
        onError = animatedOnError
    )

    val view = LocalView.current

    if (!view.isInEditMode) {
        val isAnimatedDark = animatedBackground.luminance() < 0.5f
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isAnimatedDark
            insetsController.isAppearanceLightNavigationBars = !isAnimatedDark
        }
    }

    MaterialTheme(
        colorScheme = animatedColorScheme,
        typography = AppTypography,
        content = content
    )
}

// Color luminance calculation helper
fun Color.luminance(): Float {
    return 0.2126f * red + 0.7152f * green + 0.0722f * blue
}

// Contrast calculation utilities
fun Color.contrastColor(): Color {
    val luminance = (0.299 * red + 0.587 * green + 0.114 * blue)
    return if (luminance > 0.5) Color.Black else Color.White
}

val Color.onContentColor: Color get() = this.contrastColor()
