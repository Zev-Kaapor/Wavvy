package com.wavvy.app.features.auth.ui.viewmodel

// Android architecture and context
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// Local data storage and repositories
import com.wavvy.app.core.data.local.SettingsStorage
import com.wavvy.app.features.auth.data.AuthRepositoryImpl
import com.wavvy.app.features.auth.data.GuestProfile
import com.wavvy.app.features.auth.data.GuestProfilesManager
import com.wavvy.app.features.auth.data.SavedAccount
import com.wavvy.app.features.auth.ui.state.AuthUiState
// Coroutines reactive flows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Auth view model
class AuthViewModel(
    context: Context
) : ViewModel() {

    private val repository = AuthRepositoryImpl(context)
    private val authManager = AuthManager(context)
    private val settingsStorage = SettingsStorage(context)
    private val guestProfilesManager = GuestProfilesManager(context)

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        loadSavedAccounts()
    }

    // Load saved accounts
    fun loadSavedAccounts() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val accounts = repository.savedAccountsManager.getSavedAccounts()
                val guestProfiles = guestProfilesManager.getGuestProfiles()
                _uiState.value = AuthUiState.Success(accounts, guestProfiles)
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    // Start auth flow
    fun startGoogleAuthFlow() {
        authManager.clearSession()
        val authUrl = authManager.buildAuthUrl("login")
        val redirectUri = authManager.getRedirectUri()
        _uiState.value = AuthUiState.WebViewFlow(authUrl, redirectUri)
    }

    // Handle cookies captured
    fun handleCookiesCaptured(cookies: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            repository.signInWithGoogle(cookies)
                .onSuccess {
                    val details = repository.fetchAuthenticatedAccountDetails()
                    if (details != null) {
                        repository.savedAccountsManager.saveAccount(details, cookies)
                        repository.logout()
                        loadSavedAccounts()
                    } else {
                        _uiState.value = AuthUiState.Error("Failed to fetch account details")
                    }
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error(error.localizedMessage ?: "Sign in failed")
                }
        }
    }

    // Select account
    fun selectAccount(account: SavedAccount, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authManager.clearSession()
            repository.signInWithGoogle(account.cookies)
                .onSuccess {
                    loadSavedAccounts()
                    onComplete(true)
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error(error.localizedMessage ?: "Failed to switch account")
                    onComplete(false)
                }
        }
    }

    // Remove account
    fun removeAccount(account: SavedAccount) {
        viewModelScope.launch {
            repository.savedAccountsManager.removeAccount(account.handle, account.name)
            authManager.clearSession()
            loadSavedAccounts()
        }
    }

    // Create guest profile
    fun createGuestProfile(name: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            authManager.clearSession()
            repository.logout()
            val profile = guestProfilesManager.createProfile(name)
            settingsStorage.setGuestActive(true)
            settingsStorage.saveGuestName(profile.name)
            settingsStorage.saveActiveGuestId(profile.id)
            settingsStorage.saveGuestNameIsCustom(!profile.isAutoNamed)
            loadSavedAccounts()
            onComplete()
        }
    }

    // Select guest profile
    fun selectGuestProfile(profile: GuestProfile, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authManager.clearSession()
            repository.logout()
            settingsStorage.setGuestActive(true)
            settingsStorage.saveGuestName(profile.name)
            settingsStorage.saveActiveGuestId(profile.id)
            settingsStorage.saveGuestNameIsCustom(!profile.isAutoNamed)
            guestProfilesManager.touchProfile(profile.id)
            loadSavedAccounts()
            onComplete()
        }
    }

    // Remove guest profile
    fun removeGuestProfile(profile: GuestProfile) {
        viewModelScope.launch {
            guestProfilesManager.removeProfile(profile.id)
            loadSavedAccounts()
        }
    }
}
