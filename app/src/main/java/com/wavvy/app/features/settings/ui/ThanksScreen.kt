package com.wavvy.app.features.settings.ui

// Android intents
import android.content.Intent
import androidx.core.net.toUri
// Compose layouts and foundations
import androidx.compose.foundation.Image
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.painterResource
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

// One of the projects, people or tools the app is thankful to, the words that name it, what it gave, and the address of its page and of its picture when it has them
// A person has no page and its picture is one of the app
private class Thanks(val titleRes: Int, val subtitleRes: Int, val addressRes: Int? = null, val iconRes: Int? = null, val pictureRes: Int? = null)

// The projects and tools that inspired the app, the order is made by their names
private val SpecialThanks = listOf(
    Thanks(R.string.thanks_metrolist, R.string.thanks_metrolist_subtitle, R.string.thanks_metrolist_address, R.string.thanks_metrolist_icon),
    Thanks(R.string.thanks_riplay, R.string.thanks_riplay_subtitle, R.string.thanks_riplay_address, R.string.thanks_riplay_icon),
    Thanks(R.string.thanks_rimusic, R.string.thanks_rimusic_subtitle, R.string.thanks_rimusic_address, R.string.thanks_rimusic_icon),
    Thanks(R.string.thanks_sponsorblock, R.string.thanks_sponsorblock_subtitle, R.string.thanks_sponsorblock_address, R.string.thanks_sponsorblock_icon),
    Thanks(R.string.thanks_simpmusic, R.string.thanks_simpmusic_subtitle, R.string.thanks_simpmusic_address, R.string.thanks_simpmusic_icon),
    Thanks(R.string.thanks_youtube_music, R.string.thanks_youtube_music_subtitle, R.string.thanks_youtube_music_address, R.string.thanks_youtube_music_icon),
    Thanks(R.string.thanks_morphe, R.string.thanks_morphe_subtitle, R.string.thanks_morphe_address, R.string.thanks_morphe_icon),
    Thanks(R.string.thanks_revanced, R.string.thanks_revanced_subtitle, R.string.thanks_revanced_address, R.string.thanks_revanced_icon)
)

// The people that helped to make the app, in the order of their names
private val PeopleThanks = listOf(
    Thanks(R.string.thanks_noa, R.string.thanks_noa_subtitle, pictureRes = R.drawable.thanks_noa),
    Thanks(R.string.thanks_walppxk, R.string.thanks_walppxk_subtitle, pictureRes = R.drawable.thanks_walppxk)
)

// The tools that took part in the development, in the order of their names, they are thanked apart from the projects
private val AiThanks = listOf(
    Thanks(R.string.thanks_claude, R.string.thanks_claude_subtitle, R.string.thanks_claude_address, R.string.thanks_claude_icon),
    Thanks(R.string.thanks_gemini, R.string.thanks_gemini_subtitle, R.string.thanks_gemini_address, R.string.thanks_gemini_icon)
)

// The projects the app is thankful to, in the order of their names, then the people and the tools that helped, each group under a line, and each one with its picture opens its page when it has one
@Composable
fun ThanksScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val ordered = SpecialThanks.sortedBy { resources.getString(it.titleRes).lowercase() }
    val open: (Thanks) -> Unit = { thanks ->
        thanks.addressRes?.let { context.startActivity(Intent(Intent.ACTION_VIEW, resources.getString(it).toUri())) }
    }

    SettingsPage(title = stringResource(R.string.settings_about_thanks_special), onBack = onBack, modifier = modifier) {
        Text(
            text = stringResource(R.string.thanks_projects_group),
            style = MaterialTheme.typography.labelLarge.merge(SettingsType.GroupTitle),
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(start = SettingsDimens.GroupTitleStart)
        )
        SettingsCard {
            ordered.forEachIndexed { index, thanks ->
                if (index > 0) SettingsDivider()
                ThanksRow(thanks = thanks, onClick = { open(thanks) })
            }
        }

        // The people that helped come after the projects, under a line
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            text = stringResource(R.string.thanks_people_group),
            style = MaterialTheme.typography.labelLarge.merge(SettingsType.GroupTitle),
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(start = SettingsDimens.GroupTitleStart)
        )
        SettingsCard {
            PeopleThanks.forEachIndexed { index, thanks ->
                if (index > 0) SettingsDivider()
                ThanksRow(thanks = thanks, onClick = { open(thanks) })
            }
        }

        // The tools that helped in the development are thanked apart, under a line
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            text = stringResource(R.string.thanks_ai_group),
            style = MaterialTheme.typography.labelLarge.merge(SettingsType.GroupTitle),
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(start = SettingsDimens.GroupTitleStart)
        )
        SettingsCard {
            AiThanks.forEachIndexed { index, thanks ->
                if (index > 0) SettingsDivider()
                ThanksRow(thanks = thanks, onClick = { open(thanks) })
            }
        }
    }
}

// A project, a person or a tool, its picture in a circle, its name and what it gave
@Composable
private fun ThanksRow(thanks: Thanks, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SettingsDimens.RowHeight)
            .clickable(enabled = thanks.addressRes != null, onClick = onClick)
            .padding(horizontal = SettingsDimens.RowPadding, vertical = SettingsDimens.RowVertical)
    ) {
        val pictureModifier = Modifier
            .size(SettingsDimens.ThanksIcon)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)

        // A picture of the app, or the one of the internet
        if (thanks.pictureRes != null) {
            Image(painter = painterResource(thanks.pictureRes), contentDescription = null, contentScale = ContentScale.Crop, modifier = pictureModifier)
        } else {
            AsyncImage(model = thanks.iconRes?.let { stringResource(it) }, contentDescription = null, contentScale = ContentScale.Crop, modifier = pictureModifier)
        }

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
