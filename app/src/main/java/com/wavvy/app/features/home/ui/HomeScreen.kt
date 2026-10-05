package com.wavvy.app.features.home.ui

// Compose layouts and foundations
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI utilities
import androidx.compose.ui.Modifier
// Project resources
import com.wavvy.app.features.home.ui.components.HomeFiltersPlaceholder
import com.wavvy.app.features.home.ui.components.HomeHeader

// Home tab, header and filter placeholders for now and the rest is built one piece at a time
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        // The header buttons do nothing yet
        HomeHeader(onNotificationsClick = {}, onProfileClick = {})

        // There is no data yet, so the filters are only placeholders
        HomeFiltersPlaceholder()
    }
}
