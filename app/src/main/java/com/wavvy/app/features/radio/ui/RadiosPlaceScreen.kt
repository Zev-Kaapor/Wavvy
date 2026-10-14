package com.wavvy.app.features.radio.ui

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.radio.RadiosEntry
import com.wavvy.app.core.radio.plain
import com.wavvy.app.features.home.ui.components.HomeType
import com.wavvy.app.features.search.ui.components.SearchBar

// A place of the radios, the states and the cities of a country, the cities of a state, or the stations of a city, a state or a country
@Composable
fun RadiosPlaceScreen(title: String, path: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val dimens = WavvyTheme.dimens
    var query by rememberSaveable { mutableStateOf("") }

    // Solid, so the screen under it does not show through while the page slides
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Every page of a place has a search that narrows what it lists, which are places or stations
        SearchBar(
            query = query,
            onQueryChange = { query = it },
            onSearch = {},
            onBack = onBack,
            onFocusChange = {},
            hint = stringResource(R.string.radios_search_in, title)
        )

        if (path.startsWith(PlacesPrefix)) {
            PlacesList(path = path) { browse ->
                val wanted = query.plain()
                fun narrowed(entries: List<RadiosEntry>) = if (wanted.isEmpty()) entries else entries.filter { it.title.plain().contains(wanted) }

                listOfNotNull(
                    // The link to every station of the place is for when nothing was typed
                    browse.all?.takeIf { wanted.isEmpty() }?.let { RadiosSection(null, listOf(it)) },
                    RadiosSection(R.string.radios_section_states, narrowed(browse.states)),
                    RadiosSection(R.string.radios_section_cities, narrowed(browse.cities))
                )
            }
        } else {
            StationsList(path = path, filter = query)
        }
    }
}

// The start of the address of the pages of the site that list places and not stations
private const val PlacesPrefix = "/lista/"
