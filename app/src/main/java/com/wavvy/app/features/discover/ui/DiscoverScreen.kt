package com.wavvy.app.features.discover.ui

// Compose layout and foundation
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
// Material 3 components
import androidx.compose.material3.MaterialTheme
// Compose runtime and configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
// Project design system and state
import com.wavvy.app.features.home.ui.PlayerState
import com.wavvy.app.features.discover.ui.components.GenreSection
import com.wavvy.app.features.discover.ui.components.PersonalizedCard
import com.wavvy.app.features.home.ui.components.HomeHeader

@Composable
fun DiscoverScreen(
    modifier: Modifier = Modifier,
    playerState: PlayerState,
    isAuthenticated: Boolean,
    isGuestActive: Boolean = false,
    userName: String?,
    userHandle: String?,
    userProfilePicture: String?,
    onNavigateToLogin: () -> Unit,
    onSignOut: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Shared top header section delivering ecosystem branding
        HomeHeader(
            isAuthenticated = isAuthenticated,
            isGuestActive = isGuestActive,
            userName = userName,
            userHandle = userHandle,
            userProfilePicture = userProfilePicture,
            onNavigateToSettings = onNavigateToSettings,
            onLoginClick = onNavigateToLogin,
            onSignOutClick = onSignOut
        )

        // Main content list setup
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = if (playerState.isMiniPlayerActive) 140.dp else 100.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // Horizontal discovery chips
            PersonalizedCard(
                onItemClick = { /* Handle item navigation */ }
            )

            // Horizontal grid section for musical genres
            GenreSection(
                onItemClick = { /* Handle genre selection */ }
            )
        }
    }
}
