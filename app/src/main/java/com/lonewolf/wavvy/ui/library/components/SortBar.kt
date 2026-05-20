package com.lonewolf.wavvy.ui.library.components

// Animation mechanics
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
// Foundation and interaction
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
// Material icons and components
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.theme.Poppins
import com.lonewolf.wavvy.ui.theme.accentCyan

// Library sorting and search controller
@Composable
fun SortBar(
    selectedSort: String,
    onSortSelected: (String) -> Unit,
    isDescending: Boolean,
    onToggleDirection: () -> Unit,
    sortOptions: List<String>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // UI states
    var isSearchActive by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val isDark = isSystemInDarkTheme()

    // Search closure helper
    val closeSearch = {
        isSearchActive = false
        onSearchQueryChange("")
        focusManager.clearFocus()
        onSearchActiveChange(false)
    }

    // Auto-focus logic
    LaunchedEffect(isSearchActive) {
        if (isSearchActive) focusRequester.requestFocus()
    }

    // Main layout container
    Box(modifier = modifier) {
        // Outside click detector
        if (isSearchActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = { closeSearch() }
                    )
            )
        }

        // Morphing animation between Sort and Search
        AnimatedContent(
            targetState = isSearchActive,
            transitionSpec = {
                if (targetState) {
                    (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                } else {
                    (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                } using SizeTransform(clip = false)
            },
            label = "sort_search_morph"
        ) { active ->
            if (active) {
                // Expanded Search View
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = if (isDark) MaterialTheme.accentCyan else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 12.dp).size(18.dp)
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search_hint),
                                style = TextStyle(
                                    fontFamily = Poppins,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                            textStyle = TextStyle(
                                fontFamily = Poppins,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(if (isDark) MaterialTheme.accentCyan else MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            singleLine = true
                        )
                    }

                    IconButton(onClick = { closeSearch() }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                // Default Sort View
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    val rotation by animateFloatAsState(
                        targetValue = if (isDescending) 0f else 180f,
                        label = "arrow_rotation"
                    )

                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box {
                            Row(
                                modifier = Modifier.clickable { expanded = true },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )

                                Text(
                                    text = selectedSort.ifEmpty { sortOptions.firstOrNull() ?: "" },
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = Poppins,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }

                            SortDropdown(
                                expanded = expanded,
                                onDismiss = { expanded = false },
                                options = sortOptions,
                                selectedOption = selectedSort,
                                onOptionSelected = onSortSelected
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = onToggleDirection,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp).rotate(rotation)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            isSearchActive = true
                            onSearchActiveChange(true)
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// Custom sort dropdown popup
@Composable
private fun SortDropdown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var isTransitioning by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()

    LaunchedEffect(expanded) {
        if (expanded) isTransitioning = true
    }

    if (expanded || isTransitioning) {
        Popup(
            onDismissRequest = { if (isTransitioning) onDismiss() },
            properties = PopupProperties(focusable = true),
            offset = IntOffset(0, 70)
        ) {
            AnimatedVisibility(
                visible = expanded && isTransitioning,
                enter = fadeIn(tween(200)) + scaleIn(
                    initialScale = 0.4f,
                    transformOrigin = TransformOrigin(0f, 0f),
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
                ),
                exit = fadeOut(tween(150)) + scaleOut(
                    targetScale = 0.4f,
                    transformOrigin = TransformOrigin(0f, 0f),
                    animationSpec = tween(150)
                )
            ) {
                DisposableEffect(Unit) {
                    onDispose { isTransitioning = false }
                }

                Surface(
                    modifier = Modifier
                        .width(240.dp)
                        .padding(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = if (isDark) 16.dp else 8.dp
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        options.forEach { option ->
                            val isSelected = option == selectedOption || (selectedOption.isEmpty() && option == (options.firstOrNull() ?: ""))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        onOptionSelected(option)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily = Poppins,
                                        fontSize = 15.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = if (isSelected && isDark) MaterialTheme.accentCyan
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                )

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (isDark) MaterialTheme.accentCyan else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
