package com.lonewolf.wavvy.ui.search.results.components

// Compose foundation and layout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
// Material 3 components
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Scrollable list for search findings
@Composable
fun SearchResultList(
    query: String,
    onItemClick: (String) -> Unit
) {
    // Result container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Vertical results list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 120.dp
            )
        ) {
            // Future result items will be placed here
        }
    }
}
