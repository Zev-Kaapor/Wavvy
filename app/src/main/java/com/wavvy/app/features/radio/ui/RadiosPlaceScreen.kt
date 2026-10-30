package com.wavvy.app.features.radio.ui

// Android address
import android.net.Uri
// Compose layouts and foundations
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.radio.RadioCapitals
import com.wavvy.app.core.radio.RadiosEntry
import com.wavvy.app.core.radio.plain
import com.wavvy.app.features.discover.ui.ExploreBar
import com.wavvy.app.features.podcast.ui.PodcastSearchField

// A place of the radios, the states and the cities of a country, the cities of a state, or the stations of a city, a state, a genre or a country,
// the bar has the name of the place and the magnifier that opens a search that narrows what the page lists
@Composable
fun RadiosPlaceScreen(title: String, path: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    var query by rememberSaveable { mutableStateOf("") }
    var isSearching by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = isSearching) {
        isSearching = false
        query = ""
    }

    // Solid, so the screen under it does not show through while the page slides
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ExploreBar(
            title = title,
            titleAlpha = 1f,
            onBack = onBack,
            backgroundAlpha = 1f,
            trailing = {
                IconButton(onClick = { isSearching = !isSearching }) {
                    Icon(imageVector = WavvyIcons.Search, contentDescription = stringResource(R.string.radios_search_in, title), tint = MaterialTheme.colorScheme.onBackground)
                }
            }
        )

        // Every page of a place has a search that narrows what it lists, which are places or stations
        AnimatedVisibility(visible = isSearching, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            PodcastSearchField(
                query = query,
                onQueryChange = { query = it },
                onClose = {
                    isSearching = false
                    query = ""
                },
                hint = stringResource(R.string.radios_search_in, title)
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            if (path.startsWith(PlacesPrefix)) {
                PlacesList(path = path) { browse ->
                    val wanted = query.plain()
                    fun narrowed(entries: List<RadiosEntry>) = if (wanted.isEmpty()) entries else entries.filter { it.title.plain().contains(wanted) }

                    // The site lists only the main cities, so the capitals that are not among them are added, a country has its capital and Brazil the ones of its states
                    val capitalNames = when {
                        path.startsWith(CountryPrefix) -> if (RadioCapitals.isBrazil(title)) RadioCapitals.brazilStates() else listOfNotNull(RadioCapitals.ofCountry(title))
                        path.startsWith(StatePrefix) -> listOfNotNull(RadioCapitals.ofBrazilState(title))
                        else -> emptyList()
                    }
                    val capitals = capitalNames.map { name ->
                        browse.cities.firstOrNull { it.title.plain().substringBefore("(").trim() == name.plain() }
                            ?: RadiosEntry(name, "/busca?q=${Uri.encode(name)}&qfilter=cidade")
                    }
                    val cities = browse.cities.filter { city -> capitals.none { it.path == city.path } }

                    listOfNotNull(
                        // The link to every station of the place is for when nothing was typed
                        browse.all?.takeIf { wanted.isEmpty() }?.let { RadiosSection(null, listOf(it), RadiosColors.Stations, isFull = true) },
                        RadiosSection(if (capitals.size == 1) R.string.radios_section_capital else R.string.radios_section_capitals, narrowed(capitals), RadiosColors.Capital),
                        RadiosSection(R.string.radios_section_states, narrowed(browse.states), RadiosColors.Places),
                        RadiosSection(R.string.radios_section_cities, narrowed(cities), RadiosColors.Match)
                    )
                }
            } else {
                StationsList(path = path, filter = query)
            }
        }
    }
}

// The start of the address of the pages of the site that list places and not stations, and the ones of a country and of a state
private const val PlacesPrefix = "/lista/"
private const val CountryPrefix = "/lista/pais/"
private const val StatePrefix = "/lista/uf/"
