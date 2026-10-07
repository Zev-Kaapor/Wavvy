package com.wavvy.app.features.artist.ui

// Compose layouts and foundations
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
// Java time and locale
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.features.artist.data.ArtistPage
import com.wavvy.app.features.artist.data.ArtistProfile

// What MusicBrainz calls a person, any other kind is a group of some sort
private const val PersonType = "Person"

// At the end of the page, who the artist is, the views of YouTube Music and what MusicBrainz adds, the birth, the country, the genres and the links
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ArtistAbout(
    page: ArtistPage,
    profile: ArtistProfile?
) {
    val details = profile?.takeIf { it.hasContent }
    if (page.views == null && details == null) return

    val dimens = WavvyTheme.dimens
    val uriHandler = LocalUriHandler.current
    val locale = LocalConfiguration.current.locales[0]
    val detail = MaterialTheme.typography.bodyMedium.merge(ArtistType.Detail)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.screenPadding, vertical = ArtistDimens.AboutPadding)
    ) {
        Text(
            text = stringResource(R.string.artist_about),
            style = MaterialTheme.typography.titleLarge.merge(ArtistType.About),
            color = MaterialTheme.colorScheme.onBackground
        )

        page.views?.let {
            Text(text = it, style = detail, color = MaterialTheme.colorScheme.secondary)
        }

        if (details == null) return@Column

        // The day the person was born or the group was formed, and the day it ended
        val isPerson = details.type == PersonType
        details.begin?.let {
            AboutFact(label = stringResource(if (isPerson) R.string.artist_born else R.string.artist_formed), value = dateOf(it, locale))
        }
        details.end?.let {
            AboutFact(label = stringResource(if (isPerson) R.string.artist_died else R.string.artist_disbanded), value = dateOf(it, locale))
        }

        // Where the artist is from, the city and the country in the language of the device
        val country = details.countryCode?.let { Locale("", it).getDisplayCountry(locale) }?.takeIf { it.isNotBlank() }
        listOfNotNull(details.city, country).joinToString(", ").takeIf { it.isNotEmpty() }?.let {
            AboutFact(label = stringResource(R.string.artist_origin), value = it)
        }

        if (details.genres.isNotEmpty()) {
            AboutLabel(stringResource(R.string.artist_genres))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(ArtistDimens.ChipGap),
                verticalArrangement = Arrangement.spacedBy(ArtistDimens.ChipGap)
            ) {
                details.genres.forEach { genre -> AboutChip(text = genre, color = MaterialTheme.colorScheme.onBackground) }
            }
        }

        if (details.links.isNotEmpty()) {
            AboutLabel(stringResource(R.string.artist_links))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(ArtistDimens.ChipGap),
                verticalArrangement = Arrangement.spacedBy(ArtistDimens.ChipGap)
            ) {
                details.links.forEach { link ->
                    AboutChip(
                        text = if (link.isWebsite) stringResource(R.string.artist_website) else link.platform,
                        color = MaterialTheme.colorScheme.primary,
                        onClick = { runCatching { uriHandler.openUri(link.url) } }
                    )
                }
            }
        }
    }
}

// A small outlined label, a link when it has something to open
@Composable
private fun AboutChip(
    text: String,
    color: Color,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(ArtistDimens.ChipCorner)

    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = Modifier
            .clip(shape)
            .border(ArtistDimens.ChipBorder, MaterialTheme.colorScheme.outlineVariant, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = ArtistDimens.ChipPaddingHorizontal, vertical = ArtistDimens.ChipPaddingVertical)
    )
}

// A line with what it is about in the secondary color and its value after it
@Composable
private fun AboutFact(label: String, value: String) {
    val detail = MaterialTheme.typography.bodyMedium.merge(ArtistType.Detail)

    Spacer(modifier = Modifier.height(ArtistDimens.AboutGap))
    Row {
        Text(text = label, style = detail, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.width(ArtistDimens.AboutLabelWidth))
        Text(text = value, style = detail, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
    }
}

// The name of a group of chips, with room above and below
@Composable
private fun AboutLabel(text: String) {
    Spacer(modifier = Modifier.height(ArtistDimens.AboutGroupGap))
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.merge(ArtistType.Detail),
        color = MaterialTheme.colorScheme.secondary
    )
    Spacer(modifier = Modifier.height(ArtistDimens.AboutGap))
}

// A date of MusicBrainz in the format of the language of the device, which has a whole day, only a month or only a year
private fun dateOf(value: String, locale: Locale): String {
    return runCatching {
        when (value.length) {
            WholeDateLength -> LocalDate.parse(value).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale))
            MonthLength -> YearMonth.parse(value).format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
            else -> value
        }
    }.getOrDefault(value)
}

// How long a whole date and a month are when written as 2001-10-06 and 2001-10
private const val WholeDateLength = 10
private const val MonthLength = 7
