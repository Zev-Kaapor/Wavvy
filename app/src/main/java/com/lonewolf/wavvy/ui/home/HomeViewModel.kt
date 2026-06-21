package com.lonewolf.wavvy.ui.home

// Lifecycle and state management
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// Infrastructure and data
import android.webkit.CookieManager
import com.lonewolf.wavvy.data.AuthRepository
import com.lonewolf.wavvy.data.AuthRepositoryImpl
import com.lonewolf.wavvy.data.SavedAccount
import com.lonewolf.wavvy.data.models.QuickPick
import com.lonewolf.wavvy.ui.auth.AuthManager
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

// Logic and state management for Home screen
class HomeViewModel(
    private val authManager: AuthManager,
    private val authRepository: AuthRepository
) : ViewModel() {

    // UI state holder
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Automatic session restoration check on initialization
    init {
        checkExistingSession()
        fetchQuickPicks()
    }

    // Verify existing authentication tokens to rebuild session context
    private fun checkExistingSession() {
        viewModelScope.launch {
            try {
                val accountInfo = authRepository.fetchAuthenticatedAccountDetails()
                if (accountInfo != null) {
                    _uiState.value = _uiState.value.copy(
                        isAuthenticated = true,
                        isLoading = false,
                        initialName = accountInfo.name,
                        initialHandle = accountInfo.handle,
                        initialPictureUrl = accountInfo.pictureUrl
                    )
                }
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isAuthenticated = false)
            }
        }
    }

    // Fetch personal context endpoint targets mapping results locally
    private fun fetchQuickPicks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingQuickPicks = true)
            try {
                val picks = authRepository.fetchQuickPicks()
                _uiState.value = _uiState.value.copy(
                    quickPicks = picks,
                    isLoadingQuickPicks = false
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoadingQuickPicks = false)
            }
        }
    }

    fun updateGreetingIfNeeded(greetings: Array<String>, questions: Array<String>) {
        val currentTime = System.currentTimeMillis()
        val fifteenMinutes = 15.toDuration(DurationUnit.MINUTES)
        val currentState = _uiState.value

        if (currentState.greeting == null ||
            (currentTime - currentState.lastGreetingTimestamp).toDuration(DurationUnit.MILLISECONDS) > fifteenMinutes) {
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

    // Clear WebView cookies and open manual login form
    fun loginWithGoogle() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        CookieManager.getInstance().removeAllCookies {
            CookieManager.getInstance().flush()
            try {
                val generatedUrl = authManager.buildAuthUrl(action = "login")
                _uiState.value = _uiState.value.copy(
                    authUrl = generatedUrl,
                    isLoading = false
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to generate auth configuration",
                    isLoading = false
                )
            }
        }
    }

    // Load saved accounts and open switcher dialog
    fun switchAccount() {
        viewModelScope.launch {
            val accounts = (authRepository as? AuthRepositoryImpl)
                ?.savedAccountsManager
                ?.getSavedAccounts()
                ?.filter { it.handle != _uiState.value.initialHandle }
                ?: emptyList()

            _uiState.value = _uiState.value.copy(savedAccounts = accounts, showAccountSwitcher = true)
        }
    }

    // Sign in directly with saved account cookies
    fun loginWithSavedAccount(account: SavedAccount, onUserCaptured: (String?, String?, String?) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                showAccountSwitcher = false,
                isSwitchingAccount = true
            )
            // Hold overlay long enough for fade in + account data to settle
            delay(150.toDuration(DurationUnit.MILLISECONDS))
            try {
                authRepository.signInWithGoogle(account.cookies)

                _uiState.value = _uiState.value.copy(
                    isAuthenticated = true,
                    isLoading = false,
                    initialName = account.name,
                    initialHandle = account.handle,
                    initialPictureUrl = account.pictureUrl
                )

                onUserCaptured(account.name, account.handle, account.pictureUrl)
                fetchQuickPicks()
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to restore session",
                    isLoading = false
                )
            } finally {
                delay(150.toDuration(DurationUnit.MILLISECONDS))
                _uiState.value = _uiState.value.copy(isSwitchingAccount = false)
            }
        }
    }

    // Dismiss account switcher dialog
    fun dismissAccountSwitcher() {
        _uiState.value = _uiState.value.copy(showAccountSwitcher = false)
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

                // Fetch with explicit cookies before saving
                val impl = authRepository as? AuthRepositoryImpl
                val accountInfo = impl?.fetchAccountDetailsWithCookies(cookies)
                val name = accountInfo?.name
                val handle = accountInfo?.handle
                val pictureUrl = accountInfo?.pictureUrl

                if (accountInfo != null) {
                    impl.savedAccountsManager.saveAccount(accountInfo, cookies)
                }

                _uiState.value = _uiState.value.copy(
                    initialName = name,
                    initialHandle = handle,
                    initialPictureUrl = pictureUrl
                )

                onUserCaptured(name, handle, pictureUrl)
                fetchQuickPicks()
            } catch (_: Exception) {
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
            _uiState.value = _uiState.value.copy(
                isAuthenticated = false,
                initialName = null,
                initialHandle = null,
                initialPictureUrl = null
            )
            fetchQuickPicks()
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
    val authUrl: String? = null,
    val initialName: String? = null,
    val initialHandle: String? = null,
    val initialPictureUrl: String? = null,
    val savedAccounts: List<SavedAccount> = emptyList(),
    val showAccountSwitcher: Boolean = false,
    val isSwitchingAccount: Boolean = false,
    val quickPicks: List<QuickPick> = emptyList(),
    val isLoadingQuickPicks: Boolean = false
)
