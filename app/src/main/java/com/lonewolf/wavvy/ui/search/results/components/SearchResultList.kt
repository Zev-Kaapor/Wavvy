package com.lonewolf.wavvy.ui.search.results.components

// Compose foundation and layout
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Scrollable list for search findings
@Composable
fun SearchResultList(
    query: String,
    onItemClick: (String) -> Unit
) {
    // Result container
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 80.dp,
            bottom = 120.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
    }
}
