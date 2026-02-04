package com.lonewolf.wavvy.ui.home

// Compose foundation and layout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
// State management
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
// Tools and positioning
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
// Shared and internal components
import com.lonewolf.wavvy.ui.common.FloatingNavBar
import com.lonewolf.wavvy.ui.common.HomeHeader
import com.lonewolf.wavvy.ui.player.PlayerSheet
import com.lonewolf.wavvy.ui.home.components.*

// Main home entry point with player integration
@Composable
fun HomeScreen(userName: String? = null) {
    var isMiniPlayerActive by rememberSaveable { mutableStateOf(false) }
    var isPlayerExpanded by rememberSaveable { mutableStateOf(false) }
    var currentSongTitle by rememberSaveable { mutableStateOf("") }
    var currentArtistName by rememberSaveable { mutableStateOf("") }
    var currentImageUrl by rememberSaveable { mutableStateOf<String?>(null) }

    // Music playback trigger
    val onMusicClick = { title: String, artist: String ->
        currentSongTitle = title
        currentArtistName = artist
        currentImageUrl = "https://i.redd.it/n8eg9vjl2xg71.jpg"
        isMiniPlayerActive = true
        isPlayerExpanded = false
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Main scrollable content
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item { HomeHeader(onNavigateToSettings = { }) }
            item { GreetingSection(userName = userName) }
            item { FilterPills() }
            item { FastMusicGrid(onItemClick = { onMusicClick(it, "Wavvy Artist") }) }
            item { RecentCard(onItemClick = { onMusicClick(it, "Wavvy Artist") }) }
            item { PersonalizedCard(onItemClick = { }) }
            item { ArtistSection(onItemClick = { }) }
            item { GenreSection(onItemClick = { }) }
            item { MoodSection(onItemClick = { mood -> onMusicClick(mood, "Mix") }) }
            item { FinalPilaresSection(onItemClick = { title -> if (!title.contains("IA")) onMusicClick(title, "LyraWav") }) }
            item { Spacer(modifier = Modifier.height(180.dp)) }
        }

        // Navigation UI
        Box(
            modifier = Modifier.fillMaxSize().zIndex(2f),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(modifier = Modifier.padding(bottom = 20.dp)) {
                FloatingNavBar()
            }
        }

        // Global playback sheet
        if (isMiniPlayerActive && currentSongTitle.isNotEmpty()) {
            PlayerSheet(
                isExpanded = isPlayerExpanded,
                songTitle = currentSongTitle,
                artistName = currentArtistName,
                imageUrl = currentImageUrl,
                onPillClick = { isPlayerExpanded = !isPlayerExpanded },
                onDismiss = {
                    isMiniPlayerActive = false
                    isPlayerExpanded = false
                },
                onProgressUpdate = { },
                modifier = Modifier.fillMaxSize().zIndex(3f)
            )
        }
    }
}
