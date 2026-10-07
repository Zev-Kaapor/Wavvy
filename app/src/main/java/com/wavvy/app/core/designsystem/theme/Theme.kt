package com.wavvy.app.core.designsystem.theme

// Android components
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
// Compose animation and layouts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
// Material 3 components
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
// UI styling and utilities
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Material colors of the dark theme
private val DarkColorScheme = darkColorScheme(
    primary = DarkAccent,
    onPrimary = DarkOnAccent,
    primaryContainer = DarkAccentContainer,
    onPrimaryContainer = DarkOnAccentContainer,
    inversePrimary = LightAccent,
    secondary = DarkOnSurfaceVariant,
    onSecondary = DarkOnAccent,
    secondaryContainer = DarkContainerHighest,
    onSecondaryContainer = DarkOnBackground,
    tertiary = DarkAccent,
    onTertiary = DarkOnAccent,
    tertiaryContainer = DarkAccentContainer,
    onTertiaryContainer = DarkOnAccentContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkBackground,
    onSurface = DarkOnBackground,
    surfaceVariant = DarkContainer,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceTint = DarkAccent,
    inverseSurface = DarkOnBackground,
    inverseOnSurface = DarkBackground,
    error = DarkError,
    onError = OnFilled,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    scrim = Scrim,
    surfaceBright = DarkContainerHighest,
    surfaceDim = DarkContainerLowest,
    surfaceContainer = DarkContainer,
    surfaceContainerHigh = DarkContainerHigh,
    surfaceContainerHighest = DarkContainerHighest,
    surfaceContainerLow = DarkContainerLow,
    surfaceContainerLowest = DarkContainerLowest
)

// Material colors of the light theme
private val LightColorScheme = lightColorScheme(
    primary = LightAccent,
    onPrimary = LightOnAccent,
    primaryContainer = LightAccentContainer,
    onPrimaryContainer = LightOnAccentContainer,
    inversePrimary = DarkAccent,
    secondary = LightOnSurfaceVariant,
    onSecondary = LightOnAccent,
    secondaryContainer = LightContainerHigh,
    onSecondaryContainer = LightOnBackground,
    tertiary = LightAccent,
    onTertiary = LightOnAccent,
    tertiaryContainer = LightAccentContainer,
    onTertiaryContainer = LightOnAccentContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightBackground,
    onSurface = LightOnBackground,
    surfaceVariant = LightContainer,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceTint = LightAccent,
    inverseSurface = LightOnBackground,
    inverseOnSurface = LightBackground,
    error = LightError,
    onError = OnFilled,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    scrim = Scrim,
    surfaceBright = LightContainerLowest,
    surfaceDim = LightContainerHighest,
    surfaceContainer = LightContainer,
    surfaceContainerHigh = LightContainerHigh,
    surfaceContainerHighest = LightContainerHighest,
    surfaceContainerLow = LightContainerLow,
    surfaceContainerLowest = LightContainerLowest
)

// Extra colors of the current theme
private val LocalWavvyColors = staticCompositionLocalOf { DarkWavvyColors }

// Whether the current theme is the dark one
private val LocalDarkTheme = staticCompositionLocalOf { true }

// App theme, follows the system unless a mode is forced
@SuppressLint("ConfigurationScreenWidthHeight")
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

    val colorScheme = (if (darkTheme) DarkColorScheme else LightColorScheme).animated()
    val wavvyColors = (if (darkTheme) DarkWavvyColors else LightWavvyColors).animated()
    val dimens = dimensFor(windowWidthFor(LocalConfiguration.current.screenWidthDp))

    SystemBarsAppearance(darkTheme = darkTheme)

    CompositionLocalProvider(
        LocalDimens provides dimens,
        LocalWavvyColors provides wavvyColors,
        LocalDarkTheme provides darkTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = WavvyShapes,
            content = content
        )
    }
}

// Dark colors only, for the expanded player that always sits over a dark picture, the system bars keep the app theme
@Composable
fun DarkColors(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalWavvyColors provides DarkWavvyColors,
        LocalDarkTheme provides true
    ) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = Typography,
            shapes = WavvyShapes,
            content = content
        )
    }
}

// Shortcuts to read the app tokens
object WavvyTheme {
    val colors: WavvyColors
        @Composable get() = LocalWavvyColors.current

    val dimens: Dimens
        @Composable get() = LocalDimens.current

    val isDark: Boolean
        @Composable get() = LocalDarkTheme.current
}

// Icons of the system bars follow the theme, they show up when the bars come back with a swipe
@Composable
private fun SystemBarsAppearance(darkTheme: Boolean) {
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }
}

// Activity behind a context that may be wrapped
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

// One color moving to its new value when the theme changes
@Composable
private fun animated(color: Color, label: String): Color =
    animateColorAsState(
        targetValue = color,
        animationSpec = tween(durationMillis = WavvyMotion.ThemeMillis),
        label = label
    ).value

// Every role of the color scheme animates together
@Composable
private fun ColorScheme.animated(): ColorScheme = copy(
    primary = animated(primary, "primary"),
    onPrimary = animated(onPrimary, "onPrimary"),
    primaryContainer = animated(primaryContainer, "primaryContainer"),
    onPrimaryContainer = animated(onPrimaryContainer, "onPrimaryContainer"),
    inversePrimary = animated(inversePrimary, "inversePrimary"),
    secondary = animated(secondary, "secondary"),
    onSecondary = animated(onSecondary, "onSecondary"),
    secondaryContainer = animated(secondaryContainer, "secondaryContainer"),
    onSecondaryContainer = animated(onSecondaryContainer, "onSecondaryContainer"),
    tertiary = animated(tertiary, "tertiary"),
    onTertiary = animated(onTertiary, "onTertiary"),
    tertiaryContainer = animated(tertiaryContainer, "tertiaryContainer"),
    onTertiaryContainer = animated(onTertiaryContainer, "onTertiaryContainer"),
    background = animated(background, "background"),
    onBackground = animated(onBackground, "onBackground"),
    surface = animated(surface, "surface"),
    onSurface = animated(onSurface, "onSurface"),
    surfaceVariant = animated(surfaceVariant, "surfaceVariant"),
    onSurfaceVariant = animated(onSurfaceVariant, "onSurfaceVariant"),
    surfaceTint = animated(surfaceTint, "surfaceTint"),
    inverseSurface = animated(inverseSurface, "inverseSurface"),
    inverseOnSurface = animated(inverseOnSurface, "inverseOnSurface"),
    error = animated(error, "error"),
    onError = animated(onError, "onError"),
    errorContainer = animated(errorContainer, "errorContainer"),
    onErrorContainer = animated(onErrorContainer, "onErrorContainer"),
    outline = animated(outline, "outline"),
    outlineVariant = animated(outlineVariant, "outlineVariant"),
    scrim = animated(scrim, "scrim"),
    surfaceBright = animated(surfaceBright, "surfaceBright"),
    surfaceDim = animated(surfaceDim, "surfaceDim"),
    surfaceContainer = animated(surfaceContainer, "surfaceContainer"),
    surfaceContainerHigh = animated(surfaceContainerHigh, "surfaceContainerHigh"),
    surfaceContainerHighest = animated(surfaceContainerHighest, "surfaceContainerHighest"),
    surfaceContainerLow = animated(surfaceContainerLow, "surfaceContainerLow"),
    surfaceContainerLowest = animated(surfaceContainerLowest, "surfaceContainerLowest")
)

// Same for the extra colors
@Composable
private fun WavvyColors.animated(): WavvyColors = copy(
    onMedia = animated(onMedia, "onMedia"),
    mediaScrim = animated(mediaScrim, "mediaScrim"),
    accentFill = animated(accentFill, "accentFill"),
    onAccentFill = animated(onAccentFill, "onAccentFill"),
    elevated = animated(elevated, "elevated"),
    playButton = animated(playButton, "playButton"),
    tileScrim = animated(tileScrim, "tileScrim"),
    playerRing = animated(playerRing, "playerRing"),
    playerRingTrack = animated(playerRingTrack, "playerRingTrack"),
    coverPlaceholder = animated(coverPlaceholder, "coverPlaceholder"),
    playerAccent = animated(playerAccent, "playerAccent"),
    playerLiked = animated(playerLiked, "playerLiked"),
    textShadow = animated(textShadow, "textShadow"),
    videoBadge = animated(videoBadge, "videoBadge"),
    pinBadge = animated(pinBadge, "pinBadge"),
    loginBar = animated(loginBar, "loginBar"),
    navBar = animated(navBar, "navBar"),
    chip = animated(chip, "chip"),
    skeleton = animated(skeleton, "skeleton"),
    skeletonHighlight = animated(skeletonHighlight, "skeletonHighlight"),
    navUnselected = animated(navUnselected, "navUnselected"),
    navSelected = animated(navSelected, "navSelected"),
    glowStart = animated(glowStart, "glowStart"),
    glowEnd = animated(glowEnd, "glowEnd"),
    gradientStart = animated(gradientStart, "gradientStart"),
    gradientMiddle = animated(gradientMiddle, "gradientMiddle"),
    gradientEnd = animated(gradientEnd, "gradientEnd"),
    buttonStart = animated(buttonStart, "buttonStart"),
    buttonEnd = animated(buttonEnd, "buttonEnd")
)
