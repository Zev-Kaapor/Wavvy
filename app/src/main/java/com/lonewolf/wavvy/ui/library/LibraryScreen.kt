package com.lonewolf.wavvy.ui.library

// Navigation support
import androidx.activity.compose.BackHandler
// Layout and UI components
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
// Relocation API for keyboard handling
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
// Runtime state management
import androidx.compose.runtime.*
// UI tools
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.unit.dp
// Project specific components
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.common.components.FilterPills
import com.lonewolf.wavvy.ui.home.components.HomeHeader
import com.lonewolf.wavvy.ui.library.components.SortBar
// Coroutines for animations
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(
    isAuthenticated: Boolean,
    userEmail: String?,
    userProfilePicture: String?,
    onLoginClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onNavigateBack: () -> Unit = {}
) {
    // Back navigation
    BackHandler { onNavigateBack() }

    // Library state
    val libraryFilters = stringArrayResource(R.array.library_filters).toList()
    var selectedFilter by remember { mutableStateOf("") }
    var selectedSort by remember { mutableStateOf("") }
    var isDescending by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    // Scroll state
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Visibility requester
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    // Keyboard detection
    val imeInsets = WindowInsets.ime
    val isKeyboardOpen = imeInsets.getBottom(LocalDensity.current) > 0

    // Dynamic sort options
    val sortOptions = when (selectedFilter) {
        "Músicas" -> stringArrayResource(R.array.library_sort_songs)
        "Álbuns" -> stringArrayResource(R.array.library_sort_albums)
        "Playlists", "Artistas", "Podcasts" -> stringArrayResource(R.array.library_sort_collections)
        else -> stringArrayResource(R.array.library_sort_default)
    }.toList()

    // Sort reset
    LaunchedEffect(selectedFilter) { selectedSort = sortOptions.first() }

    // Lazy layout structure
    LazyColumn(
        state = listState,
        userScrollEnabled = !isKeyboardOpen,
        modifier = Modifier
            .fillMaxSize()
            // Animate keyboard transitions smoothly
            .imePadding()
            .then(
                if (isSearching) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    scope.launch {
                        // Allow animation to finish before clearing focus
                        delay(200)
                        focusManager.clearFocus()
                    }
                } else Modifier
            ),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Header
        item(key = "header") {
            HomeHeader(
                isAuthenticated = isAuthenticated,
                userEmail = userEmail,
                userProfilePicture = userProfilePicture,
                onNavigateToSettings = { },
                onLoginClick = onLoginClick,
                onSignOutClick = onSignOutClick
            )
        }

        // Filter pills
        item(key = "library_filters") {
            FilterPills(
                availableFilters = libraryFilters,
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it },
                onInitializeFilters = { },
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // Sort bar
        item(key = "library_sort") {
            // Visibility adjustment
            Box(
                modifier = Modifier
                    .bringIntoViewRequester(bringIntoViewRequester)
                    // Offset buffer for keyboard
                    .padding(bottom = if (isKeyboardOpen) 50.dp else 0.dp)
            ) {
                SortBar(
                    selectedSort = selectedSort,
                    onSortSelected = { selectedSort = it },
                    isDescending = isDescending,
                    onToggleDirection = { isDescending = !isDescending },
                    sortOptions = sortOptions,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onSearchActiveChange = { active ->
                        isSearching = active
                        if (active) {
                            scope.launch {
                                // Defer scroll to ensure layout settles
                                delay(300)
                                bringIntoViewRequester.bringIntoView()
                            }
                        }
                    }
                )
            }
        }

        // Scroll spacer
        item(key = "spacer") {
            Spacer(modifier = Modifier.height(200.dp))
        }
    }
}
