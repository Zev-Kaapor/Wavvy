package com.wavvy.app.features.home.ui

// Android lifecycle and coroutines
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
// Storage and repositories
import com.wavvy.app.core.data.local.SettingsStorage
import com.wavvy.app.features.auth.data.AuthRepository
import com.wavvy.app.features.home.data.QuickPicksRepository
import com.wavvy.app.features.home.data.RecentHistoryManager
import com.wavvy.app.features.home.ui.components.RecentTrack

// Home view model
class HomeViewModel(
    private val settingsStorage: SettingsStorage,
    private val authRepository: AuthRepository,
    private val recentHistoryManager: RecentHistoryManager,
    private val quickPicksRepository: QuickPicksRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var accountJob: Job? = null
    private var quickPicksJob: Job? = null
    private var forgottenFavoritesJob: Job? = null
    private var allFilters: List<String> = emptyList()

    init {
        observeActiveAccount()
        observeRecentTracks()
        loadQuickPicks()
        loadForgottenFavorites()
    }

    // Observe active account
    fun observeActiveAccount() {
        accountJob?.cancel()
        accountJob = viewModelScope.launch {
            refreshActiveAccount()
        }
    }

    // Refresh active account
    private suspend fun refreshActiveAccount() {
        val account = authRepository.fetchAuthenticatedAccountDetails()
        if (account != null) {
            recentHistoryManager.setActiveOwner(account.handle ?: account.name)
            _uiState.update {
                it.copy(
                    isAuthenticated = true,
                    isGuestActive = false,
                    isGuestNameCustom = false,
                    initialName = account.name,
                    initialHandle = account.handle,
                    initialPictureUrl = account.pictureUrl
                )
            }
        } else if (settingsStorage.isGuestActive()) {
            val guestName = settingsStorage.getGuestName()
            recentHistoryManager.setActiveOwner(settingsStorage.getActiveGuestId())
            _uiState.update {
                it.copy(
                    isAuthenticated = false,
                    isGuestActive = true,
                    isGuestNameCustom = settingsStorage.isGuestNameCustom(),
                    initialName = guestName.ifBlank { null },
                    initialHandle = null,
                    initialPictureUrl = null
                )
            }
        } else {
            recentHistoryManager.setActiveOwner(null)
            _uiState.update {
                it.copy(
                    isAuthenticated = false,
                    isGuestActive = false,
                    isGuestNameCustom = false,
                    initialName = null,
                    initialHandle = null,
                    initialPictureUrl = null
                )
            }
        }
    }

    // Observe recent tracks
    private fun observeRecentTracks() {
        viewModelScope.launch {
            recentHistoryManager.recentTracks.collect { tracks ->
                _uiState.update { it.copy(recentTracks = tracks) }
            }
        }
    }

    // Load quick picks
    private fun loadQuickPicks() {
        quickPicksJob?.cancel()
        quickPicksJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingQuickPicks = true) }
            val picks = quickPicksRepository.fetchQuickPicks().shuffled()
            _uiState.update {
                it.copy(
                    quickPicks = picks,
                    isLoadingQuickPicks = false
                )
            }
        }
    }

    // Load forgotten favorites
    private fun loadForgottenFavorites() {
        forgottenFavoritesJob?.cancel()
        forgottenFavoritesJob = viewModelScope.launch {
            val forgotten = authRepository.fetchForgottenFavorites()
            _uiState.update { it.copy(forgottenFavorites = forgotten) }
        }
    }

    // Update greeting
    fun updateGreetingIfNeeded(greetings: Array<String>, questions: Array<String>) {
        if (_uiState.value.greeting != null) return
        if (greetings.isNotEmpty() && questions.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    greeting = greetings.random(),
                    question = questions.random()
                )
            }
        }
    }

    // Initialize filters
    fun initializeFiltersIfNeeded(filters: Array<String>, isLandscape: Boolean) {
        if (allFilters.isNotEmpty()) return
        if (filters.isNotEmpty()) {
            allFilters = filters.toList()
            shuffleFilters(isLandscape)
        }
    }

    // Shuffle filters
    private fun shuffleFilters(isLandscape: Boolean) {
        if (allFilters.isEmpty()) return
        val shuffled = allFilters.shuffled()
        val displayFilters = if (isLandscape) shuffled.take(10) else shuffled.take(5)
        _uiState.update { it.copy(availableFilters = displayFilters) }
    }

    // Refresh quick picks
    fun refreshQuickPicks() {
        _uiState.update { it.copy(selectedFilter = "") }
        loadQuickPicks()
        loadForgottenFavorites()
    }

    // Refresh after auth
    fun refreshAfterAuth() {
        accountJob?.cancel()
        accountJob = viewModelScope.launch {
            refreshActiveAccount()
            loadQuickPicks()
            loadForgottenFavorites()
        }
    }

    // Handle filter selection
    fun onFilterSelected(filter: String) {
        selectFilter(filter)
    }

    // Select filter
    fun selectFilter(filter: String) {
        val nextFilter = if (_uiState.value.selectedFilter == filter) "" else filter
        _uiState.update { it.copy(selectedFilter = nextFilter) }

        quickPicksJob?.cancel()
        quickPicksJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingQuickPicks = true) }
            val picks = if (nextFilter.isBlank()) {
                quickPicksRepository.fetchQuickPicks().shuffled()
            } else {
                authRepository.fetchSongsByGenre(nextFilter).shuffled()
            }
            _uiState.update {
                it.copy(
                    quickPicks = picks,
                    isLoadingQuickPicks = false
                )
            }
        }
    }

    // Logout
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            settingsStorage.saveGuestName("")
            settingsStorage.saveActiveGuestId(null)
            settingsStorage.setGuestActive(false)
            _uiState.update {
                it.copy(
                    isAuthenticated = false,
                    initialName = null,
                    initialHandle = null,
                    initialPictureUrl = null
                )
            }
            observeActiveAccount()
            loadQuickPicks()
        }
    }

    // Remove recent track
    fun removeRecentTrack(trackId: String) {
        viewModelScope.launch {
            recentHistoryManager.removeItem(trackId)
        }
    }

    // Save track to history
    fun saveTrackToHistory(track: RecentTrack) {
        if (!settingsStorage.getBoolean(SettingsStorage.KEY_PAUSE_PLAYBACK_HISTORY, false)) {
            viewModelScope.launch {
                recentHistoryManager.saveTrack(track)
            }
        }
    }
}
