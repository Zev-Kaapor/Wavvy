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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lonewolf.wavvy.data.AuthRepositoryImpl
import com.lonewolf.wavvy.ui.auth.AuthManager
import com.lonewolf.wavvy.ui.common.navigation.DockedNavBar
// Project screens and state
import com.lonewolf.wavvy.ui.home.HomeScreen
import com.lonewolf.wavvy.ui.home.HomeViewModel
import com.lonewolf.wavvy.ui.home.HomeViewModelFactory
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

    // Authentication ecosystem infrastructure
    val context = LocalContext.current
    val authManager = remember { AuthManager(context) }
    val authRepository = remember { AuthRepositoryImpl(context) }
    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(authManager, authRepository)
    )

    // Dynamic state mapping
    val uiState by homeViewModel.uiState.collectAsState()

    var userName by rememberSaveable { mutableStateOf<String?>(null) }
    var userHandle by rememberSaveable { mutableStateOf<String?>(null) }
    var userProfilePicture by rememberSaveable { mutableStateOf<String?>(null) }

    // Persistent synchronization layer for application session state
    LaunchedEffect(uiState.isAuthenticated, uiState.initialName, uiState.initialHandle, uiState.initialPictureUrl) {
        if (uiState.isAuthenticated) {
            userName = uiState.initialName
            userHandle = uiState.initialHandle
            userProfilePicture = uiState.initialPictureUrl
        } else {
            userName = null
            userHandle = null
            userProfilePicture = null
        }
    }

    // Layout configuration
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // Track active embedded browser interactions
    val isAuthWebViewOpen = uiState.authUrl != null

    // Root container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Content area - Pure immersive layout
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = if (isLandscape && !isAuthWebViewOpen) 125.dp else 0.dp)
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
                    NavRoutes.HOME -> HomeScreen(
                        userName = userName,
                        userHandle = userHandle,
                        userProfilePicture = userProfilePicture,
                        playerState = playerState,
                        viewModel = homeViewModel,
                        onUserCaptured = { name, handle, picture ->
                            userName = name
                            userHandle = handle
                            userProfilePicture = picture
                        }
                    )
                    NavRoutes.SEARCH -> SearchScreen(
                        playerState = playerState,
                        onNavigateBack = { currentRoute = NavRoutes.HOME }
                    )
                    NavRoutes.LIBRARY -> LibraryScreen(
                        isAuthenticated = uiState.isAuthenticated,
                        userHandle = userHandle,
                        userProfilePicture = userProfilePicture,
                        onLoginClick = { homeViewModel.loginWithGoogle() },
                        onSignOutClick = { homeViewModel.logout() },
                        onNavigateBack = { currentRoute = NavRoutes.HOME }
                    )
                }
            }
        }

        // Global player overlay - hidden during authentication to prevent overlaps
        if (!isAuthWebViewOpen) {
            PlayerIntegration(playerState)
        }

        // Navigation overlay - animated visibility for smooth transitions
        AnimatedVisibility(
            visible = !isAuthWebViewOpen,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(2f)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = if (isLandscape) Alignment.CenterStart else Alignment.BottomCenter
            ) {
                DockedNavBar(
                    modifier = Modifier,
                    currentRoute = currentRoute,
                    onHomeClick = { currentRoute = NavRoutes.HOME },
                    onSearchClick = { currentRoute = NavRoutes.SEARCH },
                    onLibraryClick = { currentRoute = NavRoutes.LIBRARY }
                )
            }
        }

        // Account switch overlay
        AnimatedVisibility(
            visible = uiState.isSwitchingAccount,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(10f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
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
            artistNames = state.currentArtistNames,
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
