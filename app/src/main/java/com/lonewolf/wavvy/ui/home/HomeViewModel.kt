package com.lonewolf.wavvy.ui.home

// Lifecycle and State
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Business logic and state management for Home
class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
}

// Data wrapper for Home screen state
data class HomeUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
