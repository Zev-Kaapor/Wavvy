package com.lonewolf.wavvy.ui.settings

// Compose foundation and layout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
// Material 3 and icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
// State and UI tools
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.navigation.DefaultTab
import com.lonewolf.wavvy.ui.settings.sections.*
import com.lonewolf.wavvy.ui.theme.Poppins
import com.lonewolf.wavvy.ui.theme.ThemeMode

// Screen navigation enum state mapping
enum class SettingsSection {
    MAIN, GENERAL, APPEARANCE, PLAYER, CONTENT, PRIVACY, STORAGE, BACKUP, LINKS, ABOUT
}

// Orchestrator wrapper layout for settings screen hierarchy
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    queueLimit: Int,
    onQueueLimitChange: (Int) -> Unit,
    onClearPlaybackHistory: suspend () -> Unit,
    onClearSearchHistory: suspend () -> Unit,
    onNavigateBack: () -> Unit,
    scrollState: ScrollState,
    isPlayerActive: Boolean,
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    currentDefaultTab: DefaultTab,
    onDefaultTabChange: (DefaultTab) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentSection by remember { mutableStateOf(SettingsSection.MAIN) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val handleBackNavigation = {
        if (currentSection == SettingsSection.MAIN) {
            onNavigateBack()
        } else {
            currentSection = SettingsSection.MAIN
        }
    }

    BackHandler(onBack = handleBackNavigation)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentSection) {
                            SettingsSection.MAIN -> stringResource(R.string.menu_settings)
                            SettingsSection.GENERAL -> stringResource(R.string.setting_section_general)
                            SettingsSection.APPEARANCE -> stringResource(R.string.setting_section_appearance)
                            SettingsSection.PLAYER -> stringResource(R.string.setting_section_player)
                            SettingsSection.CONTENT -> stringResource(R.string.setting_section_content)
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
                    // Transparent so the Scaffold's own background shows through, avoiding Material3's internal spring animation on containerColor
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentSection,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                },
                label = "settings_navigation_animation"
            ) { section ->
                when (section) {
                    SettingsSection.MAIN -> {
                        MainSettingsList(
                            scrollState = scrollState,
                            isPlayerActive = isPlayerActive,
                            onNavigateToSection = { currentSection = it }
                        )
                    }
                    SettingsSection.GENERAL -> {
                        GeneralSubScreen(
                            isPlayerActive = isPlayerActive
                        )
                    }
                    SettingsSection.APPEARANCE -> {
                        AppearanceSubScreen(
                            isPlayerActive = isPlayerActive,
                            currentTheme = currentTheme,
                            onThemeChange = onThemeChange,
                            currentDefaultTab = currentDefaultTab,
                            onDefaultTabChange = onDefaultTabChange
                        )
                    }
                    SettingsSection.PLAYER -> {
                        PlayerSubScreen(
                            queueLimit = queueLimit,
                            onQueueLimitChange = onQueueLimitChange,
                            isPlayerActive = isPlayerActive
                        )
                    }
                    SettingsSection.CONTENT -> {
                        ContentSubScreen(
                            isPlayerActive = isPlayerActive
                        )
                    }
                    SettingsSection.PRIVACY -> {
                        PrivacySubScreen(
                            onClearPlaybackHistory = onClearPlaybackHistory,
                            onClearSearchHistory = onClearSearchHistory,
                            isPlayerActive = isPlayerActive
                        )
                    }
                    SettingsSection.STORAGE -> {
                        StorageSubScreen(
                            isPlayerActive = isPlayerActive
                        )
                    }
                    SettingsSection.BACKUP -> {
                        BackupSubScreen(
                            isPlayerActive = isPlayerActive
                        )
                    }
                    SettingsSection.LINKS -> {
                        LinksSubScreen(
                            isPlayerActive = isPlayerActive
                        )
                    }
                    SettingsSection.ABOUT -> {
                        AboutSubScreen(
                            appVersion = "1.0.0",
                            isPlayerActive = isPlayerActive
                        )
                    }
                }
            }
        }
    }
}
