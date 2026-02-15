package com.lonewolf.wavvy.ui.common

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
// Project screens and state
import com.lonewolf.wavvy.ui.home.HomeScreen
import com.lonewolf.wavvy.ui.home.PlayerState
import com.lonewolf.wavvy.ui.library.LibraryScreen
import com.lonewolf.wavvy.ui.player.PlayerSheet
import com.lonewolf.wavvy.ui.search.SearchScreen

// Navigation route constants
object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val LIBRARY = "library"
}

// Main application container
@Composable
fun MainScreen() {
    // Isolated player state
    val playerState = rememberSaveable(saver = PlayerState.Saver) { PlayerState() }

    // Navigation state
    var currentRoute by remember { mutableStateOf(Routes.HOME) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Content area with transitions
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = currentRoute,
                transitionSpec = {
                    fadeIn(animationSpec = tween(400)) togetherWith
                            fadeOut(animationSpec = tween(400))
                },
                label = "screen_transition"
            ) { targetRoute ->
                // Screen selector
                Box(modifier = Modifier.fillMaxSize()) {
                    when (targetRoute) {
                        Routes.HOME -> HomeScreen(playerState = playerState)
                        Routes.SEARCH -> SearchScreen(
                            onNavigateBack = { currentRoute = Routes.HOME }
                        )
                        Routes.LIBRARY -> LibraryScreen(
                            onNavigateBack = { currentRoute = Routes.HOME }
                        )
                    }
                }
            }
        }

        // Global player overlay
        PlayerIntegration(playerState)

        // Floating navigation bar
        Box(
            modifier = Modifier.fillMaxSize().zIndex(2f),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(modifier = Modifier.padding(bottom = 20.dp)) {
                FloatingNavBar(
                    currentRoute = currentRoute,
                    onHomeClick = { currentRoute = Routes.HOME },
                    onSearchClick = { currentRoute = Routes.SEARCH },
                    onLibraryClick = { currentRoute = Routes.LIBRARY }
                )
            }
        }
    }
}

// Player state and sheet controller
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
            modifier = Modifier.fillMaxSize().zIndex(3f)
        )
    }
}
