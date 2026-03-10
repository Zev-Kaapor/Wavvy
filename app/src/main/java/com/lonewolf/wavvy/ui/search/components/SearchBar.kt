package com.lonewolf.wavvy.ui.search.components

// Compose foundation and layout
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
// Material icons and components
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.data.SearchHistoryManager
import com.lonewolf.wavvy.ui.theme.Poppins
import com.lonewolf.wavvy.ui.theme.accentCyan
// Coroutines
import kotlinx.coroutines.launch

// Main search component
@Composable
fun SearchBar(
    onSearch: (String) -> Unit = {},
    onBack: () -> Unit = {},
    isResultsVisible: Boolean = false,
    externalQuery: String = "",
    onQueryChange: (String) -> Unit = {}
) {
    // State for the query
    var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(externalQuery))
    }

    // Sync internal state with external query resets
    LaunchedEffect(externalQuery) {
        if (externalQuery != textFieldValue.text) {
            textFieldValue = TextFieldValue(
                text = externalQuery,
                selection = TextRange(externalQuery.length)
            )
        }
    }

    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val historyManager: SearchHistoryManager = remember { SearchHistoryManager(context) }
    val searchHistory: List<String> by historyManager.history.collectAsState(initial = emptyList())

    val onPerformSearch = {
        if (textFieldValue.text.isNotBlank()) {
            val trimmedQuery = textFieldValue.text.trim()
            scope.launch {
                historyManager.saveSearch(trimmedQuery)
                onSearch(trimmedQuery)
            }
            focusManager.clearFocus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        SearchTopBar(
            textFieldValue = textFieldValue,
            onValueChange = {
                textFieldValue = it
                onQueryChange(it.text)
            },
            onSearchAction = onPerformSearch,
            onBackClick = onBack,
            onClearClick = {
                textFieldValue = TextFieldValue("")
                onQueryChange("")
            }
        )

        // UI components
        if (!isResultsVisible && textFieldValue.text.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                if (searchHistory.isNotEmpty()) {
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

                        TextButton(
                            onClick = { scope.launch { historyManager.clearAll() } },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.accentCyan)
                        ) {
                            Text(
                                text = stringResource(R.string.search_clear_all),
                                fontFamily = Poppins,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(items = searchHistory) { item ->
                            RecentSearchItem(
                                text = item,
                                onClick = {
                                    textFieldValue = TextFieldValue(
                                        text = item,
                                        selection = TextRange(item.length)
                                    )
                                    onSearch(item)
                                },
                                onRemove = { scope.launch { historyManager.removeItem(item) } }
                            )
                        }
                    }
                }
            }
        }
    }
}

// Custom search top bar with locked vertical transitions
@Composable
private fun SearchTopBar(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onSearchAction: () -> Unit,
    onBackClick: () -> Unit,
    onClearClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.close_button),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        TextField(
            value = textFieldValue,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .clip(CircleShape),
            placeholder = {
                Text(
                    text = stringResource(R.string.search_placeholder_yt),
                    fontFamily = Poppins,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                AnimatedContent(
                    targetState = textFieldValue.text.isEmpty(),
                    transitionSpec = {
                        val duration = 150
                        if (targetState) {
                            (slideInVertically(tween(duration)) { -it } + fadeIn(tween(duration)))
                                .togetherWith(slideOutVertically(tween(duration)) { it } + fadeOut(tween(duration)))
                        } else {
                            (slideInVertically(tween(duration)) { it } + fadeIn(tween(duration)))
                                .togetherWith(slideOutVertically(tween(duration)) { -it } + fadeOut(tween(duration)))
                        }.using(SizeTransform(clip = false))
                    },
                    modifier = Modifier.padding(end = 4.dp),
                    contentAlignment = Alignment.Center,
                    label = "icon_vertical_transition"
                ) { isQueryEmpty ->
                    if (isQueryEmpty) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = if (isDark) Color.White else Color.Black,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        IconButton(onClick = onClearClick) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.search_clear_all),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.accentCyan
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearchAction() }),
            shape = CircleShape
        )

        Spacer(modifier = Modifier.width(8.dp))
    }
}

// Recent search item
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
