package com.lonewolf.wavvy.ui.search.components

// Compose foundation and layout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
// Material icons and components
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
// State and UI
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.data.SearchHistoryManager
import com.lonewolf.wavvy.ui.theme.Poppins
// Coroutines
import kotlinx.coroutines.launch

// Main search component
@Composable
fun SearchBar(
    onSearch: (String) -> Unit = {}
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // History manager instance
    val historyManager: SearchHistoryManager = remember { SearchHistoryManager(context) }

    // Search history observer
    val searchHistory: List<String> by historyManager.history.collectAsState(initial = emptyList())

    // Execution logic for searching
    val onPerformSearch = {
        if (searchQuery.isNotBlank()) {
            val trimmedQuery = searchQuery.trim()
            scope.launch {
                historyManager.saveSearch(trimmedQuery)
                onSearch(trimmedQuery)
            }
            focusManager.clearFocus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Search input field
        SearchTopBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onSearchAction = onPerformSearch
        )

        // Conditional content area
        if (searchQuery.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                if (searchHistory.isNotEmpty()) {
                    // History header section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.menu_history),
                            fontFamily = Poppins,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )

                        // Clear history trigger
                        TextButton(
                            onClick = { scope.launch { historyManager.clearAll() } },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Text(
                                text = stringResource(R.string.search_clear_all),
                                fontFamily = Poppins,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Recent searches list
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(items = searchHistory) { item ->
                            RecentSearchItem(
                                text = item,
                                onClick = {
                                    searchQuery = item
                                    onSearch(item)
                                },
                                onRemove = { scope.launch { historyManager.removeItem(item) } }
                            )
                        }
                    }
                }
            }
        } else {
            // Live search status
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${stringResource(R.string.search_prefix_loading)} $searchQuery",
                    fontFamily = Poppins,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontSize = 16.sp
                )
            }
        }
    }
}

// Individual recent search entry
@Composable
private fun RecentSearchItem(
    text: String,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // History icon
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        // Query text
        Text(
            text = text,
            fontFamily = Poppins,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )
        // Remove item button
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// Custom search text field
@Composable
private fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(12.dp)),
            placeholder = {
                Text(
                    text = stringResource(R.string.search_hint),
                    fontFamily = Poppins,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                IconButton(onClick = onSearchAction) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.tertiary
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearchAction() }),
            shape = RoundedCornerShape(12.dp)
        )
    }
}
