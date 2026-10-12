package com.wavvy.app.core.designsystem.theme

// Colors and the kinds of part
import androidx.compose.ui.graphics.Color
import com.wavvy.app.core.playback.SponsorCategory

// The color of each kind of part marked by the community, the same ones SponsorBlock shows on its bars, so the user knows them at a glance
object SponsorColors {
    private val NonMusic = Color(0xFFFF9900)
    private val Sponsor = Color(0xFF00D400)
    private val SelfPromo = Color(0xFFFFFF00)
    private val Interaction = Color(0xFFCC00FF)
    private val Intro = Color(0xFF00FFFF)
    private val Outro = Color(0xFF0202ED)
    private val Preview = Color(0xFF008FD6)

    fun of(category: SponsorCategory): Color = when (category) {
        SponsorCategory.NonMusic -> NonMusic
        SponsorCategory.Sponsor -> Sponsor
        SponsorCategory.SelfPromo -> SelfPromo
        SponsorCategory.Interaction -> Interaction
        SponsorCategory.Intro -> Intro
        SponsorCategory.Outro -> Outro
        SponsorCategory.Preview -> Preview
    }
}
