package com.wavvy.app.core.navigation

// Compose animation and layouts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
// Material 3 components
import androidx.compose.material3.MaterialTheme
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
// UI utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
// Navigation
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
// Project resources
import com.wavvy.app.core.designsystem.theme.WavvyMotion
import com.wavvy.app.core.designsystem.theme.backgroundGlow
import com.wavvy.app.core.playback.PlayerConnection
import com.wavvy.app.features.artist.ui.ArtistScreen
import com.wavvy.app.features.collection.ui.CollectionMenuHost
import com.wavvy.app.features.collection.ui.CollectionScreen
import com.wavvy.app.features.discography.ui.DiscographyScreen
import com.wavvy.app.features.discover.ui.DiscoverScreen
import com.wavvy.app.features.notifications.ui.NotificationsScreen
import com.wavvy.app.features.home.ui.HomeScreen
import com.wavvy.app.features.menu.ItemMenuHost
import com.wavvy.app.features.player.ui.LocalMiniPlayerInset
import com.wavvy.app.features.player.ui.MiniPlayerShade
import com.wavvy.app.features.player.ui.PlayerSheet
import com.wavvy.app.features.player.ui.components.PlayerDimens
import com.wavvy.app.features.profile.ui.LocalProfile
import com.wavvy.app.features.profile.ui.ProfileSheet
import com.wavvy.app.features.search.ui.SearchScreen

// Main app, the current tab with the navigation bar below it, or the rail on the side in landscape, the mini player above them and the profile menu over it
@Composable
fun MainScaffold(
    onSignOut: () -> Unit,
    onSignIn: () -> Unit
) {
    val navController = rememberNavController()
    var profileOpen by rememberSaveable { mutableStateOf(false) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    // A page opened from a tab keeps that tab selected
    val routeTab = MainTab.entries.firstOrNull { it.route == currentRoute }
    var lastTab by rememberSaveable { mutableStateOf(MainTab.HOME) }
    LaunchedEffect(routeTab) { routeTab?.let { lastTab = it } }
    val selected = routeTab ?: lastTab
    val context = LocalContext.current
    val density = LocalDensity.current

    // Connects to the player, so a song that kept playing in the background shows up again
    LaunchedEffect(Unit) { PlayerConnection.connect(context) }

    // The cards of every screen open their pages through this navigation
    DisposableEffect(navController) {
        ItemNavigator.attach(navController)
        onDispose { ItemNavigator.attach(null) }
    }
    val track by PlayerConnection.track.collectAsState()
    val hasPlayer = track != null

    // Real height of the bar, which grows with the system font size and the system bars
    var navBarHeight by remember { mutableStateOf(PlayerDimens.None) }

    val onSelect: (MainTab) -> Unit = { tab ->
        // A page that was opened from this tab goes away and brings the tab back, which restoring the saved state would put back
        val closedPages = routeTab == null && tab == selected && navController.popBackStack(tab.route, inclusive = false)

        if (!closedPages) {
            navController.navigate(tab.route) {
                // Keep one entry per tab and restore its state, except for the first tab, which stays in the stack and whose
                // saved state would put back the pages that were just left, since popping to it saves them under its own name
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = tab != MainTab.HOME
            }
        }
    }

    // The glow is only on the Home for now, behind the content, the bar and the rail are solid
    val glowStrength by animateFloatAsState(
        targetValue = if (selected == MainTab.HOME) 1f else 0f,
        animationSpec = tween(WavvyMotion.TabSwitchMillis),
        label = "HomeGlow"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .backgroundGlow { glowStrength }
    ) {
        val isLandscape = maxWidth > maxHeight
        val pillBottom: Dp = if (isLandscape) PlayerDimens.MiniBottomLandscape else navBarHeight + PlayerDimens.MiniGap
        val contentInset = if (!hasPlayer) {
            PlayerDimens.None
        } else if (isLandscape) {
            PlayerDimens.MiniHeight + PlayerDimens.MiniBottomLandscape
        } else {
            PlayerDimens.MiniHeight + PlayerDimens.MiniGap
        }

        CompositionLocalProvider(LocalMiniPlayerInset provides contentInset) {
            if (isLandscape) {
                Row(modifier = Modifier.fillMaxSize()) {
                    WavvyNavRail(selected = selected, onSelect = onSelect)

                    // The rail already makes room for the camera cutout on its side
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Start))
                    ) {
                        MainNavHost(
                            navController = navController,
                            onProfileClick = { profileOpen = true },
                            modifier = Modifier.fillMaxSize()
                        )
                        MiniPlayerShade(visible = hasPlayer, isLandscape = true, modifier = Modifier.align(Alignment.BottomCenter))
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        MainNavHost(
                            navController = navController,
                            onProfileClick = { profileOpen = true },
                            modifier = Modifier.fillMaxSize()
                        )
                        MiniPlayerShade(visible = hasPlayer, isLandscape = false, modifier = Modifier.align(Alignment.BottomCenter))
                    }
                    WavvyNavBar(
                        selected = selected,
                        onSelect = onSelect,
                        modifier = Modifier.onSizeChanged { navBarHeight = with(density) { it.height.toDp() } }
                    )
                }
            }
        }

        // Above the bar and the content, opening over the whole screen, below the profile menu
        PlayerSheet(bottomPadding = pillBottom, isLandscape = isLandscape)

        // Over the bar and the rail, so the whole screen dims behind it
        ProfileSheet(
            visible = profileOpen,
            profile = LocalProfile.current,
            onDismiss = { profileOpen = false },
            onSignOut = {
                profileOpen = false
                onSignOut()
            },
            onSignIn = {
                profileOpen = false
                onSignIn()
            }
        )

        // The menu of a song of the Home, over everything including the player
        ItemMenuHost()

        // The menu of the page of an album or of a playlist
        CollectionMenuHost()
    }
}

// The screen that comes back slides a little and fades, to the side it is on
private fun AnimatedContentTransitionScope<NavBackStackEntry>.popEnter(routeIndexMap: Map<String, Int>): EnterTransition {
    val currentRouteIndex = routeIndexMap[targetState.destination.route] ?: -1
    val previousRouteIndex = routeIndexMap[initialState.destination.route] ?: -1

    return if (previousRouteIndex != -1 && previousRouteIndex < currentRouteIndex) {
        slideInHorizontally { it / WavvyMotion.PageSlideDivisor } + fadeIn(tween(WavvyMotion.TabSwitchMillis))
    } else {
        slideInHorizontally { -it / WavvyMotion.PageSlideDivisor } + fadeIn(tween(WavvyMotion.TabSwitchMillis))
    }
}

// The screen that leaves slides a little and fades, to the side it came from
private fun AnimatedContentTransitionScope<NavBackStackEntry>.popExit(routeIndexMap: Map<String, Int>): ExitTransition {
    val currentRouteIndex = routeIndexMap[initialState.destination.route] ?: -1
    val targetRouteIndex = routeIndexMap[targetState.destination.route] ?: -1

    return if (currentRouteIndex != -1 && currentRouteIndex < targetRouteIndex) {
        slideOutHorizontally { -it / WavvyMotion.PageSlideDivisor } + fadeOut(tween(WavvyMotion.TabSwitchMillis))
    } else {
        slideOutHorizontally { it / WavvyMotion.PageSlideDivisor } + fadeOut(tween(WavvyMotion.TabSwitchMillis))
    }
}

// The four tabs, with a short fade when switching between them
@Composable
private fun MainNavHost(
    navController: NavHostController,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // The place of each tab in the bar, which tells to which side a screen moves
    val routeIndexMap = remember { MainTab.entries.mapIndexed { index, tab -> tab.route to index }.toMap() }

    NavHost(
        navController = navController,
        startDestination = MainTab.HOME.route,
        modifier = modifier,
        // Slides a little and fades, to the side the destination is on, as Metrolist does it, a page that is not a tab counts as the one after
        enterTransition = {
            val currentRouteIndex = routeIndexMap[targetState.destination.route] ?: -1
            val previousRouteIndex = routeIndexMap[initialState.destination.route] ?: -1

            if (currentRouteIndex == -1 || currentRouteIndex > previousRouteIndex) {
                slideInHorizontally { it / WavvyMotion.PageSlideDivisor } + fadeIn(tween(WavvyMotion.TabSwitchMillis))
            } else {
                slideInHorizontally { -it / WavvyMotion.PageSlideDivisor } + fadeIn(tween(WavvyMotion.TabSwitchMillis))
            }
        },
        exitTransition = {
            val currentRouteIndex = routeIndexMap[initialState.destination.route] ?: -1
            val targetRouteIndex = routeIndexMap[targetState.destination.route] ?: -1

            if (targetRouteIndex == -1 || targetRouteIndex > currentRouteIndex) {
                slideOutHorizontally { -it / WavvyMotion.PageSlideDivisor } + fadeOut(tween(WavvyMotion.TabSwitchMillis))
            } else {
                slideOutHorizontally { it / WavvyMotion.PageSlideDivisor } + fadeOut(tween(WavvyMotion.TabSwitchMillis))
            }
        },
        popEnterTransition = { popEnter(routeIndexMap) },
        popExitTransition = { popExit(routeIndexMap) },
        // The back gesture plays the same transitions as the back button, following the finger, instead of the shrinking of the library
        predictivePopEnterTransition = { popEnter(routeIndexMap) },
        predictivePopExitTransition = { popExit(routeIndexMap) }
    ) {
        composable(MainTab.HOME.route) { HomeScreen(onProfileClick = onProfileClick) }

        composable(MainTab.EXPLORE.route) { SearchScreen() }

        composable(MainTab.DISCOVER.route) { DiscoverScreen(onProfileClick = onProfileClick) }

        composable(
            route = CollectionRoute,
            arguments = listOf(
                navArgument(CollectionKindArg) { type = NavType.StringType },
                navArgument(CollectionIdArg) { type = NavType.StringType },
                navArgument(CollectionTitleArg) { type = NavType.StringType; defaultValue = "" }
            )
        ) { CollectionScreen(onBack = { navController.popBackStack() }) }

        composable(NotificationsRoute) { NotificationsScreen(onBack = { navController.popBackStack() }) }

        composable(
            route = ArtistRoute,
            arguments = listOf(navArgument(ArtistIdArg) { type = NavType.StringType })
        ) { ArtistScreen(onBack = { navController.popBackStack() }) }

        composable(
            route = DiscographyRoute,
            arguments = listOf(
                navArgument(DiscographyIdArg) { type = NavType.StringType },
                navArgument(DiscographyParamsArg) { type = NavType.StringType; defaultValue = "" },
                navArgument(DiscographyTitleArg) { type = NavType.StringType; defaultValue = "" },
                navArgument(DiscographyFilterArg) { type = NavType.StringType; defaultValue = "" }
            )
        ) { DiscographyScreen(onBack = { navController.popBackStack() }) }

        MainTab.entries.filter { it != MainTab.HOME && it != MainTab.EXPLORE && it != MainTab.DISCOVER }.forEach { tab ->
            composable(tab.route) { TabPlaceholder() }
        }
    }
}
