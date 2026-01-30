package com.lonewolf.wavvy.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.lonewolf.wavvy.ui.common.FloatingNavBar
import com.lonewolf.wavvy.ui.common.PlayerContainer
import com.lonewolf.wavvy.ui.home.components.*

@Composable
fun HomeScreen(userName: String? = null) {
    var isMiniPlayerActive by rememberSaveable { mutableStateOf(false) }
    var isPlayerExpanded by rememberSaveable { mutableStateOf(false) }
    var currentSongTitle by rememberSaveable { mutableStateOf("") }
    var currentArtistName by rememberSaveable { mutableStateOf("") }
    var currentImageUrl by rememberSaveable { mutableStateOf<String?>(null) }

    val onMusicClick = { title: String, artist: String ->
        currentSongTitle = title
        currentArtistName = artist
        currentImageUrl = null
        isMiniPlayerActive = true
        isPlayerExpanded = false
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item { HomeHeader(onNavigateToSettings = { }) }
            item { GreetingSection(userName = userName) }
            item { FilterPills() }
            item { FastMusicGrid(onItemClick = { onMusicClick(it, "Wavvy Artist") }) }
            item { RecentSection(onItemClick = { onMusicClick(it, "Wavvy Artist") }) }
            item { PersonalizedSection(onItemClick = { }) }
            item { ArtistSection(onItemClick = { }) }
            item { GenreSection(onItemClick = { }) }
            item { MoodSection(onItemClick = { mood -> onMusicClick(mood, "Mix") }) }
            item { FinalPilaresSection(onItemClick = { title -> if (!title.contains("IA")) onMusicClick(title, "LyraWav") }) }
            item { Spacer(modifier = Modifier.height(180.dp)) }
        }

        // Navbar Container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(2f),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 20.dp)
            ) {
                FloatingNavBar()
            }
        }

        // Player Container logic
        if (isMiniPlayerActive && currentSongTitle.isNotEmpty()) {
            PlayerContainer(
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
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(3f)
            )
        }
    }
}
