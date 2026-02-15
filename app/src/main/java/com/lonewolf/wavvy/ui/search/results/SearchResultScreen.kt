package com.lonewolf.wavvy.ui.search.results

// Compose foundation and layout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Modifier
// Project components
import com.lonewolf.wavvy.ui.search.results.components.SearchResultList

// Screen to display search results
@Composable
fun SearchResultScreen(
    query: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Results list container
        SearchResultList(
            query = query,
            onItemClick = { /* Handle song play */ }
        )
    }
}
