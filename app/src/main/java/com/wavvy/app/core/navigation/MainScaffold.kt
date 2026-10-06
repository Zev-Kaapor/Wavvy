package com.wavvy.app.core.navigation

// Compose animation and layouts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
// UI utilities
import androidx.compose.ui.Modifier
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
import com.wavvy.app.features.home.ui.HomeScreen
import com.wavvy.app.features.profile.ui.LocalProfile
import com.wavvy.app.features.profile.ui.ProfileSheet

// Main app, the current tab with the navigation bar below it, or the rail on the side in landscape, and the profile menu over it
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
        if (maxWidth > maxHeight) {
            Row(modifier = Modifier.fillMaxSize()) {
                WavvyNavRail(selected = selected, onSelect = onSelect)

                // The rail already makes room for the camera cutout on its side
                MainNavHost(
                    navController = navController,
                    onProfileClick = { profileOpen = true },
                    modifier = Modifier
                        .weight(1f)
                        .consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Start))
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                MainNavHost(
                    navController = navController,
                    onProfileClick = { profileOpen = true },
                    modifier = Modifier.weight(1f)
                )
                WavvyNavBar(selected = selected, onSelect = onSelect)
            }
        }

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
