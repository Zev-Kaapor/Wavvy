package com.lonewolf.wavvy.ui.navigation

// Compose animations and foundations
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.lonewolf.wavvy.ui.common.navigation.FloatingNavBar
// Project screens and state
import com.lonewolf.wavvy.ui.home.HomeScreen
import com.lonewolf.wavvy.ui.home.PlayerState
import com.lonewolf.wavvy.ui.library.LibraryScreen
import com.lonewolf.wavvy.ui.player.PlayerSheet
import com.lonewolf.wavvy.ui.search.SearchScreen

// Main application container
@Composable
fun MainScreen() {
    // UI state management
    val playerState = rememberSaveable(saver = PlayerState.Saver) { PlayerState() }
    var currentRoute by remember { mutableStateOf(NavRoutes.HOME) }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Content area with smooth transitions
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = if (isLandscape) 72.dp else 0.dp)
        ) {
            AnimatedContent(
                targetState = currentRoute,
                transitionSpec = {
                    fadeIn(animationSpec = tween(250)) togetherWith
                            fadeOut(animationSpec = tween(250))
                },
                label = "screen_transition"
            ) { targetRoute ->
                // Screen selector
                when (targetRoute) {
                    NavRoutes.HOME -> HomeScreen(playerState = playerState)
                    NavRoutes.SEARCH -> SearchScreen(
                        playerState = playerState,
                        onNavigateBack = { currentRoute = NavRoutes.HOME }
                    )
                    NavRoutes.LIBRARY -> LibraryScreen(
                        onNavigateBack = { currentRoute = NavRoutes.HOME }
                    )
                }
            }
        }

        // Global player overlay
        PlayerIntegration(playerState)

        // Navigation overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(2f),
            contentAlignment = if (isLandscape) Alignment.CenterStart else Alignment.BottomCenter
        ) {
            FloatingNavBar(
                modifier = if (!isLandscape) Modifier.padding(horizontal = 16.dp) else Modifier,
                currentRoute = currentRoute,
                onHomeClick = { currentRoute = NavRoutes.HOME },
                onSearchClick = { currentRoute = NavRoutes.SEARCH },
                onLibraryClick = { currentRoute = NavRoutes.LIBRARY }
            )
        }
    }
}

// Player sheet integration
@Composable
fun PlayerIntegration(state: PlayerState) {
    if (state.isMiniPlayerActive && state.currentSongTitle.isNotEmpty()) {
        PlayerSheet(
            isExpanded = state.isPlayerExpanded,
            songTitle = state.currentSongTitle,
            artistName = state.currentArtistName,
            imageUrl = state.currentImageUrl,
            songUrl = state.currentSongUrl,
            onPillClick = { state.isPlayerExpanded = !state.isPlayerExpanded },
            onDismiss = {
                state.isMiniPlayerActive = false
                state.isPlayerExpanded = false
            },
            onProgressUpdate = { },
            isQueueActive = state.isQueueActive,
            onQueueToggle = { state.isQueueActive = !state.isQueueActive },
            modifier = Modifier
                .fillMaxSize()
                .zIndex(3f)
        )
    }
}
