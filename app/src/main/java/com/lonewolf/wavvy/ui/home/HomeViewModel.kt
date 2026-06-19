package com.lonewolf.wavvy.ui.home

// Lifecycle and state management
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// Infrastructure and data
import com.lonewolf.wavvy.data.AuthRepository
import com.lonewolf.wavvy.ui.auth.AuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Logic and state management for Home screen
class HomeViewModel(
    private val authManager: AuthManager,
    private val authRepository: AuthRepository
) : ViewModel() {
    // UI state holder
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun updateGreetingIfNeeded(greetings: Array<String>, questions: Array<String>) {
        val currentTime = System.currentTimeMillis()
        val fifteenMinutesInMillis = 15 * 60 * 1000

        val currentState = _uiState.value

        if (currentState.greeting == null ||
            currentTime - currentState.lastGreetingTimestamp > fifteenMinutesInMillis) {

            _uiState.value = currentState.copy(
                greeting = greetings.random(),
                question = questions.random(),
                lastGreetingTimestamp = currentTime
            )
        }
    }

    // Persist filter selection in ViewModel to survive navigation
    fun onFilterSelected(filter: String) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
    }

    // Initialize or update filters based on orientation limit
    fun initializeFiltersIfNeeded(allMoods: Array<String>, isLandscape: Boolean) {
        val limit = if (isLandscape) 10 else 5
        val currentState = _uiState.value

        val masterList = currentState.masterMoodList.ifEmpty { allMoods.toList().shuffled() }
        val newFilters = masterList.take(limit)

        if (currentState.availableFilters != newFilters || currentState.masterMoodList != masterList) {
            _uiState.value = currentState.copy(
                availableFilters = newFilters,
                masterMoodList = masterList
            )
        }
    }

    // Launch Google identity provider to acquire authentication token configuration
    fun loginWithGoogle() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        try {
            val generatedUrl = authManager.buildAuthUrl()
            _uiState.value = _uiState.value.copy(
                authUrl = generatedUrl,
                isLoading = false
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Failed to generate auth configuration",
                isLoading = false
            )
        }
    }

    // Capture external authentication result to update application session
    fun onTokenReceived(cookies: String, onUserCaptured: (String?, String?, String?) -> Unit = { _, _, _ -> }) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                authRepository.signInWithGoogle(cookies)

                _uiState.value = _uiState.value.copy(
                    isAuthenticated = true,
                    authUrl = null,
                    isLoading = false
                )

                val accountInfo = authRepository.fetchAuthenticatedAccountDetails()
                val name = accountInfo?.name
                val handle = accountInfo?.handle
                val pictureUrl = accountInfo?.pictureUrl

                onUserCaptured(name, handle, pictureUrl)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Session registration failed",
                    isLoading = false
                )
            }
        }
    }

    // Reset authentication interaction flow state
    fun cancelWebLogin() {
        _uiState.value = _uiState.value.copy(authUrl = null)
    }

    // Clear session states across framework layers
    fun logout() {
        viewModelScope.launch {
            authManager.clearSession()
            authRepository.logout()
            _uiState.value = _uiState.value.copy(isAuthenticated = false)
        }
    }
}

// Data wrapper for Home screen state
data class HomeUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val greeting: String? = null,
    val question: String? = null,
    val lastGreetingTimestamp: Long = 0L,
    val selectedFilter: String = "",
    val availableFilters: List<String> = emptyList(),
    val masterMoodList: List<String> = emptyList(),
    val isAuthenticated: Boolean = false,
    val authUrl: String? = null
)
