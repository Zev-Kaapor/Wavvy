package com.lonewolf.wavvy.ui.home

import android.annotation.SuppressLint
import java.util.Calendar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
// Lifecycle and ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.common.components.FilterPills
// Shared and internal components
import com.lonewolf.wavvy.ui.home.components.HomeHeader
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
    fun updatePlayback(title: String, artist: String, url: String? = null, expand: Boolean = false) {
        currentSongTitle = title
        currentArtistName = artist
        currentImageUrl = null
        currentSongUrl = url
        isMiniPlayerActive = true
        isPlayerExpanded = expand
        isQueueActive = false
    }

    // Play all quick choices songs
    fun playAllQuickChoices(artist: String) {
        currentSongTitle = "$artist Mix"
        currentArtistName = artist
        currentImageUrl = null
        currentSongUrl = null
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
@SuppressLint("LocalContextResourcesRead")
@Composable
fun HomeScreen(
    userName: String? = null,
    playerState: PlayerState,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    // Initialize or update greeting based on time and persistence logic
    LaunchedEffect(Unit) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val (resGreetings, resQuestions) = when (hour) {
            in 0..5 -> R.array.dawn_greetings to R.array.dawn_questions
            in 6..11 -> R.array.morning_greetings to R.array.morning_questions
            in 12..17 -> R.array.afternoon_greetings to R.array.afternoon_questions
            else -> R.array.evening_greetings to R.array.evening_questions
        }
        val greetings = context.resources.getStringArray(resGreetings)
        val questions = context.resources.getStringArray(resQuestions)

        viewModel.updateGreetingIfNeeded(greetings, questions)
    }
    // String resources for dynamic content
    val forgottenFavoritesTitle = stringResource(R.string.section_title_forgotten_favorites)
    val defaultArtist = stringResource(R.string.default_artist_name)
    val defaultSong = stringResource(R.string.default_song_title)
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
                uiState.greeting?.let { greeting ->
                    uiState.question?.let { question ->
                        GreetingSection(
                            userName = userName,
                            greetingTemplate = greeting,
                            question = question
                        )
                    }
                }
            }

            // Category filters
            item(key = "filters", contentType = "filters") {
                FilterPills(
                    availableFilters = uiState.availableFilters,
                    selectedFilter = uiState.selectedFilter,
                    onFilterSelected = { viewModel.onFilterSelected(it) },
                    onInitializeFilters = { viewModel.setAvailableFilters(it) }
                )
            }

            // Quick choices grid
            item(key = "fast_grid", contentType = "fast_grid") {
                FastMusicGrid(
                    onItemClick = { title ->
                        playerState.updatePlayback(title, defaultArtist)
                    },
                    onPlayAllClick = {
                        // Play all quick choices songs
                        playerState.playAllQuickChoices(defaultArtist)
                    }
                )
            }

            // Recently played
            item(key = "recent_card", contentType = "recent_section") {
                RecentSection(onItemClick = { title ->
                    playerState.updatePlayback(title, defaultArtist)
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
                    onArtistClick = { title -> playerState.updatePlayback(title, defaultArtist) },
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
                    if (!title.contains("IA")) playerState.updatePlayback(title, defaultSong)
                })
            }

            // Bottom padding
            item(key = "bottom_spacer", contentType = "spacer") {
                Spacer(modifier = Modifier.height(180.dp))
            }
        }
    }
}
