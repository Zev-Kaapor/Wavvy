package com.lonewolf.wavvy.ui.library

// Compose foundation and layout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
// Material 3 components
import androidx.compose.runtime.*
// UI styling and utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.unit.dp
// Project resources
import com.lonewolf.wavvy.R
// Project components
import com.lonewolf.wavvy.ui.common.components.FilterPills
import com.lonewolf.wavvy.ui.home.components.HomeHeader
import com.lonewolf.wavvy.ui.library.components.SortBar

@Composable
fun LibraryScreen(
    onNavigateBack: () -> Unit = {}
) {
    // System back navigation
    BackHandler {
        onNavigateBack()
    }

    // Library state management
    val libraryFilters = stringArrayResource(R.array.library_filters).toList()
    var selectedFilter by remember { mutableStateOf("") }
    var selectedSort by remember { mutableStateOf("") }
    var isDescending by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Dynamic sort options logic
    val sortOptions = when (selectedFilter) {
        "Músicas" -> stringArrayResource(R.array.library_sort_songs)
        "Álbuns" -> stringArrayResource(R.array.library_sort_albums)
        "Playlists", "Artistas", "Podcasts" -> stringArrayResource(R.array.library_sort_collections)
        else -> stringArrayResource(R.array.library_sort_default)
    }.toList()

    // Reset sort selection when filter changes
    LaunchedEffect(selectedFilter) {
        selectedSort = sortOptions.first()
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        HomeHeader(onNavigateToSettings = { })

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Library categories
            item(key = "library_filters") {
                FilterPills(
                    availableFilters = libraryFilters,
                    selectedFilter = selectedFilter,
                    onFilterSelected = { selectedFilter = it },
                    onInitializeFilters = { },
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // List controller with sort and search actions
            item(key = "library_sort") {
                SortBar(
                    selectedSort = selectedSort,
                    onSortSelected = { selectedSort = it },
                    isDescending = isDescending,
                    onToggleDirection = { isDescending = !isDescending },
                    sortOptions = sortOptions,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it }
                )
            }
        }
    }
}
