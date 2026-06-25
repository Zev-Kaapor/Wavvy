package com.lonewolf.wavvy.ui.home

// Lifecycle components
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
// Infrastructure and dependencies
import com.lonewolf.wavvy.data.AuthRepository
import com.lonewolf.wavvy.data.RecentHistoryManager
import com.lonewolf.wavvy.ui.auth.AuthManager

class HomeViewModelFactory(
    private val authManager: AuthManager,
    private val authRepository: AuthRepository,
    private val recentHistoryManager: RecentHistoryManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(authManager, authRepository, recentHistoryManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
