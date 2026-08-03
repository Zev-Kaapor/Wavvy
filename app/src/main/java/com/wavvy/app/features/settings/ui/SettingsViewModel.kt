package com.wavvy.app.features.settings.ui

// Lifecycle and coroutines
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// Storage and preferences
import com.wavvy.app.core.data.local.SettingsStorage
import com.wavvy.app.core.designsystem.components.ToastData
import com.wavvy.app.core.designsystem.theme.ThemeMode
import com.wavvy.app.core.navigation.DefaultTab
import com.wavvy.app.features.auth.data.AuthRepository
import com.wavvy.app.features.home.data.QuickPicksRepository
import com.wavvy.app.features.home.data.RecentHistoryManager
import com.wavvy.app.features.home.models.KworbChartConfig
import com.wavvy.app.features.home.models.QuickPicksSource
import com.wavvy.app.features.search.data.SearchHistoryManager
// StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Settings screen state orchestrator
class SettingsViewModel(
    private val settingsStorage: SettingsStorage,
    private val recentHistoryManager: RecentHistoryManager,
    private val searchHistoryManager: SearchHistoryManager,
    private val quickPicksRepository: QuickPicksRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // Load persisted preferences into initial state
        _uiState.update {
            it.copy(
                currentTheme = settingsStorage.getThemeMode(),
                currentDefaultTab = settingsStorage.getDefaultTab(),
                queueLimit = settingsStorage.getQueueLimit(),
                kworbChartConfig = quickPicksRepository.getKworbConfig()
            )
        }
        loadQuickPicksSourceState()
    }

    // Resolve login state and available quick picks sources
    private fun loadQuickPicksSourceState() {
        viewModelScope.launch {
            val isLoggedIn = authRepository.fetchAuthenticatedAccountDetails() != null
            val available = quickPicksRepository.availableSources(isLoggedIn)
            _uiState.update {
                it.copy(
                    isLoggedIn = isLoggedIn,
                    availableQuickPicksSources = available
                )
            }
        }
    }

    // Navigation state handlers
    fun navigateToSection(section: SettingsSection) {
        _uiState.update {
            it.copy(
                currentSection = section,
                backStack = it.backStack + it.currentSection
            )
        }
    }

    fun navigateBack(): Boolean {
        val stack = _uiState.value.backStack
        return if (stack.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    currentSection = stack.last(),
                    backStack = stack.dropLast(1)
                )
            }
            true
        } else {
            false
        }
    }

    // Settings update operations
    fun updateQueueLimit(limit: Int) {
        _uiState.update { it.copy(queueLimit = limit) }
        viewModelScope.launch {
            settingsStorage.saveQueueLimit(limit)
        }
    }

    fun updateTheme(theme: ThemeMode) {
        _uiState.update { it.copy(currentTheme = theme) }
        viewModelScope.launch {
            settingsStorage.saveThemeMode(theme)
        }
    }

    fun updateDefaultTab(tab: DefaultTab) {
        _uiState.update { it.copy(currentDefaultTab = tab) }
        viewModelScope.launch {
            settingsStorage.saveDefaultTab(tab)
        }
    }

    // Quick picks source persistence
    fun updateQuickPicksSource(source: QuickPicksSource) {
        _uiState.update { it.copy(quickPicksSource = source) }
        quickPicksRepository.saveSource(source)
    }

    // Kworb chart config persistence
    fun updateKworbChartConfig(config: KworbChartConfig) {
        _uiState.update { it.copy(kworbChartConfig = config) }
        quickPicksRepository.saveKworbConfig(config)
    }

    // Maintenance operations
    fun clearPlaybackHistory(successTitle: String, successSubtitle: String) {
        viewModelScope.launch {
            recentHistoryManager.clearAll()
            showToast(ToastData(message = successTitle, subtitle = successSubtitle))
        }
    }

    fun clearSearchHistory(successTitle: String, successSubtitle: String) {
        viewModelScope.launch {
            searchHistoryManager.clearAll()
            showToast(ToastData(message = successTitle, subtitle = successSubtitle))
        }
    }

    fun showToast(toastData: ToastData) {
        _uiState.update { it.copy(toastMessage = toastData) }
    }

    fun consumeToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }
}
