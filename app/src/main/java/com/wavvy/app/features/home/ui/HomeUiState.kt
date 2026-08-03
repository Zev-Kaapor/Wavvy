package com.wavvy.app.features.home.ui

// Navigation state and domain types
import com.wavvy.app.features.home.models.QuickPick
import com.wavvy.app.features.home.ui.components.ForgottenTrack
import com.wavvy.app.features.home.ui.components.RecentTrack

data class HomeUiState(
    val isAuthenticated: Boolean = false,
    val isGuestActive: Boolean = false,
    val isGuestNameCustom: Boolean = false,
    val initialName: String? = null,
    val initialHandle: String? = null,
    val initialPictureUrl: String? = null,
    val greeting: String? = null,
    val question: String? = null,
    val availableFilters: List<String> = emptyList(),
    val selectedFilter: String = "",
    val quickPicks: List<QuickPick> = emptyList(),
    val isLoadingQuickPicks: Boolean = false,
    val recentTracks: List<RecentTrack> = emptyList(),
    val forgottenFavorites: List<ForgottenTrack> = emptyList()
)
