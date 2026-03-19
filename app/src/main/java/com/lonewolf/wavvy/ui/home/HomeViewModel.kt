package com.lonewolf.wavvy.ui.home

// Lifecycle and state management
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Logic and state management for Home screen
class HomeViewModel : ViewModel() {
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
    val masterMoodList: List<String> = emptyList()
)
