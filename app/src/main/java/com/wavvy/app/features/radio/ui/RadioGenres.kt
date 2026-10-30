package com.wavvy.app.features.radio.ui

// Text collation
import java.text.Collator
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringArrayResource
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.radio.RadioSegment
import com.wavvy.app.core.radio.RadioSegments
import com.wavvy.app.core.radio.RadioStation
import com.wavvy.app.core.radio.plain

// The genres with the name in the language of the app, in the order of that language
internal class RadioGenres(val sorted: List<Pair<RadioSegment, String>>, private val titles: Map<Int, String>) {
    // The name of a genre in the language of the app
    fun titleOf(segment: RadioSegment): String = titles[segment.id] ?: segment.siteName

    // The name of the genre a station says it has, as the site writes it, which is left as it is when the site has no list for it
    fun titleOf(siteName: String): String = RadioSegments.named(siteName)?.let(::titleOf) ?: siteName
}

@Composable
internal fun rememberRadioGenres(): RadioGenres {
    val names = stringArrayResource(R.array.radio_genre_names)
    val locale = LocalConfiguration.current.locales[0]

    return remember(names, locale) {
        val titles = RadioSegments.titles(names)
        val collator = Collator.getInstance(locale).apply { strength = Collator.PRIMARY }
        RadioGenres(RadioSegments.all.map { it to titles.getValue(it.id) }.sortedWith { a, b -> collator.compare(a.second, b.second) }, titles)
    }
}

// The stations in the order of the names in the language of the app when the whole list came in one page, the word that every name starts with does not count so they are not all under the same letter
// A list that comes page by page keeps the order of the site, so what arrives is added at the end and the user sees what is new
@Composable
internal fun rememberSortedStations(stations: List<RadioStation>, isWhole: Boolean): List<RadioStation> {
    val locale = LocalConfiguration.current.locales[0]

    return remember(stations, isWhole, locale) {
        if (!isWhole) return@remember stations
        val collator = Collator.getInstance(locale).apply { strength = Collator.PRIMARY }
        stations.sortedWith { a, b -> collator.compare(sortKey(a.name), sortKey(b.name)) }
    }
}

// The name without the first words that almost every station has
private val CommonStart = Regex("^(radio|rede|fm|web radio)\\s+")

private fun sortKey(name: String): String = name.plain().replace(CommonStart, "")
