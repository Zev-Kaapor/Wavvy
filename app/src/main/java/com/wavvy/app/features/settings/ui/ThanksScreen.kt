package com.wavvy.app.features.settings.ui

// Android intents
import android.content.Intent
import androidx.core.net.toUri
// Compose UI
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
// Project resources
import com.wavvy.app.R
import com.wavvy.app.features.settings.ui.components.SettingsActionRow
import com.wavvy.app.features.settings.ui.components.SettingsCard
import com.wavvy.app.features.settings.ui.components.SettingsDivider
import com.wavvy.app.features.settings.ui.components.SettingsPage

// The name of the screen of the thanks in the route of the settings
const val ThanksSection = "Thanks"

// One of the projects the app is thankful to, the words that name it, what it gave and the address of its page
private class Thanks(val titleRes: Int, val subtitleRes: Int, val addressRes: Int)

// The projects and tools that inspired the app, in the order they are shown
private val SpecialThanks = listOf(
    Thanks(R.string.thanks_metrolist, R.string.thanks_metrolist_subtitle, R.string.thanks_metrolist_address),
    Thanks(R.string.thanks_riplay, R.string.thanks_riplay_subtitle, R.string.thanks_riplay_address),
    Thanks(R.string.thanks_rimusic, R.string.thanks_rimusic_subtitle, R.string.thanks_rimusic_address),
    Thanks(R.string.thanks_simpmusic, R.string.thanks_simpmusic_subtitle, R.string.thanks_simpmusic_address),
    Thanks(R.string.thanks_youtube_music, R.string.thanks_youtube_music_subtitle, R.string.thanks_youtube_music_address),
    Thanks(R.string.thanks_revanced, R.string.thanks_revanced_subtitle, R.string.thanks_revanced_address),
    Thanks(R.string.thanks_gemini, R.string.thanks_gemini_subtitle, R.string.thanks_gemini_address),
    Thanks(R.string.thanks_claude, R.string.thanks_claude_subtitle, R.string.thanks_claude_address)
)

// The projects and tools the app is thankful to, each one opens its page
@Composable
fun ThanksScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val resources = LocalResources.current

    SettingsPage(title = stringResource(R.string.settings_about_thanks_special), onBack = onBack, modifier = modifier) {
        SettingsCard {
            SpecialThanks.forEachIndexed { index, thanks ->
                if (index > 0) SettingsDivider()
                SettingsActionRow(
                    title = stringResource(thanks.titleRes),
                    subtitle = stringResource(thanks.subtitleRes),
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, resources.getString(thanks.addressRes).toUri())) }
                )
            }
        }
    }
}
