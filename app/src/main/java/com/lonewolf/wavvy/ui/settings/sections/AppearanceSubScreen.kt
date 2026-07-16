package com.lonewolf.wavvy.ui.settings.sections

// Compose foundation and layout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
// Material 3 and icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.navigation.DefaultTab
import com.lonewolf.wavvy.ui.settings.components.SettingsGroupCard
import com.lonewolf.wavvy.ui.settings.components.SettingsInteractiveRow
import com.lonewolf.wavvy.ui.theme.Poppins
import com.lonewolf.wavvy.ui.theme.ThemeMode

// Visual theme and layout navigation subscreen layout
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSubScreen(
    isPlayerActive: Boolean,
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    currentDefaultTab: DefaultTab,
    onDefaultTabChange: (DefaultTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val internalScrollState = rememberScrollState()
    var showThemeSheet by remember { mutableStateOf(false) }
    var showDefaultTabSheet by remember { mutableStateOf(false) }

    val themeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val defaultTabSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(internalScrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SettingsGroupCard(title = stringResource(R.string.setting_subgroup_theme)) {
            val themeSubtitle = when (currentTheme) {
                ThemeMode.SYSTEM -> stringResource(R.string.setting_app_language_default)
                ThemeMode.LIGHT -> "Light mode"
                ThemeMode.DARK -> stringResource(R.string.setting_theme_dark_amoled)
            }

            SettingsInteractiveRow(
                title = stringResource(R.string.setting_theme),
                subtitle = themeSubtitle,
                icon = Icons.Rounded.DarkMode,
                showDivider = false,
                onClick = { showThemeSheet = true }
            )
        }

        SettingsGroupCard(title = stringResource(R.string.setting_subgroup_navigation)) {
            val defaultTabSubtitle = when (currentDefaultTab) {
                DefaultTab.HOME -> stringResource(R.string.setting_default_tab_home)
                DefaultTab.SEARCH -> stringResource(R.string.search_hint)
                DefaultTab.LIBRARY -> stringResource(R.string.nav_library)
            }

            SettingsInteractiveRow(
                title = stringResource(R.string.setting_default_tab),
                subtitle = defaultTabSubtitle,
                icon = Icons.Rounded.Home,
                showDivider = false,
                onClick = { showDefaultTabSheet = true }
            )
        }

        Spacer(modifier = Modifier.height(if (isPlayerActive) 110.dp else 16.dp))
    }

    // Theme selector modal
    if (showThemeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showThemeSheet = false },
            sheetState = themeSheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
            ) {
                Text(
                    text = stringResource(R.string.setting_theme),
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                ThemeMode.entries.forEachIndexed { index, mode ->
                    val isSelected = currentTheme == mode
                    val label = when (mode) {
                        ThemeMode.SYSTEM -> stringResource(R.string.setting_app_language_default)
                        ThemeMode.LIGHT -> "Light mode"
                        ThemeMode.DARK -> stringResource(R.string.setting_theme_dark_amoled)
                    }
                    val icon = when (mode) {
                        ThemeMode.SYSTEM -> Icons.Rounded.SettingsSuggest
                        ThemeMode.LIGHT -> Icons.Rounded.WbSunny
                        ThemeMode.DARK -> Icons.Rounded.Bedtime
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                onThemeChange(mode)
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val tintColor = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                        val backgroundTint = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        }
                        val contentTint = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(backgroundTint, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) contentTint else tintColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = label,
                            fontFamily = Poppins,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            color = tintColor,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (index < ThemeMode.entries.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                        )
                    }
                }
            }
        }
    }

    // Default tab selector modal
    if (showDefaultTabSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDefaultTabSheet = false },
            sheetState = defaultTabSheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
            ) {
                Text(
                    text = stringResource(R.string.setting_default_tab),
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                DefaultTab.entries.forEachIndexed { index, tab ->
                    val isSelected = currentDefaultTab == tab

                    val label = when (tab) {
                        DefaultTab.HOME -> stringResource(R.string.setting_default_tab_home)
                        DefaultTab.SEARCH -> stringResource(R.string.search_hint)
                        DefaultTab.LIBRARY -> stringResource(R.string.nav_library)
                    }
                    val icon = when (tab) {
                        DefaultTab.HOME -> Icons.Rounded.Home
                        DefaultTab.SEARCH -> Icons.Rounded.Search
                        DefaultTab.LIBRARY -> Icons.Rounded.LibraryMusic
                    }

                    // UI components
                    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    val textColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                onDefaultTabChange(tab)
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(containerColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = label,
                            fontFamily = Poppins,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (index < DefaultTab.entries.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                        )
                    }
                }
            }
        }
    }
}
