package com.wavvy.app.features.auth.ui.state

// Domain data models
import com.wavvy.app.features.auth.data.GuestProfile
import com.wavvy.app.features.auth.data.SavedAccount

// Auth ui state
sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    data class Success(
        val savedAccounts: List<SavedAccount>,
        val guestProfiles: List<GuestProfile> = emptyList()
    ) : AuthUiState
    data class WebViewFlow(val authUrl: String, val redirectUri: String) : AuthUiState
    data class Error(val message: String) : AuthUiState
}
