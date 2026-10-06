package com.wavvy.app.features.home.ui

// Compose layouts and foundations
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI utilities
import androidx.compose.ui.Modifier
// Project resources
import com.wavvy.app.features.home.ui.components.HomeHeader
import com.wavvy.app.features.home.ui.components.HomeSkeleton

// Home tab, header and the loading placeholder for now and the rest is built one piece at a time
@Composable
fun HomeScreen(
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // The bell does nothing yet
        HomeHeader(onNotificationsClick = {}, onProfileClick = onProfileClick)

        // There is no data yet, so the whole body is the placeholder
        HomeSkeleton(modifier = Modifier.weight(1f))
    }
}
