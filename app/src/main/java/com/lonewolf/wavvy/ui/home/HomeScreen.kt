package com.lonewolf.wavvy.ui.home

// Compose foundation and layout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
// State management
import androidx.compose.runtime.*
// Tools and positioning
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
// Shared and internal components
import com.lonewolf.wavvy.ui.common.FloatingNavBar
import com.lonewolf.wavvy.ui.common.HomeHeader
import com.lonewolf.wavvy.ui.player.PlayerSheet
import com.lonewolf.wavvy.ui.home.components.*

// State holder for playback UI
@Stable
class PlayerState(
    isMiniPlayerActive: Boolean = false,
    isPlayerExpanded: Boolean = false,
    currentSongTitle: String = "",
    currentArtistName: String = "",
    currentImageUrl: String? = null
) {
    var isMiniPlayerActive by mutableStateOf(isMiniPlayerActive)
    var isPlayerExpanded by mutableStateOf(isPlayerExpanded)
    var currentSongTitle by mutableStateOf(currentSongTitle)
    var currentArtistName by mutableStateOf(currentArtistName)
    var currentImageUrl by mutableStateOf(currentImageUrl)

    // Update playback state
    fun updatePlayback(title: String, artist: String) {
        currentSongTitle = title
        currentArtistName = artist
        currentImageUrl = null
        isMiniPlayerActive = true
        isPlayerExpanded = false
    }
}

// Main screen entry point
@Composable
fun HomeScreen(userName: String? = null) {
    // Isolated player state
    val playerState = remember { PlayerState() }

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
                    playerState.updatePlayback(title, "Wavvy Artist")
                })
            }

            // Recently played
            item(key = "recent_card", contentType = "recent_section") {
                RecentSection(onItemClick = { title ->
                    playerState.updatePlayback(title, "Wavvy Artist")
                })
            }

            // Explore cards
            item(key = "personalized", contentType = "personalized") {
                PersonalizedCard(onItemClick = { })
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
                    playerState.updatePlayback(mood, "Mix")
                })
            }

            // Podcasts, Lives and IA
            item(key = "pilares", contentType = "pilares") {
                FinalPilaresSection(onItemClick = { title ->
                    if (!title.contains("IA")) playerState.updatePlayback(title, "Wavvy")
                })
            }

            // Bottom padding
            item(key = "bottom_spacer", contentType = "spacer") {
                Spacer(modifier = Modifier.height(180.dp))
            }
        }

        // Navigation bar overlay
        Box(
            modifier = Modifier.fillMaxSize().zIndex(2f),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(modifier = Modifier.padding(bottom = 20.dp)) {
                FloatingNavBar()
            }
        }

        // Playback integration
        PlayerIntegration(playerState)
    }
}

// Scoped player recomposition wrapper
@Composable
fun PlayerIntegration(state: PlayerState) {
    if (state.isMiniPlayerActive && state.currentSongTitle.isNotEmpty()) {
        PlayerSheet(
            isExpanded = state.isPlayerExpanded,
            songTitle = state.currentSongTitle,
            artistName = state.currentArtistName,
            imageUrl = state.currentImageUrl,
            onPillClick = { state.isPlayerExpanded = !state.isPlayerExpanded },
            onDismiss = {
                state.isMiniPlayerActive = false
                state.isPlayerExpanded = false
            },
            onProgressUpdate = { },
            modifier = Modifier.fillMaxSize().zIndex(3f)
        )
    }
}
