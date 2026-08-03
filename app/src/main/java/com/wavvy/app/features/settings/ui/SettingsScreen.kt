package com.wavvy.app.features.settings.ui

// Compose foundation and layout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
// Material 3 and icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
// State and UI tools
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
// Dependency injection via KOIN
import org.koin.androidx.compose.koinViewModel
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.ToastData
import com.wavvy.app.core.designsystem.theme.Poppins
import com.wavvy.app.features.settings.ui.sections.AboutSubScreen
import com.wavvy.app.features.settings.ui.sections.AppearanceSubScreen
import com.wavvy.app.features.settings.ui.sections.BackupSubScreen
import com.wavvy.app.features.settings.ui.sections.content.ContentSubScreen
import com.wavvy.app.features.settings.ui.sections.GeneralSubScreen
import com.wavvy.app.features.settings.ui.sections.LinksSubScreen
import com.wavvy.app.features.settings.ui.sections.PlayerSubScreen
import com.wavvy.app.features.settings.ui.sections.PrivacySubScreen
import com.wavvy.app.features.settings.ui.sections.StorageSubScreen
import com.wavvy.app.features.settings.ui.sections.content.components.QuickPicksSheetOverlays
import com.wavvy.app.features.settings.ui.sections.content.components.QuickPicksSubScreen

// Orchestrator wrapper layout for settings screen hierarchy
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onShowToast: (ToastData) -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    viewModel: SettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Maintenance action messages
    val playbackSuccessTitle = stringResource(R.string.setting_clear_playback_history_success)
    val playbackSuccessSubtitle = stringResource(R.string.setting_clear_playback_history_success_desc)
    val searchSuccessTitle = stringResource(R.string.setting_clear_search_history_success)
    val searchSuccessSubtitle = stringResource(R.string.setting_clear_search_history_success_desc)

    // Toast delegation consumer
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { toast ->
            onShowToast(toast)
            viewModel.consumeToast()
        }
    }

    val handleBackNavigation = {
        if (!viewModel.navigateBack()) {
            onNavigateBack()
        }
    }

    BackHandler(onBack = handleBackNavigation)

    // Quick picks sheet visibility — lifted here so overlays render outside Scaffold's padding
    var showSourceSheet by remember { mutableStateOf(false) }
    var showScopeSheet by remember { mutableStateOf(false) }
    var showCountrySheet by remember { mutableStateOf(false) }
    var showPeriodSheet by remember { mutableStateOf(false) }

    // Auto-dismiss quick picks sheets when navigating away from that section
    LaunchedEffect(uiState.currentSection) {
        if (uiState.currentSection != SettingsSection.QUICK_PICKS) {
            showSourceSheet = false
            showScopeSheet = false
            showCountrySheet = false
            showPeriodSheet = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when (uiState.currentSection) {
                                SettingsSection.MAIN -> stringResource(R.string.menu_settings)
                                SettingsSection.GENERAL -> stringResource(R.string.setting_section_general)
                                SettingsSection.APPEARANCE -> stringResource(R.string.setting_section_appearance)
                                SettingsSection.PLAYER -> stringResource(R.string.setting_section_player)
                                SettingsSection.CONTENT -> stringResource(R.string.setting_section_content)
                                SettingsSection.QUICK_PICKS -> stringResource(R.string.setting_section_quick_picks)
                                SettingsSection.PRIVACY -> stringResource(R.string.setting_section_privacy)
                                SettingsSection.STORAGE -> stringResource(R.string.setting_section_storage)
                                SettingsSection.BACKUP -> stringResource(R.string.setting_section_backup)
                                SettingsSection.LINKS -> stringResource(R.string.setting_section_links)
                                SettingsSection.ABOUT -> stringResource(R.string.setting_section_about)
                            },
                            fontFamily = Poppins,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = handleBackNavigation) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.close_button)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = uiState.currentSection,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                    },
                    label = "settings_navigation_animation"
                ) { section ->
                    when (section) {
                        SettingsSection.MAIN -> {
                            MainSettingsList(
                                scrollState = scrollState,
                                isPlayerActive = uiState.isPlayerActive,
                                onNavigateToSection = { viewModel.navigateToSection(it) }
                            )
                        }
                        SettingsSection.GENERAL -> {
                            GeneralSubScreen(
                                isPlayerActive = uiState.isPlayerActive
                            )
                        }
                        SettingsSection.APPEARANCE -> {
                            AppearanceSubScreen(
                                isPlayerActive = uiState.isPlayerActive,
                                currentTheme = uiState.currentTheme,
                                onThemeChange = { viewModel.updateTheme(it) },
                                currentDefaultTab = uiState.currentDefaultTab,
                                onDefaultTabChange = { viewModel.updateDefaultTab(it) }
                            )
                        }
                        SettingsSection.PLAYER -> {
                            PlayerSubScreen(
                                queueLimit = uiState.queueLimit,
                                onQueueLimitChange = { viewModel.updateQueueLimit(it) },
                                isPlayerActive = uiState.isPlayerActive
                            )
                        }
                        SettingsSection.CONTENT -> {
                            ContentSubScreen(
                                onNavigateToSection = { viewModel.navigateToSection(it) },
                                isPlayerActive = uiState.isPlayerActive
                            )
                        }
                        SettingsSection.QUICK_PICKS -> {
                            QuickPicksSubScreen(
                                isUserLoggedIn = uiState.isLoggedIn,
                                currentSource = uiState.quickPicksSource,
                                kworbConfig = uiState.kworbChartConfig,
                                onShowSourceSheet = { showSourceSheet = true },
                                onShowScopeSheet = { showScopeSheet = true },
                                onShowCountrySheet = { showCountrySheet = true },
                                onShowPeriodSheet = { showPeriodSheet = true }
                            )
                        }
                        SettingsSection.PRIVACY -> {
                            PrivacySubScreen(
                                onClearPlaybackHistory = {
                                    viewModel.clearPlaybackHistory(
                                        playbackSuccessTitle,
                                        playbackSuccessSubtitle
                                    )
                                },
                                onClearSearchHistory = {
                                    viewModel.clearSearchHistory(
                                        searchSuccessTitle,
                                        searchSuccessSubtitle
                                    )
                                },
                                isPlayerActive = uiState.isPlayerActive
                            )
                        }
                        SettingsSection.STORAGE -> {
                            StorageSubScreen(
                                isPlayerActive = uiState.isPlayerActive
                            )
                        }
                        SettingsSection.BACKUP -> {
                            BackupSubScreen(
                                onShowToast = { viewModel.showToast(it) },
                                isPlayerActive = uiState.isPlayerActive
                            )
                        }
                        SettingsSection.LINKS -> {
                            LinksSubScreen(
                                isPlayerActive = uiState.isPlayerActive
                            )
                        }
                        SettingsSection.ABOUT -> {
                            AboutSubScreen(
                                appVersion = "1.0.0",
                                isPlayerActive = uiState.isPlayerActive
                            )
                        }
                    }
                }
            }
        }

        // Fullscreen sheets
        if (uiState.currentSection == SettingsSection.QUICK_PICKS) {
            Box(modifier = Modifier.fillMaxSize().zIndex(10f)) {
                QuickPicksSheetOverlays(
                    isUserLoggedIn = uiState.isLoggedIn,
                    currentSource = uiState.quickPicksSource,
                    kworbConfig = uiState.kworbChartConfig,
                    onSourceSelected = { viewModel.updateQuickPicksSource(it) },
                    onKworbConfigChanged = { viewModel.updateKworbChartConfig(it) },
                    showSourceSheet = showSourceSheet,
                    onDismissSource = { showSourceSheet = false },
                    showScopeSheet = showScopeSheet,
                    onDismissScope = { showScopeSheet = false },
                    showCountrySheet = showCountrySheet,
                    onDismissCountry = { showCountrySheet = false },
                    showPeriodSheet = showPeriodSheet,
                    onDismissPeriod = { showPeriodSheet = false }
                )
            }
        }
    }
}
