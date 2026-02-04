package com.lonewolf.wavvy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset

// Brand Palette - Re-calibrated for high visibility
private val PureBlack = Color(0xFF000000)
private val RichBlack = Color(0xFF0C0C12) // Slightly lighter for surface definition
private val DeepCharcoal = Color(0xFF181820) // Secondary surfaces in dark mode
private val MutedSlate = Color(0xFF252530) // Tertiary/Stroke in dark mode

private val PremiumWhite = Color(0xFFFAFAFC)
private val GhostWhite = Color(0xFFF0F0F3) // Background light
private val SilverGrey = Color(0xFFD1D1D6) // Secondary surfaces in light mode

private val ElectricCyan = Color(0xFF00E5FF)
private val DeepCyan = Color(0xFF00B8D4)
private val VibrantPurple = Color(0xFF7C4DFF)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1A1A24),
    onPrimary = PremiumWhite,
    tertiary = DeepCyan,
    background = GhostWhite,
    onBackground = Color(0xFF0A0A0F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A24),
    surfaceVariant = Color(0xFFE2E2E8), // Defined borders/cards
    onSurfaceVariant = Color(0xFF48484A),
    primaryContainer = Color(0xFFDFDFE5),
    secondaryContainer = Color(0xFFD1D1D8), // Much clearer in light mode
    error = Color(0xFFFF3B30),
    onError = PremiumWhite
)

private val DarkColors = darkColorScheme(
    primary = PremiumWhite,
    onPrimary = PureBlack,
    tertiary = ElectricCyan,
    background = PureBlack,
    onBackground = PremiumWhite,
    surface = RichBlack,
    onSurface = PremiumWhite,
    surfaceVariant = DeepCharcoal, // Visible elevation
    onSurfaceVariant = Color(0xFFA1A1AA),
    primaryContainer = DeepCharcoal,
    secondaryContainer = MutedSlate, // Distinct secondary cards
    error = Color(0xFFFF453A),
    onError = PremiumWhite
)

object CustomGradients {
    val lyraLight = listOf(Color(0xFF667EEA), Color(0xFF764BA2))
    val lyraDark = listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF), Color(0xFFFF00E5))
}

object GenreGradients {
    private fun smoothGradient(start: Color, center: Color, end: Color): Brush {
        return Brush.linearGradient(
            colors = listOf(start, center, end),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }

    // Core & Heavy
    val pop = smoothGradient(Color(0xFFE91E63), Color(0xFFD81B60), Color(0xFF9C27B0))
    val rock = smoothGradient(Color(0xFF424242), Color(0xFF212121), Color(0xFF000000))
    val metal = smoothGradient(Color(0xFF757575), Color(0xFF424242), Color(0xFF000000))
    val punk = smoothGradient(Color(0xFFFF1744), Color(0xFFD500F9), Color(0xFF1A237E))
    val hardrock = smoothGradient(Color(0xFFB71C1C), Color(0xFF442222), Color(0xFF000000))

    // Urban & Rhythm
    val hiphop = smoothGradient(Color(0xFFFBC02D), Color(0xFFF57C00), Color(0xFFE64A19))
    val rnb = smoothGradient(Color(0xFF880E4F), Color(0xFF4A148C), Color(0xFF1A237E))
    val trap = smoothGradient(Color(0xFF607D8B), Color(0xFF263238), Color(0xFF000000))
    val phonk = smoothGradient(Color(0xFF7B1FA2), Color(0xFF4A148C), Color(0xFF000000))

    // Electronic & Vibe
    val electronic = smoothGradient(Color(0xFF00E5FF), Color(0xFF00B0FF), Color(0xFF1A237E))
    val indie = smoothGradient(Color(0xFF81C784), Color(0xFF43A047), Color(0xFF1B5E20))
    val lofi = smoothGradient(Color(0xFF9575CD), Color(0xFF7E57C2), Color(0xFF311B92))
    val ambient = smoothGradient(Color(0xFF4DD0E1), Color(0xFF0097A7), Color(0xFF006064))

    // Classy
    val jazz = smoothGradient(Color(0xFF7986CB), Color(0xFF3F51B5), Color(0xFF1A237E))
    val soul = smoothGradient(Color(0xFFFFA000), Color(0xFFFF6F00), Color(0xFF3E2723))

    // World & Regional
    val flamenco = smoothGradient(Color(0xFFFF5252), Color(0xFFD32F2F), Color(0xFF2D0D0D))
    val arabic = smoothGradient(Color(0xFFFFD54F), Color(0xFFFFA000), Color(0xFFBF360C))
    val greek = smoothGradient(Color(0xFF4FC3F7), Color(0xFF03A9F4), Color(0xFF0D47A1))
    val mpb = smoothGradient(Color(0xFF66BB6A), Color(0xFF388E3C), Color(0xFF1B5E20))
    val funk = smoothGradient(Color(0xFFE040FB), Color(0xFFD500F9), Color(0xFF4A148C))
    val sertanejo = smoothGradient(Color(0xFFA1887F), Color(0xFF8D6E63), Color(0xFF3E2723))
    val pagode = smoothGradient(Color(0xFFFFB74D), Color(0xFFFF9800), Color(0xFFE64A19))
    val rapNacional = smoothGradient(Color(0xFF90A4AE), Color(0xFF546E7A), Color(0xFF263238))
    val reggaeton = smoothGradient(Color(0xFFFF80AB), Color(0xFFFF4081), Color(0xFFC2185B))
    val afrobeat = smoothGradient(Color(0xFFFFD600), Color(0xFFFFA000), Color(0xFFBF360C))
    val reggae = smoothGradient(Color(0xFF66BB6A), Color(0xFFFFEB3B), Color(0xFFE53935))

    // Asian
    val kpop = smoothGradient(Color(0xFFFF4081), Color(0xFFF50057), Color(0xFF7C4DFF))
    val jpop = smoothGradient(Color(0xFFFF80AB), Color(0xFFF06292), Color(0xFFAD1457))
    val cpop = smoothGradient(Color(0xFFFF5252), Color(0xFFFF1744), Color(0xFFFFD600))
    val hindustani = smoothGradient(Color(0xFFFFB74D), Color(0xFFFF9800), Color(0xFFD84315))

    // Aesthetic & Movements
    val vaporwave = smoothGradient(Color(0xFFF48FB1), Color(0xFFCE93D8), Color(0xFF00E5FF))
    val synthwave = smoothGradient(Color(0xFFFF006E), Color(0xFF833AB4), Color(0xFF00E5FF))
    val citypop = smoothGradient(Color(0xFFF48FB1), Color(0xFF64B5F6), Color(0xFF1976D2))
    val darkwave = smoothGradient(Color(0xFF6A1B9A), Color(0xFF311B92), Color(0xFF000000))
    val dreamcore = smoothGradient(Color(0xFFE1BEE7), Color(0xFFBA68C8), Color(0xFF311B92))
    val chillwave = smoothGradient(Color(0xFFB2DFDB), Color(0xFF4DB6AC), Color(0xFF006064))
}

object DiscoveryChipColors {
    val trending = Color(0xFF00E676)
    val top50 = Color(0xFF9C27B0)
    val releases = Color(0xFFFF1744)
    val mixes = Color(0xFFFFAB00)
    val community = Color(0xFF2979FF)
    val radios = Color(0xFFFF6D00)
    val playlists = Color(0xFF00E5FF)
}

object MusicStateColors {
    val playing = Color(0xFF00E5FF)
    val paused = Color(0xFF8E8E93)
    val liked = Color(0xFFFF2D55)
    val downloaded = Color(0xFF00E676)
}

// Extensions
val MaterialTheme.lyraGradient @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) CustomGradients.lyraDark else CustomGradients.lyraLight
val MaterialTheme.accentCyan @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) ElectricCyan else DeepCyan
val MaterialTheme.accentPurple @Composable @ReadOnlyComposable get() = VibrantPurple

@Composable
fun WavvyTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
