package com.wavvy.app.features.settings.ui

// Core design system and theme models
import com.wavvy.app.core.designsystem.components.ToastData
import com.wavvy.app.core.designsystem.theme.ThemeMode
import com.wavvy.app.core.navigation.DefaultTab
// Quick picks models
import com.wavvy.app.features.home.models.KworbChartConfig
import com.wavvy.app.features.home.models.QuickPicksSource

// Navigation sections
enum class SettingsSection {
    MAIN, GENERAL, APPEARANCE, PLAYER, CONTENT, QUICK_PICKS, PRIVACY, STORAGE, BACKUP, LINKS, ABOUT
}

// UI state holder for settings feature
data class SettingsUiState(
    val currentSection: SettingsSection = SettingsSection.MAIN,
    val backStack: List<SettingsSection> = emptyList(),
    val queueLimit: Int = 20,
    val currentTheme: ThemeMode = ThemeMode.SYSTEM,
    val currentDefaultTab: DefaultTab = DefaultTab.HOME,
    val isPlayerActive: Boolean = false,
    val toastMessage: ToastData? = null,
    val isLoggedIn: Boolean = false,
    val availableQuickPicksSources: List<QuickPicksSource> = emptyList(),
    val quickPicksSource: QuickPicksSource = QuickPicksSource.KWORB_CHART,
    val kworbChartConfig: KworbChartConfig = KworbChartConfig()
)
