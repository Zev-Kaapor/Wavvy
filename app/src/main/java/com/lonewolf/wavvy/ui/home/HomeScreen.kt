package com.lonewolf.wavvy.ui.home

import android.annotation.SuppressLint
import java.util.Calendar
// Lifecycle and state management
import androidx.activity.compose.BackHandler
// Animations
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
// Compose foundation and layout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
// State management
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
// Tools and positioning
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
// Lifecycle and ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.auth.EmbeddedAuthWebView
import com.lonewolf.wavvy.ui.common.components.FilterPills
// Shared and internal components
import com.lonewolf.wavvy.ui.home.components.HomeHeader
import com.lonewolf.wavvy.ui.home.components.*
import com.lonewolf.wavvy.ui.player.PlayerViewModel

// State holder for playback UI
@Stable
class PlayerState(
    isMiniPlayerActive: Boolean = false,
    isPlayerExpanded: Boolean = false,
    isQueueActive: Boolean = false,
    currentSongTitle: String = "",
    currentArtistNames: List<String> = emptyList(),
    currentImageUrl: String? = null,
    currentSongUrl: String? = null
) {
    var isMiniPlayerActive by mutableStateOf(isMiniPlayerActive)
    var isPlayerExpanded by mutableStateOf(isPlayerExpanded)
    var isQueueActive by mutableStateOf(isQueueActive)
    var currentSongTitle by mutableStateOf(currentSongTitle)
    var currentArtistNames by mutableStateOf(currentArtistNames)
    var currentImageUrl by mutableStateOf(currentImageUrl)
    var currentSongUrl by mutableStateOf(currentSongUrl)

    // Update playback state
    fun updatePlayback(title: String, artists: List<String>, imageUrl: String? = null, url: String? = null, expand: Boolean = false) {
        currentSongTitle = title
        currentArtistNames = artists
        currentImageUrl = imageUrl
        currentSongUrl = url
        isMiniPlayerActive = true
        isPlayerExpanded = expand
        isQueueActive = false
    }

    // Play all quick choices songs
    fun playAllQuickChoices(artists: List<String>, mixTitle: String) {
        currentSongTitle = mixTitle
        currentArtistNames = artists
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
                    it.currentArtistNames,
                    it.currentImageUrl,
                    it.currentSongUrl
                )
            },
            restore = {
                @Suppress("UNCHECKED_CAST")
                PlayerState(
                    isMiniPlayerActive = it[0] as Boolean,
                    isPlayerExpanded = it[1] as Boolean,
                    isQueueActive = it[2] as Boolean,
                    currentSongTitle = it[3] as String,
                    currentArtistNames = it[4] as List<String>,
                    currentImageUrl = it[5] as? String,
                    currentSongUrl = it[6] as? String
                )
            }
        )
    }
}

// Main screen entry point
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("LocalContextResourcesRead")
@Composable
fun HomeScreen(
    userName: String? = null,
    userHandle: String? = null,
    userProfilePicture: String? = null,
    playerState: PlayerState,
    viewModel: HomeViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel(),
    onNavigateToSettings: () -> Unit,
    onUserCaptured: (String?, String?, String?) -> Unit = { _, _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // Local state to track gestures independently of init loading
    var isGestureRefreshing by remember { mutableStateOf(false) }
    val refreshState = rememberPullToRefreshState()

    // Persistent synchronization layer for application session state
    LaunchedEffect(isLandscape) {
        // Greetings logic
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val (resGreetings, resQuestions) = when (hour) {
            in 0..5 -> R.array.dawn_greetings to R.array.dawn_questions
            in 6..11 -> R.array.morning_greetings to R.array.morning_questions
            in 12..17 -> R.array.afternoon_greetings to R.array.afternoon_questions
            else -> R.array.evening_greetings to R.array.evening_questions
        }
        viewModel.updateGreetingIfNeeded(
            context.resources.getStringArray(resGreetings),
            context.resources.getStringArray(resQuestions)
        )

        // Persistent filter logic
        val allMoods = context.resources.getStringArray(R.array.filter_moods)
        viewModel.initializeFiltersIfNeeded(allMoods, isLandscape)
    }

    // Persistent synchronization layer for application session state
    LaunchedEffect(uiState.isLoadingQuickPicks) {
        if (!uiState.isLoadingQuickPicks) {
            isGestureRefreshing = false
        }
    }

    // String resources
    val forgottenFavoritesTitle = stringResource(R.string.section_title_forgotten_favorites)
    val defaultArtist = stringResource(R.string.default_artist_name)
    val defaultSong = stringResource(R.string.default_song_title)
    val mixSuffix = "Mix"

    Box(modifier = Modifier.fillMaxSize()) {
        // WebView authentication overlay with fade animations
        AnimatedVisibility(
            visible = uiState.authUrl != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(20f)
        ) {
            BackHandler {
                viewModel.cancelWebLogin()
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
            ) {
                if (uiState.authUrl != null) {
                    EmbeddedAuthWebView(
                        authUrl = uiState.authUrl!!,
                        redirectUri = "https://music.youtube.com",
                        onTokenCaptured = { token ->
                            viewModel.onTokenReceived(token) { name: String?, handle: String?, picture: String? ->
                                onUserCaptured(name, handle, picture)
                            }
                        },
                        onErrorReceived = { viewModel.cancelWebLogin() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Content layout container capturing scroll metrics natively
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullToRefresh(
                    isRefreshing = isGestureRefreshing,
                    state = refreshState,
                    onRefresh = {
                        viewModel.refreshQuickPicks()
                        isGestureRefreshing = true
                    }
                )
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // Immersive Header
                item(key = "header", contentType = "header") {
                    HomeHeader(
                        isAuthenticated = uiState.isAuthenticated,
                        userName = if (uiState.isAuthenticated) (uiState.initialName ?: userName) else null,
                        userHandle = if (uiState.isAuthenticated) (uiState.initialHandle ?: userHandle) else null,
                        userProfilePicture = if (uiState.isAuthenticated) (uiState.initialPictureUrl ?: userProfilePicture) else null,
                        onNavigateToSettings = onNavigateToSettings,
                        onLoginClick = { viewModel.loginWithGoogle() },
                        onSignOutClick = { viewModel.logout() },
                        onSwitchAccount = { viewModel.switchAccount() },
                        onAccountSelected = { account ->
                            viewModel.loginWithSavedAccount(account) { name, handle, picture ->
                                onUserCaptured(name, handle, picture)
                            }
                        },
                        savedAccounts = uiState.savedAccounts,
                        showAccountSwitcher = uiState.showAccountSwitcher,
                        onDismissAccountSwitcher = { viewModel.dismissAccountSwitcher() }
                    )
                }

                // User greeting
                item(key = "greeting", contentType = "greeting") {
                    uiState.greeting?.let { greeting ->
                        uiState.question?.let { question ->
                            GreetingSection(
                                userName = if (uiState.isAuthenticated) (uiState.initialName ?: userName) else null,
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
                        onInitializeFilters = { }
                    )
                }

                // Quick choices grid
                item(key = "fast_grid", contentType = "fast_grid") {
                    FastMusicGrid(
                        quickPicks = uiState.quickPicks,
                        isLoading = uiState.isLoadingQuickPicks,
                        onItemClick = { pick ->
                            // Clean and validate the artist list exactly like FastMusicCard does
                            val cleanArtistsList = pick.artists.map { it.trim() }.filter { it.isNotBlank() }
                            val validatedArtists = if (cleanArtistsList.isNotEmpty()) {
                                cleanArtistsList
                            } else {
                                listOf(defaultArtist)
                            }

                            playerState.updatePlayback(
                                title = pick.title,
                                artists = validatedArtists,
                                imageUrl = pick.thumbnailUrl,
                                url = pick.videoId,
                                expand = false
                            )
                            playerViewModel.loadAndPlay(
                                youtubeUrl = "https://www.youtube.com/watch?v=${pick.videoId}",
                                title = pick.title,
                                artist = validatedArtists.joinToString(", "),
                                imageUrl = pick.thumbnailUrl ?: ""
                            )
                        },
                        onPlayAllClick = {
                            playerState.playAllQuickChoices(
                                artists = listOf(defaultArtist),
                                mixTitle = "$defaultArtist $mixSuffix"
                            )
                        }
                    )
                }

                // Recently played
                item(key = "recent_card", contentType = "recent_section") {
                    RecentSection(
                        tracks = uiState.recentTracks,
                        onItemClick = { track ->
                            playerState.updatePlayback(
                                title = track.title,
                                artists = listOf(track.artist),
                                imageUrl = track.imageUrl,
                                url = track.id,
                                expand = false
                            )
                            playerViewModel.loadAndPlay(
                                youtubeUrl = "https://www.youtube.com/watch?v=${track.id}",
                                title = track.title,
                                artist = track.artist,
                                imageUrl = track.imageUrl
                            )
                        }
                    )
                }

                // Explore cards
                item(key = "personalized", contentType = "personalized") {
                    PersonalizedCard(onItemClick = { })
                }

                // Forgotten favorites
                item(key = "forgotten_favorites", contentType = "forgotten_favorites") {
                    ForgottenFavoritesSection(onItemClick = { title ->
                        playerState.updatePlayback(
                            title = title,
                            artists = listOf(forgottenFavoritesTitle)
                        )
                    })
                }

                // Grouped discovery section
                item(key = "discovery_discovery") {
                    SimilarDiscoverySection(
                        baseName = null,
                        artists = emptyList(),
                        songs = emptyList(),
                        onArtistClick = { title ->
                            playerState.updatePlayback(
                                title = title,
                                artists = listOf(defaultArtist)
                            )
                        },
                        onSongClick = { title ->
                            playerState.updatePlayback(
                                title = title,
                                artists = listOf(mixSuffix)
                            )
                        }
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
                        playerState.updatePlayback(
                            title = mood,
                            artists = listOf(mixSuffix)
                        )
                    })
                }

                // Podcasts, Lives and IA
                item(key = "pilares", contentType = "pilares") {
                    FinalPilaresSection(onItemClick = { title ->
                        if (!title.contains("IA")) {
                            playerState.updatePlayback(
                                title = title,
                                artists = listOf(defaultSong)
                            )
                        }
                    })
                }

                // Bottom padding
                item(key = "bottom_spacer", contentType = "spacer") {
                    Spacer(modifier = Modifier.height(180.dp))
                }
            }

            // Material 3 indicator tied strictly to manual gesture refreshing state
            PullToRefreshDefaults.Indicator(
                state = refreshState,
                isRefreshing = isGestureRefreshing,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 170.dp)
            )
        }
    }
}
