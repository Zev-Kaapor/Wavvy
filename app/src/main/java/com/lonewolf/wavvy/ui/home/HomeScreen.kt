package com.lonewolf.wavvy.ui.home

// Compose foundation and layout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
// State management
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
// Tools and positioning
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
// Project resources
import com.lonewolf.wavvy.R
// Shared and internal components
import com.lonewolf.wavvy.ui.common.HomeHeader
import com.lonewolf.wavvy.ui.home.components.*

// State holder for playback UI
@Stable
class PlayerState(
    isMiniPlayerActive: Boolean = false,
    isPlayerExpanded: Boolean = false,
    isQueueActive: Boolean = false,
    currentSongTitle: String = "",
    currentArtistName: String = "",
    currentImageUrl: String? = null,
    currentSongUrl: String? = null
) {
    var isMiniPlayerActive by mutableStateOf(isMiniPlayerActive)
    var isPlayerExpanded by mutableStateOf(isPlayerExpanded)
    var isQueueActive by mutableStateOf(isQueueActive)
    var currentSongTitle by mutableStateOf(currentSongTitle)
    var currentArtistName by mutableStateOf(currentArtistName)
    var currentImageUrl by mutableStateOf(currentImageUrl)
    var currentSongUrl by mutableStateOf(currentSongUrl)

    // Update playback state
    fun updatePlayback(title: String, artist: String, url: String? = null) {
        currentSongTitle = title
        currentArtistName = artist
        currentImageUrl = null
        currentSongUrl = url
        isMiniPlayerActive = true
        isPlayerExpanded = false
        isQueueActive = false
    }

    companion object {
        val Saver: Saver<PlayerState, *> = listSaver(
            save = {
                listOf(
                    it.isMiniPlayerActive,
                    it.isPlayerExpanded,
                    it.isQueueActive,
                    it.currentSongTitle,
                    it.currentArtistName,
                    it.currentImageUrl,
                    it.currentSongUrl
                )
            },
            restore = {
                PlayerState(
                    isMiniPlayerActive = it[0] as Boolean,
                    isPlayerExpanded = it[1] as Boolean,
                    isQueueActive = it[2] as Boolean,
                    currentSongTitle = it[3] as String,
                    currentArtistName = it[4] as String,
                    currentImageUrl = it[5] as? String,
                    currentSongUrl = it[6] as? String
                )
            }
        )
    }
}

// Main screen entry point
@Composable
fun HomeScreen(
    userName: String? = null,
    playerState: PlayerState
) {
    // String resources for dynamic content
    val forgottenFavoritesTitle = stringResource(R.string.section_title_forgotten_favorites)
    val wavvyArtist = stringResource(R.string.placeholder_wavvy_artist)
    val wavvySong = stringResource(R.string.placeholder_wavvy_song)
    val mixSuffix = "Mix"

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Main content list
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            item(key = "header", contentType = "header") {
                HomeHeader(onNavigateToSettings = { })
            }

            // User greeting
            item(key = "greeting", contentType = "greeting") {
                GreetingSection(userName = userName)
            }

            // Category filters
            item(key = "filters", contentType = "filters") {
                FilterPills()
            }

            // Quick choices grid
            item(key = "fast_grid", contentType = "fast_grid") {
                FastMusicGrid(onItemClick = { title ->
                    playerState.updatePlayback(title, wavvyArtist)
                })
            }

            // Recently played
            item(key = "recent_card", contentType = "recent_section") {
                RecentSection(onItemClick = { title ->
                    playerState.updatePlayback(title, wavvyArtist)
                })
            }

            // Explore cards
            item(key = "personalized", contentType = "personalized") {
                PersonalizedCard(onItemClick = { })
            }

            // Forgotten favorites
            item(key = "forgotten_favorites", contentType = "forgotten_favorites") {
                ForgottenFavoritesSection(onItemClick = { title ->
                    playerState.updatePlayback(title, forgottenFavoritesTitle)
                })
            }

            // Grouped discovery section
            item(key = "discovery_discovery") {
                SimilarDiscoverySection(
                    baseName = null,
                    artists = emptyList(),
                    songs = emptyList(),
                    onArtistClick = { title -> playerState.updatePlayback(title, wavvyArtist) },
                    onSongClick = { title -> playerState.updatePlayback(title, mixSuffix) }
                )
            }

            // Artists section
            item(key = "artists", contentType = "artists") {
                ArtistSection(onItemClick = { })
            }

            // Genres section
            item(key = "genres", contentType = "genres") {
                GenreSection(onItemClick = { })
            }

            // Moods section
            item(key = "moods", contentType = "moods") {
                MoodSection(onItemClick = { mood ->
                    playerState.updatePlayback(mood, mixSuffix)
                })
            }

            // Podcasts, Lives and IA
            item(key = "pilares", contentType = "pilares") {
                FinalPilaresSection(onItemClick = { title ->
                    if (!title.contains("IA")) playerState.updatePlayback(title, wavvySong)
                })
            }

            // Bottom padding
            item(key = "bottom_spacer", contentType = "spacer") {
                Spacer(modifier = Modifier.height(180.dp))
            }
        }
    }
}
