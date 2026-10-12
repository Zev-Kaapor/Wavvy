package com.wavvy.app.features.settings.ui

// Android intents
import android.content.Intent
import androidx.core.net.toUri
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose UI
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
// Pictures
import coil3.compose.AsyncImage
// Project resources
import com.wavvy.app.R
import com.wavvy.app.features.settings.ui.components.SettingsCard
import com.wavvy.app.features.settings.ui.components.SettingsDivider
import com.wavvy.app.features.settings.ui.components.SettingsPage

// The name of the screen of the thanks in the route of the settings
const val ThanksSection = "Thanks"

// One of the projects the app is thankful to, the words that name it, what it gave, the address of its page and the address of its picture
private class Thanks(val titleRes: Int, val subtitleRes: Int, val addressRes: Int, val iconRes: Int)

// The projects and tools that inspired the app, the order is made by their names
private val SpecialThanks = listOf(
    Thanks(R.string.thanks_metrolist, R.string.thanks_metrolist_subtitle, R.string.thanks_metrolist_address, R.string.thanks_metrolist_icon),
    Thanks(R.string.thanks_riplay, R.string.thanks_riplay_subtitle, R.string.thanks_riplay_address, R.string.thanks_riplay_icon),
    Thanks(R.string.thanks_rimusic, R.string.thanks_rimusic_subtitle, R.string.thanks_rimusic_address, R.string.thanks_rimusic_icon),
    Thanks(R.string.thanks_simpmusic, R.string.thanks_simpmusic_subtitle, R.string.thanks_simpmusic_address, R.string.thanks_simpmusic_icon),
    Thanks(R.string.thanks_youtube_music, R.string.thanks_youtube_music_subtitle, R.string.thanks_youtube_music_address, R.string.thanks_youtube_music_icon),
    Thanks(R.string.thanks_morphe, R.string.thanks_morphe_subtitle, R.string.thanks_morphe_address, R.string.thanks_morphe_icon),
    Thanks(R.string.thanks_revanced, R.string.thanks_revanced_subtitle, R.string.thanks_revanced_address, R.string.thanks_revanced_icon),
    Thanks(R.string.thanks_gemini, R.string.thanks_gemini_subtitle, R.string.thanks_gemini_address, R.string.thanks_gemini_icon),
    Thanks(R.string.thanks_claude, R.string.thanks_claude_subtitle, R.string.thanks_claude_address, R.string.thanks_claude_icon)
)

// The projects and tools the app is thankful to, in the order of their names, each one with its picture opens its page
@Composable
fun ThanksScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val ordered = SpecialThanks.sortedBy { resources.getString(it.titleRes).lowercase() }

    SettingsPage(title = stringResource(R.string.settings_about_thanks_special), onBack = onBack, modifier = modifier) {
        SettingsCard {
            ordered.forEachIndexed { index, thanks ->
                if (index > 0) SettingsDivider()
                ThanksRow(
                    thanks = thanks,
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, resources.getString(thanks.addressRes).toUri())) }
                )
            }
        }
    }
}

// A project, its picture in a circle, its name and what it gave
@Composable
private fun ThanksRow(thanks: Thanks, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SettingsDimens.RowHeight)
            .clickable(onClick = onClick)
            .padding(horizontal = SettingsDimens.RowPadding, vertical = SettingsDimens.RowVertical)
    ) {
        AsyncImage(
            model = stringResource(thanks.iconRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(SettingsDimens.ThanksIcon)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        )

        Column(modifier = Modifier.weight(1f).padding(start = SettingsDimens.RowIconGap)) {
            Text(
                text = stringResource(thanks.titleRes),
                style = MaterialTheme.typography.bodyLarge.merge(SettingsType.RowTitle),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(thanks.subtitleRes),
                style = MaterialTheme.typography.bodyMedium.merge(SettingsType.RowSubtitle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
