package com.wavvy.app.features.recap.ui

// Compose state and runtime
import androidx.compose.runtime.compositionLocalOf
// UI styling and utilities
import androidx.compose.ui.graphics.Color

// The colors of one page: the flat color behind, the color of the words, and two colors for the shapes
class RecapTheme(val background: Color, val ink: Color, val accent: Color, val accentAlt: Color) {
    // The words that come second, a little softer than the others
    val softInk: Color get() = ink.copy(alpha = RecapColors.SoftAlpha)
}

// Colors of the pages of the retrospective, which look the same on the light and on the dark theme
object RecapColors {
    // How much softer the second words are, how faint the bars of the story that were not reached are, and the circle behind the cross
    const val SoftAlpha = 0.75f
    const val TrackAlpha = 0.3f
    const val ButtonAlpha = 0.12f
    // How faint the shapes that stand for a name and a cover that are not there yet are
    const val PlaceholderAlpha = 0.16f
    // How faint the words at the foot of a page are
    const val FooterAlpha = 0.55f

    // The light that crosses the name of the artist and the crown
    val Shine = Color(0xFFFFFFFF)

    // The words over the bands of the stack of genres, and the face of the card of the kind of listener with its words
    val BandInk = Color(0xFF1A1A1A)
    val CardFace = Color(0xFFFFFFFF)
    val CardInk = Color(0xFF1A1A1A)

    // The pages take these one after the other, each one flat with its own colors
    val Themes = listOf(
        RecapTheme(Color(0xFFFF5A3C), Color(0xFF1A1A1A), Color(0xFF7B2CFF), Color(0xFFC6FF3D)),
        RecapTheme(Color(0xFFA8A4FF), Color(0xFF14112E), Color(0xFFFF7A3C), Color(0xFFFFE04D)),
        RecapTheme(Color(0xFF1FD6A4), Color(0xFF06251C), Color(0xFFFF5A3C), Color(0xFFFFE04D)),
        RecapTheme(Color(0xFF5A0AD1), Color(0xFFFFE04D), Color(0xFFFF7A00), Color(0xFF1A1A1A)),
        RecapTheme(Color(0xFFF96FC0), Color(0xFF1A1A1A), Color(0xFF1FD65F), Color(0xFFFFE04D)),
        RecapTheme(Color(0xFFFFD60A), Color(0xFF1A1A1A), Color(0xFF7B2CFF), Color(0xFFFF5A3C)),
        RecapTheme(Color(0xFF121212), Color(0xFFFFFFFF), Color(0xFFF72585), Color(0xFF00E5FF))
    )

    // The colors of the lines of the timeline, the last one is dark so it does not vanish on the green of its page
    val Lines = listOf(Color(0xFFFF3D81), Color(0xFFC6FF3D), Color(0xFFA8A4FF), Color(0xFFFFB020), Color(0xFF0B2A6B))

    // How strong the line of the artist is on the page of the most played song, not as black as the title and not faint
    const val SongArtistAlpha = 0.85f

    // How strong the edge of the plastic, the hinges of the spine and the bands of light are on the case of the cover of the album
    const val CaseFrameAlpha = 0.4f
    const val CaseHingeAlpha = 0.28f
    const val CaseGlareAlpha = 0.16f

    // The record of the page of the most played song: its black, the faint lines of its grooves and the light on it
    val Vinyl = Color(0xFF141414)
    const val VinylGrooveAlpha = 0.1f
    const val VinylGlareAlpha = 0.16f

    // The numbers of the first three places of a list: a cyan, a strong lime green and a pure yellow, light and bright so they stand out on the pink of the page, the others keep the color of the words
    val PlaceColors = listOf(Color(0xFF00E5FF), Color(0xFFC6FF3D), Color(0xFFFFEB00))

    // The colors the bands of the stack of genres take one after the other
    val Bands = listOf(Color(0xFFFF3D81), Color(0xFFC6FF3D), Color(0xFFA8A4FF), Color(0xFFFFB020), Color(0xFF1FD6A4))
}

// The colors of the page that is drawn, read by the words and the shapes of that page
val LocalRecapTheme = compositionLocalOf { RecapColors.Themes.first() }
