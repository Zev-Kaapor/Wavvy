package com.wavvy.app.core.navigation

// Compose animations and foundations
import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.koin.androidx.compose.koinViewModel
// Core design system components
import com.wavvy.app.core.designsystem.components.DockedNavBar
import com.wavvy.app.core.designsystem.components.CustomToast
import com.wavvy.app.core.designsystem.components.ToastData
import com.wavvy.app.core.designsystem.bottomsheet.LocalMenuState
import com.wavvy.app.core.designsystem.bottomsheet.PreviewBottomSheetContent
import com.wavvy.app.core.designsystem.theme.ThemeMode
// Features definitions
import com.wavvy.app.features.auth.ui.screens.AuthScreen
import com.wavvy.app.features.auth.ui.viewmodel.AuthViewModel
import com.wavvy.app.features.home.ui.HomeScreen
import com.wavvy.app.features.home.ui.HomeViewModel
import com.wavvy.app.features.home.ui.PlayerState
import com.wavvy.app.features.library.ui.LibraryScreen
import com.wavvy.app.features.player.ui.PlayerSheet
import com.wavvy.app.features.player.ui.PlayerViewModel
import com.wavvy.app.features.player.ui.components.PlayerMoreOptions
import com.wavvy.app.features.search.ui.SearchScreen
import com.wavvy.app.features.discover.ui.DiscoverScreen
import com.wavvy.app.features.settings.ui.SettingsScreen

// Main application navigation orchestrator
@Suppress("UNUSED_PARAMETER")
@Composable
fun MainScreen(
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    currentDefaultTab: DefaultTab,
    onDefaultTabChange: (DefaultTab) -> Unit,
    homeViewModel: HomeViewModel = koinViewModel(),
    authViewModel: AuthViewModel = koinViewModel(),
    playerViewModel: PlayerViewModel = koinViewModel()
) {
    val playerState = rememberSaveable(saver = PlayerState.Saver) { PlayerState() }
    var currentRoute by rememberSaveable { mutableStateOf(NavRoutes.SPLASH) }
    var previousRoute by rememberSaveable { mutableStateOf<String?>(null) }
    val settingsScrollState = rememberScrollState()

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var activeToast by remember { mutableStateOf<ToastData?>(null) }
    val currentMediaItem by playerViewModel.currentMediaItem.collectAsState()

    // Hide active keyboard on navigation changes
    LaunchedEffect(currentRoute) {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    // Sync playback system status
    LaunchedEffect(currentMediaItem) {
        if (currentMediaItem != null) {
            playerState.isMiniPlayerActive = true
        }
    }

    val uiState by homeViewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Explicit condition check for system visibility parameters
    val shouldHideNavBar = currentRoute == NavRoutes.SPLASH ||
            currentRoute == NavRoutes.AUTH ||
            currentRoute == NavRoutes.SETTINGS

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val contentStartPadding by animateDpAsState(
            targetValue = if (isLandscape && !shouldHideNavBar) 125.dp else 0.dp,
            animationSpec = tween(300),
            label = "content_start_padding"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = contentStartPadding)
                .zIndex(0f)
        ) {
            AnimatedContent(
                targetState = currentRoute,
                transitionSpec = {
                    // Smooth crossfade with explicit duration matching
                    fadeIn(animationSpec = tween(350)) togetherWith fadeOut(animationSpec = tween(350))
                },
                label = "screen_transition"
            ) { targetRoute ->
                when (targetRoute) {
                    NavRoutes.SPLASH -> SplashScreen(
                        onInitializationComplete = { isLoggedIn ->
                            currentRoute = if (isLoggedIn) currentDefaultTab.route else NavRoutes.AUTH
                        }
                    )
                    NavRoutes.AUTH -> AuthScreen(
                        viewModel = authViewModel,
                        currentTheme = currentTheme,
                        onThemeSelected = onThemeChange,
                        onAuthSuccess = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            homeViewModel.refreshAfterAuth()
                            currentRoute = currentDefaultTab.route
                        }
                    )
                    NavRoutes.HOME -> HomeScreen(
                        playerState = playerState,
                        viewModel = homeViewModel,
                        onNavigateToSettings = {
                            previousRoute = currentRoute
                            currentRoute = NavRoutes.SETTINGS
                        },
                        onNavigateToLogin = { currentRoute = NavRoutes.AUTH }
                    )
                    NavRoutes.SEARCH -> SearchScreen(
                        playerState = playerState,
                        onNavigateBack = { currentRoute = NavRoutes.HOME }
                    )
                    NavRoutes.DISCOVER -> DiscoverScreen(
                        playerState = playerState,
                        isAuthenticated = uiState.isAuthenticated,
                        isGuestActive = uiState.isGuestActive,
                        userName = uiState.initialName,
                        userHandle = uiState.initialHandle,
                        userProfilePicture = uiState.initialPictureUrl,
                        onNavigateToLogin = { currentRoute = NavRoutes.AUTH },
                        onSignOut = {
                            homeViewModel.logout()
                            currentRoute = NavRoutes.AUTH
                        },
                        onNavigateToSettings = {
                            previousRoute = currentRoute
                            currentRoute = NavRoutes.SETTINGS
                        }
                    )
                    NavRoutes.LIBRARY -> LibraryScreen(
                        isAuthenticated = uiState.isAuthenticated,
                        isGuestActive = uiState.isGuestActive,
                        userName = uiState.initialName,
                        userHandle = uiState.initialHandle,
                        userProfilePicture = uiState.initialPictureUrl,
                        onLoginClick = { currentRoute = NavRoutes.AUTH },
                        onSignOutClick = {
                            homeViewModel.logout()
                            currentRoute = NavRoutes.AUTH
                        },
                        onNavigateToSettings = {
                            previousRoute = currentRoute
                            currentRoute = NavRoutes.SETTINGS
                        },
                        onNavigateBack = { currentRoute = NavRoutes.HOME }
                    )
                    NavRoutes.SETTINGS -> SettingsScreen(
                        scrollState = settingsScrollState,
                        onNavigateBack = { currentRoute = previousRoute ?: NavRoutes.HOME },
                        onShowToast = { activeToast = it }
                    )
                }
            }
        }

        // Ambient background gradient
        val targetBottomMargin = if (isLandscape) 20.dp else if (shouldHideNavBar) 20.dp else 93.dp
        val gradientHeightOffset = if (isLandscape) 50.dp else 160.dp

        AnimatedVisibility(
            visible = playerState.isMiniPlayerActive,
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
                    currentRoute = currentRoute,
                    onHomeClick = { currentRoute = NavRoutes.HOME },
                    onSearchClick = { currentRoute = NavRoutes.SEARCH },
                    onDiscoverClick = { currentRoute = NavRoutes.DISCOVER },
                    onLibraryClick = { currentRoute = NavRoutes.LIBRARY }
                )
            }
        }

        // Global Toast notification layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(5f),
            contentAlignment = Alignment.BottomCenter
        ) {
            val toastBottomPadding by remember(shouldHideNavBar, playerState.isMiniPlayerActive) {
                derivedStateOf {
                    when {
                        playerState.isMiniPlayerActive -> if (shouldHideNavBar) 95.dp else 176.dp
                        else -> if (shouldHideNavBar) 20.dp else 88.dp
                    }
                }
            }

            AnimatedContent(
                targetState = activeToast,
                transitionSpec = {
                    (slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)))
                        .togetherWith(slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300)))
                },
                contentKey = { it?.id },
                label = "toast_transition"
            ) { toast ->
                if (toast != null) {
                    CustomToast(
                        message = toast.message,
                        subtitle = toast.subtitle,
                        durationMillis = toast.durationMillis,
                        onDismiss = { if (activeToast?.id == toast.id) activeToast = null },
                        modifier = Modifier.padding(bottom = toastBottomPadding)
                    )
                }
            }
        }

        // Expanded system player integration
        PlayerIntegration(
            state = playerState,
            viewModel = playerViewModel,
            isNavBarVisible = !shouldHideNavBar,
            showBorder = currentRoute == NavRoutes.SETTINGS,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(4f)
        )

        // More options menu overlay
        val menuState = LocalMenuState.current
        if (menuState.isVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(30f),
                contentAlignment = Alignment.BottomCenter
            ) {
                PlayerMoreOptions(
                    onDismiss = { menuState.dismiss() }
                ) {
                    PreviewBottomSheetContent(
                        onOptionsClicked = { menuState.dismiss() }
                    )
                }
            }
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
