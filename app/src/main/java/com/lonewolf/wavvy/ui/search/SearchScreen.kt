package com.lonewolf.wavvy.ui.search

// Activity and Compose foundation
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
// State and UI
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
// Project components
import com.lonewolf.wavvy.ui.home.PlayerState
import com.lonewolf.wavvy.ui.search.components.SearchBar

// Main search feature coordinator
@Composable
fun SearchScreen(
    playerState: PlayerState,
    onNavigateBack: () -> Unit = {}
) {
    // Search state persistence
    var activeQuery by rememberSaveable { mutableStateOf("") }
    var isResultsVisible by rememberSaveable { mutableStateOf(false) }

    // Unified back logic to reset search when leaving results
    val handleBackAction = {
        if (isResultsVisible) {
            isResultsVisible = false
            activeQuery = ""
        } else {
            onNavigateBack()
        }
    }

    // System back button handler
    BackHandler(onBack = handleBackAction)

    Column(modifier = Modifier.fillMaxSize()) {
        // Persistent search bar component
        SearchBar(
            isResultsVisible = isResultsVisible,
            externalQuery = activeQuery,
            onSearch = { query ->
                activeQuery = query
                isResultsVisible = true
            },
            onBack = handleBackAction,
            onQueryChange = { query ->
                activeQuery = query
                if (query.isEmpty()) isResultsVisible = false
            }
        )

        // State transition between History (Empty Box) and Results
        AnimatedContent(
            targetState = isResultsVisible,
            transitionSpec = {
                fadeIn(animationSpec = tween(250)) togetherWith
                        fadeOut(animationSpec = tween(250))
            },
            label = "search_content_transition",
            modifier = Modifier.fillMaxSize()
        ) { showResults ->
            if (showResults) {
                SearchResultScreen(
                    query = activeQuery,
                    playerState = playerState,
                    onBack = handleBackAction
                )
            } else {
                // Background area when history is showing
                Box(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
