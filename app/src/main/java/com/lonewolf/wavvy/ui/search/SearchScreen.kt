package com.lonewolf.wavvy.ui.search

// Compose foundation and animations
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
// State and UI
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
// Project components
import com.lonewolf.wavvy.ui.search.components.SearchBar
import com.lonewolf.wavvy.ui.search.results.SearchResultScreen

// Main search feature coordinator
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit = {}
) {
    // Search state persistence
    var activeQuery by rememberSaveable { mutableStateOf("") }
    var isResultsVisible by rememberSaveable { mutableStateOf(false) }

    // System back navigation logic
    BackHandler {
        if (isResultsVisible) {
            isResultsVisible = false
        } else {
            onNavigateBack()
        }
    }

    // Smooth transition between search and results
    AnimatedContent(
        targetState = isResultsVisible,
        transitionSpec = {
            fadeIn(animationSpec = tween(400)) togetherWith
                    fadeOut(animationSpec = tween(400))
        },
        label = "search_state_transition"
    ) { showResults ->
        if (showResults) {
            // Results view
            SearchResultScreen(
                query = activeQuery,
                onBack = { isResultsVisible = false }
            )
        } else {
            // Search bar and history view
            SearchBar(
                onSearch = { query ->
                    activeQuery = query
                    isResultsVisible = true
                }
            )
        }
    }
}
