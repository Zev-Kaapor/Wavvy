package com.wavvy.app.features.library.ui

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
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.FilterPills
import com.wavvy.app.features.library.ui.components.SortBar
import com.wavvy.app.features.home.ui.components.HomeHeader
// Coroutines for animations
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun LibraryScreen(
    isAuthenticated: Boolean,
    isGuestActive: Boolean = false,
    userName: String?,
    userHandle: String?,
    userProfilePicture: String?,
    onLoginClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateBack: () -> Unit
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

    LazyColumn(
        state = listState,
        userScrollEnabled = !isKeyboardOpen,
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .then(
                if (isSearching) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    scope.launch {
                        delay(200.milliseconds)
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
                isGuestActive = isGuestActive,
                userName = userName,
                userHandle = userHandle,
                userProfilePicture = userProfilePicture,
                onNavigateToSettings = onNavigateToSettings,
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
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }

        // Sort bar
        item(key = "library_sort") {
            Box(
                modifier = Modifier
                    .bringIntoViewRequester(bringIntoViewRequester)
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
                                delay(300.milliseconds)
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
