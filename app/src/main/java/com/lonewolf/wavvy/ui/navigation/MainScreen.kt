package com.lonewolf.wavvy.ui.navigation

// Compose animations and foundations
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lonewolf.wavvy.data.AuthRepositoryImpl
import com.lonewolf.wavvy.data.RecentHistoryManager
import com.lonewolf.wavvy.data.SearchHistoryManager
import com.lonewolf.wavvy.ui.auth.AuthManager
import com.lonewolf.wavvy.ui.common.components.DockedNavBar
import com.lonewolf.wavvy.ui.common.components.CustomToast
import com.lonewolf.wavvy.ui.common.components.ToastData
// Project screens and state
import com.lonewolf.wavvy.ui.home.HomeScreen
import com.lonewolf.wavvy.ui.home.HomeViewModel
import com.lonewolf.wavvy.ui.home.HomeViewModelFactory
import com.lonewolf.wavvy.ui.home.PlayerState
import com.lonewolf.wavvy.ui.library.LibraryScreen
import com.lonewolf.wavvy.ui.player.PlayerSheet
import com.lonewolf.wavvy.ui.player.PlayerViewModel
import com.lonewolf.wavvy.ui.search.SearchScreen
import com.lonewolf.wavvy.ui.settings.SettingsScreen
import com.lonewolf.wavvy.ui.theme.ThemeMode

// Main application navigation orchestrator
@Composable
fun MainScreen(
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    currentDefaultTab: DefaultTab,
    onDefaultTabChange: (DefaultTab) -> Unit
) {
    val playerState = rememberSaveable(saver = PlayerState.Saver) { PlayerState() }
    var currentRoute by rememberSaveable { mutableStateOf(currentDefaultTab.route) }
    val settingsScrollState = rememberScrollState()

    // Global notification toast state
    var activeToast by remember { mutableStateOf<ToastData?>(null) }

    val playerViewModel: PlayerViewModel = viewModel()
    val currentMediaItem by playerViewModel.currentMediaItem.collectAsState()

    // Sync playback system status
    LaunchedEffect(currentMediaItem) {
        if (currentMediaItem != null) {
            playerState.isMiniPlayerActive = true
        }
    }

    val context = LocalContext.current
    val authManager = remember { AuthManager(context) }
    val authRepository = remember { AuthRepositoryImpl(context) }
    val recentHistoryManager = remember { RecentHistoryManager(context) }
    val searchHistoryManager = remember { SearchHistoryManager(context) }
    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(authManager, authRepository, recentHistoryManager)
    )

    val uiState by homeViewModel.uiState.collectAsState()

    var userName by rememberSaveable { mutableStateOf<String?>(null) }
    var userHandle by rememberSaveable { mutableStateOf<String?>(null) }
    var userProfilePicture by rememberSaveable { mutableStateOf<String?>(null) }

    // Session status validation
    LaunchedEffect(
        uiState.isAuthenticated,
        uiState.initialName,
        uiState.initialHandle,
        uiState.initialPictureUrl
    ) {
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

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isAuthWebViewOpen = uiState.authUrl != null
    val shouldHideNavBar = isAuthWebViewOpen || currentRoute == NavRoutes.SETTINGS

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main view content wrapper
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = if (isLandscape && !shouldHideNavBar) 125.dp else 0.dp)
                .zIndex(0f)
        ) {
            AnimatedContent(
                targetState = currentRoute,
                transitionSpec = {
                    fadeIn(animationSpec = tween(250)) togetherWith
                            fadeOut(animationSpec = tween(250))
                },
                label = "screen_transition"
            ) { targetRoute ->
                when (targetRoute) {
                    NavRoutes.HOME -> HomeScreen(
                        userName = userName,
                        userHandle = userHandle,
                        userProfilePicture = userProfilePicture,
                        playerState = playerState,
                        viewModel = homeViewModel,
                        onNavigateToSettings = { currentRoute = NavRoutes.SETTINGS },
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
                        userName = userName,
                        userHandle = userHandle,
                        userProfilePicture = userProfilePicture,
                        onLoginClick = { homeViewModel.loginWithGoogle() },
                        onSignOutClick = { homeViewModel.logout() },
                        onNavigateBack = { currentRoute = NavRoutes.HOME }
                    )

                    NavRoutes.SETTINGS -> SettingsScreen(
                        queueLimit = 50,
                        onQueueLimitChange = { },
                        onClearPlaybackHistory = {
                            recentHistoryManager.clearAll()
                        },
                        onClearSearchHistory = {
                            searchHistoryManager.clearAll()
                        },
                        onShowToast = { toast ->
                            activeToast = toast
                        },
                        onNavigateBack = { currentRoute = NavRoutes.HOME },
                        scrollState = settingsScrollState,
                        isPlayerActive = playerState.isMiniPlayerActive,
                        currentTheme = currentTheme,
                        onThemeChange = onThemeChange,
                        currentDefaultTab = currentDefaultTab,
                        onDefaultTabChange = onDefaultTabChange
                    )                }
            }
        }

        // Ambient background gradient
        val navInsets = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val isGestureMode = navInsets <= 24.dp
        val navBarBottom = if (isGestureMode) 20.dp else navInsets + 8.dp
        val targetBottomMargin = if (isLandscape) {
            20.dp
        } else {
            if (shouldHideNavBar) navBarBottom else navBarBottom + 68.dp + 5.dp
        }

        val gradientHeightOffset = if (isLandscape) 50.dp else 160.dp

        AnimatedVisibility(
            visible = !isAuthWebViewOpen && playerState.isMiniPlayerActive,
            enter = fadeIn(animationSpec = tween(400)),
            exit = fadeOut(animationSpec = tween(400)),
            modifier = Modifier
                .fillMaxWidth()
                .height(targetBottomMargin + gradientHeightOffset)
                .align(Alignment.BottomCenter)
                .zIndex(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )
        }

        // Core navigation dock container
        AnimatedVisibility(
            visible = !shouldHideNavBar,
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

        // Global Toast notification layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(3f),
            contentAlignment = Alignment.BottomCenter
        ) {
            val toastBottomPadding = when {
                shouldHideNavBar && !playerState.isMiniPlayerActive -> 20.dp
                shouldHideNavBar && playerState.isMiniPlayerActive -> 95.dp
                else -> 176.dp
            }

            AnimatedVisibility(
                visible = activeToast != null,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            ) {
                activeToast?.let { toast ->
                    CustomToast(
                        message = toast.message,
                        subtitle = toast.subtitle,
                        durationMillis = toast.durationMillis,
                        onDismiss = { activeToast = null },
                        modifier = Modifier.padding(bottom = toastBottomPadding)
                    )
                }
            }
        }

        // Expanded system player integration
        if (!isAuthWebViewOpen) {
            PlayerIntegration(
                state = playerState,
                viewModel = playerViewModel,
                isNavBarVisible = !shouldHideNavBar,
                showBorder = currentRoute == NavRoutes.SETTINGS,
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(4f)
            )
        }

        // Authentication transition view overlay
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

// Media player container wrapper component
@Composable
fun PlayerIntegration(
    state: PlayerState,
    viewModel: PlayerViewModel,
    isNavBarVisible: Boolean,
    showBorder: Boolean,
    modifier: Modifier = Modifier
) {
    if (state.isMiniPlayerActive) {
        PlayerSheet(
            isExpanded = state.isPlayerExpanded,
            imageUrl = state.currentImageUrl,
            songUrl = state.currentSongUrl,
            initialTitle = state.currentSongTitle,
            initialArtist = state.currentArtistNames.joinToString(", "),
            onPillClick = { state.isPlayerExpanded = !state.isPlayerExpanded },
            onDismiss = {
                state.isMiniPlayerActive = false
                state.isPlayerExpanded = false
            },
            onProgressUpdate = { },
            isQueueActive = state.isQueueActive,
            onQueueToggle = { state.isQueueActive = !state.isQueueActive },
            isNavBarVisible = isNavBarVisible,
            showBorder = showBorder,
            viewModel = viewModel,
            modifier = modifier
        )
    }
}
