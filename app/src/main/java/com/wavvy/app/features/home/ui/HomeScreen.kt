package com.wavvy.app.features.home.ui

// Android and platform utilities
import android.annotation.SuppressLint
import android.content.res.Configuration
import java.util.Calendar
// Compose layouts and foundations
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
// Material 3 components
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
// Compose state and UI utilities
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
// Dependency injection
import org.koin.androidx.compose.koinViewModel
// Project resources and components
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.FilterPills
import com.wavvy.app.core.designsystem.components.SectionTitle
import com.wavvy.app.features.home.ui.components.ArtistSection
import com.wavvy.app.features.home.ui.components.FastMusicGrid
import com.wavvy.app.features.home.ui.components.ForgottenFavoritesSection
import com.wavvy.app.features.home.ui.components.GreetingSection
import com.wavvy.app.features.home.ui.components.HomeHeader
import com.wavvy.app.features.home.ui.components.LivesRow
import com.wavvy.app.features.home.ui.components.MoodSection
import com.wavvy.app.features.home.ui.components.PodcastsRow
import com.wavvy.app.features.home.ui.components.RadioOnlineCard
import com.wavvy.app.features.home.ui.components.RecentSection
import com.wavvy.app.features.home.ui.components.RecentTrack
import com.wavvy.app.features.home.ui.components.SimilarDiscoverySection

// Main home screen layout
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("LocalContextResourcesRead")
@Composable
fun HomeScreen(
    userName: String? = null,
    userHandle: String? = null,
    userProfilePicture: String? = null,
    playerState: PlayerState,
    viewModel: HomeViewModel = koinViewModel(),
    onNavigateToSettings: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Local state to track gestures independently of initial loading
    var isGestureRefreshing by remember { mutableStateOf(false) }
    val refreshState = rememberPullToRefreshState()

    // Dynamic greeting calculation
    LaunchedEffect(Unit) {
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

        val allGenres = context.resources.getStringArray(R.array.filter_genres)
        viewModel.initializeFiltersIfNeeded(allGenres, isLandscape)
    }

    // Refresh state sync
    LaunchedEffect(uiState.isLoadingQuickPicks) {
        if (!uiState.isLoadingQuickPicks) {
            isGestureRefreshing = false
        }
    }

    // String resources
    val defaultArtist = stringResource(R.string.default_artist_name)
    val defaultSong = stringResource(R.string.default_song_title)
    val mixSuffix = "Mix"

    // Section item click handler
    val onSectionItemClick: (String) -> Unit = { title ->
        if (!title.contains("IA")) {
            playerState.updatePlayback(
                title = title,
                artists = listOf(defaultSong)
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main pull-to-refresh container
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
                // Header section
                item(key = "header", contentType = "header") {
                    HomeHeader(
                        isAuthenticated = uiState.isAuthenticated,
                        isGuestActive = uiState.isGuestActive,
                        userName = uiState.initialName ?: userName,
                        userHandle = if (uiState.isAuthenticated) (uiState.initialHandle ?: userHandle) else null,
                        userProfilePicture = if (uiState.isAuthenticated) (uiState.initialPictureUrl ?: userProfilePicture) else null,
                        onNavigateToSettings = onNavigateToSettings,
                        onLoginClick = onNavigateToLogin,
                        onSignOutClick = {
                            viewModel.logout()
                            onNavigateToLogin()
                        }
                    )
                }

                // Greeting section
                item(key = "greeting", contentType = "greeting") {
                    uiState.greeting?.let { greeting ->
                        uiState.question?.let { question ->
                            val greetingName = if (uiState.isGuestActive && !uiState.isGuestNameCustom) {
                                null
                            } else {
                                uiState.initialName ?: userName
                            }
                            GreetingSection(
                                userName = greetingName,
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
                            val cleanArtistsList =
                                pick.artists.map { it.trim() }.filter { it.isNotBlank() }
                            val validatedArtists = cleanArtistsList.ifEmpty { listOf(defaultArtist) }
                            val thumbnail = pick.thumbnailUrl ?: ""

                            playerState.updatePlayback(
                                title = pick.title,
                                artists = validatedArtists,
                                imageUrl = thumbnail,
                                url = pick.videoId,
                                expand = false
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
                        },
                        onRemoveClick = { track -> viewModel.removeRecentTrack(track.id) }
                    )
                }

                // Forgotten favorites
                item(key = "forgotten_favorites", contentType = "forgotten_favorites") {
                    ForgottenFavoritesSection(
                        tracks = uiState.forgottenFavorites,
                        onItemClick = { trackId ->
                            val selected = uiState.forgottenFavorites.find { it.id == trackId }
                            if (selected != null) {
                                playerState.updatePlayback(
                                    title = selected.title,
                                    artists = listOf(selected.artist),
                                    imageUrl = "",
                                    url = selected.id,
                                    expand = false
                                )
                            }
                        }
                    )
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

                // Moods section
                item(key = "moods", contentType = "moods") {
                    Spacer(modifier = Modifier.height(24.dp))
                    MoodSection(onItemClick = onSectionItemClick)
                }

                // Online radio section
                item(key = "radio_online", contentType = "radio_online") {
                    Column {
                        Spacer(modifier = Modifier.height(24.dp))
                        SectionTitle(text = stringResource(R.string.section_title_online_radio))
                        RadioOnlineCard(onItemClick = onSectionItemClick)
                    }
                }

                // Podcasts section
                item(key = "podcasts", contentType = "podcasts") {
                    Spacer(modifier = Modifier.height(24.dp))
                    PodcastsRow(onItemClick = onSectionItemClick)
                }

                // Lives section
                item(key = "lives", contentType = "lives") {
                    Spacer(modifier = Modifier.height(24.dp))
                    LivesRow(onItemClick = onSectionItemClick)
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
