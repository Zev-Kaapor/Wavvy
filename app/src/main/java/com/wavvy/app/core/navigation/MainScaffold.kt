package com.wavvy.app.core.navigation

// Compose animation and layouts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
// Project resources
import com.wavvy.app.core.designsystem.theme.WavvyMotion
import com.wavvy.app.core.designsystem.theme.backgroundGlow
import com.wavvy.app.core.playback.PlayerConnection
import com.wavvy.app.features.home.ui.HomeScreen
import com.wavvy.app.features.menu.ItemMenuHost
import com.wavvy.app.features.player.ui.LocalMiniPlayerInset
import com.wavvy.app.features.player.ui.MiniPlayerShade
import com.wavvy.app.features.player.ui.PlayerSheet
import com.wavvy.app.features.player.ui.components.PlayerDimens
import com.wavvy.app.features.profile.ui.LocalProfile
import com.wavvy.app.features.profile.ui.ProfileSheet

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
    val selected = MainTab.entries.firstOrNull { it.route == currentRoute } ?: MainTab.HOME
    val context = LocalContext.current
    val density = LocalDensity.current

    // Connects to the player, so a song that kept playing in the background shows up again
    LaunchedEffect(Unit) { PlayerConnection.connect(context) }
    val track by PlayerConnection.track.collectAsState()
    val hasPlayer = track != null

    // Real height of the bar, which grows with the system font size and the system bars
    var navBarHeight by remember { mutableStateOf(PlayerDimens.None) }

    val onSelect: (MainTab) -> Unit = { tab ->
        navController.navigate(tab.route) {
            // Keep one entry per tab and restore its state
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
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
    }
}

// The four tabs, with a short fade when switching between them
@Composable
private fun MainNavHost(
    navController: NavHostController,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = MainTab.HOME.route,
        modifier = modifier,
        enterTransition = { fadeIn(tween(WavvyMotion.TabSwitchMillis)) },
        exitTransition = { fadeOut(tween(WavvyMotion.TabSwitchMillis)) },
        popEnterTransition = { fadeIn(tween(WavvyMotion.TabSwitchMillis)) },
        popExitTransition = { fadeOut(tween(WavvyMotion.TabSwitchMillis)) }
    ) {
        composable(MainTab.HOME.route) { HomeScreen(onProfileClick = onProfileClick) }

        MainTab.entries.filter { it != MainTab.HOME }.forEach { tab ->
            composable(tab.route) { TabPlaceholder() }
        }
    }
}
