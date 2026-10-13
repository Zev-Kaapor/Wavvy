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
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
// UI utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.core.backup.BackupInbox
import com.wavvy.app.core.designsystem.components.ConfirmDialog
import com.wavvy.app.core.designsystem.components.Toasts
import com.wavvy.app.core.drive.DriveStore
import com.wavvy.app.core.designsystem.theme.WavvyMotion
import com.wavvy.app.core.designsystem.theme.backgroundGlow
import com.wavvy.app.core.playback.PlayerConnection
import com.wavvy.app.features.listen.ui.ListenGlobalDialogs
import com.wavvy.app.features.artist.ui.ArtistScreen
import com.wavvy.app.features.collection.ui.CollectionMenuHost
import com.wavvy.app.features.collection.ui.CollectionScreen
import com.wavvy.app.features.discography.ui.DiscographyScreen
import com.wavvy.app.features.discover.ui.DiscoverScreen
import com.wavvy.app.features.discover.ui.ExploreScreen
import com.wavvy.app.R
import com.wavvy.app.features.history.ui.HistoryScreen
import com.wavvy.app.features.library.ui.LibraryScreen
import com.wavvy.app.core.designsystem.components.ToastHost
import com.wavvy.app.features.playlist.ui.AddSongHost
import com.wavvy.app.features.playlist.ui.EditPlaylistHost
import com.wavvy.app.features.playlist.ui.NewPlaylistHost
import com.wavvy.app.features.playlist.ui.SaveToPlaylistHost
import com.wavvy.app.features.notifications.ui.NotificationsScreen
import com.wavvy.app.features.podcast.ui.EpisodeScreen
import com.wavvy.app.features.podcast.ui.PodcastScreen
import com.wavvy.app.features.home.ui.HomeScreen
import com.wavvy.app.core.designsystem.components.OverlaySheetHost
import com.wavvy.app.features.menu.ItemMenuHost
import com.wavvy.app.features.player.ui.LocalMiniPlayerInset
import com.wavvy.app.features.player.ui.MiniPlayerShade
import com.wavvy.app.features.player.ui.PlayerSheet
import com.wavvy.app.features.player.ui.components.PlayerDimens
import com.wavvy.app.features.profile.ui.LocalProfile
import com.wavvy.app.features.profile.ui.ProfileList
import com.wavvy.app.features.profile.ui.ProfileListScreen
import com.wavvy.app.features.profile.ui.ProfilePeriod
import com.wavvy.app.features.profile.ui.ProfileScreen
import com.wavvy.app.features.profile.ui.ProfileSheet
import com.wavvy.app.features.search.ui.SearchScreen
import com.wavvy.app.features.settings.ui.AboutSettingsScreen
import com.wavvy.app.features.settings.ui.AppearanceSettingsScreen
import com.wavvy.app.features.settings.ui.ContentSettingsScreen
import com.wavvy.app.features.settings.ui.NotificationsSettingsScreen
import com.wavvy.app.features.settings.ui.DownloadsSettingsScreen
import com.wavvy.app.features.settings.ui.BackupSettingsScreen
import com.wavvy.app.features.settings.ui.PlayerSettingsScreen
import com.wavvy.app.features.settings.ui.PrivacySettingsScreen
import com.wavvy.app.features.settings.ui.SettingsScreen
import com.wavvy.app.features.settings.ui.SettingsSection
import com.wavvy.app.features.settings.ui.StorageSettingsScreen
import com.wavvy.app.features.settings.ui.ThanksScreen
import com.wavvy.app.features.settings.ui.ThanksSection
import com.wavvy.app.features.settings.ui.BackupDriveSection
import com.wavvy.app.features.settings.ui.BackupManualSection
import com.wavvy.app.features.settings.ui.DriveSettingsScreen
import com.wavvy.app.features.settings.ui.ManualBackupScreen
import com.wavvy.app.features.settings.ui.UpdatesSection
import com.wavvy.app.features.settings.ui.UpdatesSettingsScreen
import com.wavvy.app.features.updates.data.UpdateState
import com.wavvy.app.features.updates.data.Updates
import com.wavvy.app.core.designsystem.components.ConfirmDialog

// Main app, the current tab with the navigation bar below it, or the rail on the side in landscape, the mini player above them and the profile menu over it
@Composable
fun MainScaffold(
    onSignOut: () -> Unit,
    onSignIn: () -> Unit,
    startTab: MainTab = MainTab.HOME
) {
    val navController = rememberNavController()
    var profileOpen by rememberSaveable { mutableStateOf(false) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    // A page opened from a tab keeps that tab selected
    val routeTab = MainTab.entries.firstOrNull { it.route == currentRoute }
    var lastTab by rememberSaveable { mutableStateOf(startTab) }
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

    // A link of YouTube or YouTube Music that opened the app or came while it was open
    val link by LinkOpener.pending.collectAsState()
    LaunchedEffect(link) { link?.let { LinkOpener.open(context, it) } }

    // A backup file that was opened from another app takes the user to the screen of the backup, which asks its password, unless the Drive takes care of the backup
    val backupFile by BackupInbox.pending.collectAsState()
    val blockedByDrive = stringResource(R.string.backup_blocked_by_drive)
    LaunchedEffect(backupFile) {
        if (backupFile == null) return@LaunchedEffect
        if (DriveStore(context).isConnected.first()) {
            BackupInbox.clear()
            Toasts.show(blockedByDrive, isError = true)
        } else {
            ItemNavigator.openSettingsSection(BackupManualSection, withSettingsBelow = true)
        }
    }

    // Once the user has signed in, the app offers to connect the Drive so the data stays safe, only once and never for a guest
    val driveStore = remember { DriveStore(context) }
    val isDriveConnected by driveStore.isConnected.collectAsState(initial = true)
    val wasDriveOffered by driveStore.wasOffered.collectAsState(initial = true)
    val isSignedIn = LocalProfile.current.isSignedIn
    val driveScope = rememberCoroutineScope()
    if (isSignedIn && !isDriveConnected && !wasDriveOffered) {
        ConfirmDialog(
            title = stringResource(R.string.backup_drive_ask_title),
            text = stringResource(R.string.backup_drive_ask_text),
            confirmLabel = stringResource(R.string.backup_drive_ask_connect),
            cancelLabel = stringResource(R.string.backup_drive_offer_later),
            onConfirm = {
                driveScope.launch { driveStore.setOffered(true) }
                ItemNavigator.openSettingsSection(BackupDriveSection, withSettingsBelow = true)
            },
            onDismiss = { driveScope.launch { driveStore.setOffered(true) } }
        )
    }

    // What the guests ask the host and the notice for a guest come up on any screen
    ListenGlobalDialogs()
    val track by PlayerConnection.track.collectAsState()
    val hasPlayer = track != null

    // Real height of the bar, which grows with the system font size and the system bars
    var navBarHeight by remember { mutableStateOf(PlayerDimens.None) }

    val onSelect: (MainTab) -> Unit = { tab ->
        // A page that was opened from this tab goes away and brings the tab back, which restoring the saved state would put back
        val closedPages = routeTab == null && tab == selected && navController.popBackStack(tab.route, inclusive = false)

        // The bar shows the tab that was tapped
        val leaving = selected
        lastTab = tab

        if (!closedPages) {
            // Leaving a tab from one of its pages closes the pages first, so coming back to the tab shows its first screen
            if (routeTab == null && tab != leaving) navController.popBackStack(leaving.route, inclusive = false)

            navController.navigate(tab.route) {
                // Keep one entry per tab and restore its state, except for the first tab, which stays in the stack and whose
                // saved state would put back the pages that were just left, since popping to it saves them under its own name
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = tab != startTab
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
                            startTab = startTab,
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
                            startTab = startTab,
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
            },
            onProfile = {
                profileOpen = false
                ItemNavigator.openProfile()
            },
            onHistory = {
                profileOpen = false
                ItemNavigator.openHistory()
            },
            onSettings = {
                profileOpen = false
                ItemNavigator.openSettings()
            }
        )

        // The menu of a song of the Home, over everything including the player
        ItemMenuHost()

        // The sheets that the pages ask for, the countries of the charts and the menu of a podcast among them
        OverlaySheetHost()

        // The menu of the page of an album or of a playlist
        CollectionMenuHost()

        // The question when the app opens and there is a newer version, it goes to the screen of the updates to show how the download goes
        val updatePrompt by Updates.prompt.collectAsState()
        updatePrompt?.let { info ->
            ConfirmDialog(
                title = stringResource(R.string.updates_prompt_title),
                text = stringResource(R.string.updates_prompt_text, info.version),
                confirmLabel = stringResource(R.string.updates_prompt_confirm),
                cancelLabel = stringResource(R.string.updates_prompt_later),
                onConfirm = {
                    Updates.clearPrompt()
                    if (Updates.state.value is UpdateState.Available) Updates.download(context)
                    ItemNavigator.openSettingsSection(UpdatesSection)
                },
                onDismiss = { Updates.postpone(context) }
            )
        }

        // The notice of what just happened, over the bar and the mini player
        ToastHost(
            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + PlayerDimens.ToastGap,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // The playlists to save songs to and the sheet of a new one, which any menu opens
        SaveToPlaylistHost()
        NewPlaylistHost()
        EditPlaylistHost()
        AddSongHost()
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
    startTab: MainTab,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // The place of each tab in the bar, which tells to which side a screen moves
    val routeIndexMap = remember { MainTab.entries.mapIndexed { index, tab -> tab.route to index }.toMap() }

    NavHost(
        navController = navController,
        startDestination = startTab.route,
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

        composable(MainTab.LIBRARY.route) {
            val podcastsTitle = stringResource(R.string.library_podcasts)
            LibraryScreen(
                onProfileClick = onProfileClick,
                onFindMusic = { ItemNavigator.openTab(MainTab.EXPLORE.route) },
                onHistoryClick = { ItemNavigator.openHistory() },
                onFindPodcasts = { ItemNavigator.openExplore(PodcastsBrowseId, null, podcastsTitle, large = true) }
            )
        }

        composable(
            route = CollectionRoute,
            arguments = listOf(
                navArgument(CollectionKindArg) { type = NavType.StringType },
                navArgument(CollectionIdArg) { type = NavType.StringType },
                navArgument(CollectionTitleArg) { type = NavType.StringType; defaultValue = "" }
            )
        ) { CollectionScreen(onBack = { navController.popBackStack() }) }

        composable(
            route = ExploreRoute,
            arguments = listOf(
                navArgument(ExploreIdArg) { type = NavType.StringType },
                navArgument(ExploreParamsArg) { type = NavType.StringType; defaultValue = "" },
                navArgument(ExploreTitleArg) { type = NavType.StringType; defaultValue = "" },
                navArgument(ExploreLargeArg) { type = NavType.BoolType; defaultValue = false }
            )
        ) { ExploreScreen(onBack = { navController.popBackStack() }) }

        composable(
            route = PodcastRoute,
            arguments = listOf(navArgument(PodcastIdArg) { type = NavType.StringType })
        ) { PodcastScreen(onBack = { navController.popBackStack() }) }

        composable(
            route = EpisodeRoute,
            arguments = listOf(navArgument(EpisodeIdArg) { type = NavType.StringType })
        ) { EpisodeScreen(onBack = { navController.popBackStack() }) }

        composable(NotificationsRoute) { NotificationsScreen(onBack = { navController.popBackStack() }) }

        composable(HistoryRoute) { HistoryScreen(onBack = { navController.popBackStack() }) }

        composable(ProfileRoute) { ProfileScreen(onBack = { navController.popBackStack() }) }

        composable(
            route = ProfileListRoute,
            arguments = listOf(
                navArgument(ProfileListKindArg) { type = NavType.StringType },
                navArgument(ProfileListPeriodArg) { type = NavType.StringType; defaultValue = "" }
            )
        ) { entry ->
            val list = ProfileList.entries.firstOrNull { it.name == entry.arguments?.getString(ProfileListKindArg) } ?: ProfileList.Songs
            val period = ProfilePeriod.entries.firstOrNull { it.name == entry.arguments?.getString(ProfileListPeriodArg) }
            ProfileListScreen(list = list, period = period, onBack = { navController.popBackStack() })
        }

        composable(SettingsRoute) {
            val context = LocalContext.current
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onSectionClick = { section ->
                    if (section == SettingsSection.Links) LinkOpener.openSystemSettings(context)
                    else if (section.isReady) ItemNavigator.openSettingsSection(section.name)
                }
            )
        }

        composable(
            route = SettingsSectionRoute,
            arguments = listOf(navArgument(SettingsSectionArg) { type = NavType.StringType })
        ) { entry ->
            when (entry.arguments?.getString(SettingsSectionArg)) {
                SettingsSection.Backup.name -> BackupSettingsScreen(onBack = { navController.popBackStack() })
                BackupDriveSection -> DriveSettingsScreen(onBack = { navController.popBackStack() })
                BackupManualSection -> ManualBackupScreen(onBack = { navController.popBackStack() })
                SettingsSection.Player.name -> PlayerSettingsScreen(onBack = { navController.popBackStack() })
                SettingsSection.Downloads.name -> DownloadsSettingsScreen(onBack = { navController.popBackStack() })
                SettingsSection.Content.name -> ContentSettingsScreen(onBack = { navController.popBackStack() })
                SettingsSection.Storage.name -> StorageSettingsScreen(onBack = { navController.popBackStack() })
                SettingsSection.Privacy.name -> PrivacySettingsScreen(onBack = { navController.popBackStack() })
                SettingsSection.Appearance.name -> AppearanceSettingsScreen(onBack = { navController.popBackStack() })
                SettingsSection.Notifications.name -> NotificationsSettingsScreen(onBack = { navController.popBackStack() })
                SettingsSection.About.name -> AboutSettingsScreen(onBack = { navController.popBackStack() })
                ThanksSection -> ThanksScreen(onBack = { navController.popBackStack() })
                UpdatesSection -> UpdatesSettingsScreen(onBack = { navController.popBackStack() })
            }
        }

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

        MainTab.entries.filter { it != MainTab.HOME && it != MainTab.EXPLORE && it != MainTab.DISCOVER && it != MainTab.LIBRARY }.forEach { tab ->
            composable(tab.route) { TabPlaceholder() }
        }
    }
}

// The page of the podcasts of the Explore tab, where the library sends to find one
private const val PodcastsBrowseId = "FEmusic_non_music_audio"
