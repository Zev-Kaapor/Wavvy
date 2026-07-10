package com.lonewolf.wavvy.ui.settings

// Compose foundation and layout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
// Material 3 and icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ManageSearch
import androidx.compose.material.icons.automirrored.rounded.VolumeMute
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
// State and UI tools
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.theme.Poppins

// Navigation states
private enum class SettingsSubSection {
    MAIN, GENERAL, APPEARANCE, PLAYER, CONTENT, PRIVACY, STORAGE, BACKUP, LINKS, ABOUT
}

// Main screen entry point
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    queueLimit: Int,
    onQueueLimitChange: (Int) -> Unit,
    onClearPlaybackHistory: () -> Unit,
    onClearSearchHistory: () -> Unit,
    onNavigateBack: () -> Unit,
    scrollState: ScrollState,
    isPlayerActive: Boolean,
    modifier: Modifier = Modifier
) {
    var currentSubSection by remember { mutableStateOf(SettingsSubSection.MAIN) }

    // Intercept back button navigation
    BackHandler {
        if (currentSubSection == SettingsSubSection.MAIN) {
            onNavigateBack()
        } else {
            currentSubSection = SettingsSubSection.MAIN
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentSubSection) {
                            SettingsSubSection.MAIN -> stringResource(R.string.menu_settings)
                            SettingsSubSection.GENERAL -> stringResource(R.string.setting_section_general)
                            SettingsSubSection.APPEARANCE -> stringResource(R.string.setting_section_appearance)
                            SettingsSubSection.PLAYER -> stringResource(R.string.setting_section_player)
                            SettingsSubSection.CONTENT -> stringResource(R.string.setting_section_content)
                            SettingsSubSection.PRIVACY -> stringResource(R.string.setting_section_privacy)
                            SettingsSubSection.STORAGE -> stringResource(R.string.setting_section_storage)
                            SettingsSubSection.BACKUP -> stringResource(R.string.setting_section_backup)
                            SettingsSubSection.LINKS -> stringResource(R.string.setting_section_links)
                            SettingsSubSection.ABOUT -> stringResource(R.string.setting_section_about)
                        },
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (currentSubSection == SettingsSubSection.MAIN) {
                                onNavigateBack()
                            } else {
                                currentSubSection = SettingsSubSection.MAIN
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.close_button)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen transition animation
            AnimatedContent(
                targetState = currentSubSection,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                },
                label = "SettingsFadeTransition"
            ) { targetSection ->
                when (targetSection) {
                    SettingsSubSection.MAIN -> {
                        MainSettingsList(
                            scrollState = scrollState,
                            isPlayerActive = isPlayerActive,
                            onNavigateToSub = { section -> currentSubSection = section }
                        )
                    }
                    SettingsSubSection.GENERAL -> GeneralSubScreen(isPlayerActive = isPlayerActive)
                    SettingsSubSection.APPEARANCE -> AppearanceSubScreen(isPlayerActive = isPlayerActive)
                    SettingsSubSection.PLAYER -> PlayerSubScreen(queueLimit = queueLimit, onQueueLimitChange = onQueueLimitChange, isPlayerActive = isPlayerActive)
                    SettingsSubSection.CONTENT -> ContentSubScreen(isPlayerActive = isPlayerActive)
                    SettingsSubSection.PRIVACY -> PrivacySubScreen(
                        onClearPlaybackHistory = onClearPlaybackHistory,
                        onClearSearchHistory = onClearSearchHistory,
                        isPlayerActive = isPlayerActive
                    )
                    SettingsSubSection.STORAGE -> StorageSubScreen(isPlayerActive = isPlayerActive)
                    SettingsSubSection.BACKUP -> BackupSubScreen(isPlayerActive = isPlayerActive)
                    SettingsSubSection.LINKS -> LinksSubScreen(isPlayerActive = isPlayerActive)
                    SettingsSubSection.ABOUT -> AboutSubScreen(isPlayerActive = isPlayerActive)
                }
            }
        }
    }
}

// Main list structure
@Composable
private fun MainSettingsList(
    scrollState: ScrollState,
    isPlayerActive: Boolean,
    onNavigateToSub: (SettingsSubSection) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // App experience group
        SettingsGroupCard(title = stringResource(R.string.setting_group_app)) {
            SettingsItemRow(
                title = stringResource(R.string.setting_section_general),
                subtitle = stringResource(R.string.setting_section_general_subtitle),
                icon = Icons.Rounded.Settings,
                onClick = { onNavigateToSub(SettingsSubSection.GENERAL) },
                showDivider = true
            )
            SettingsItemRow(
                title = stringResource(R.string.setting_section_appearance),
                subtitle = stringResource(R.string.setting_section_appearance_subtitle),
                icon = Icons.Rounded.Palette,
                onClick = { onNavigateToSub(SettingsSubSection.APPEARANCE) },
                showDivider = false
            )
        }

        // Media group
        SettingsGroupCard(title = stringResource(R.string.setting_group_playback)) {
            SettingsItemRow(
                title = stringResource(R.string.setting_section_player),
                subtitle = stringResource(R.string.setting_section_player_subtitle),
                icon = Icons.Rounded.MusicNote,
                onClick = { onNavigateToSub(SettingsSubSection.PLAYER) },
                showDivider = true
            )
            SettingsItemRow(
                title = stringResource(R.string.setting_section_content),
                subtitle = stringResource(R.string.setting_section_content_subtitle),
                icon = Icons.Rounded.Tune,
                onClick = { onNavigateToSub(SettingsSubSection.CONTENT) },
                showDivider = false
            )
        }

        // Security group
        SettingsGroupCard(title = stringResource(R.string.setting_group_data_security)) {
            SettingsItemRow(
                title = stringResource(R.string.setting_section_privacy),
                subtitle = stringResource(R.string.setting_section_privacy_subtitle),
                icon = Icons.Rounded.Security,
                onClick = { onNavigateToSub(SettingsSubSection.PRIVACY) },
                showDivider = true
            )
            SettingsItemRow(
                title = stringResource(R.string.setting_section_storage),
                subtitle = stringResource(R.string.setting_section_storage_subtitle),
                icon = Icons.Rounded.Storage,
                onClick = { onNavigateToSub(SettingsSubSection.STORAGE) },
                showDivider = true
            )
            SettingsItemRow(
                title = stringResource(R.string.setting_section_backup),
                subtitle = stringResource(R.string.setting_section_backup_subtitle),
                icon = Icons.Rounded.CloudUpload,
                onClick = { onNavigateToSub(SettingsSubSection.BACKUP) },
                showDivider = false
            )
        }

        // Connections group
        SettingsGroupCard(title = stringResource(R.string.setting_group_more)) {
            SettingsItemRow(
                title = stringResource(R.string.setting_section_links),
                subtitle = stringResource(R.string.setting_section_links_subtitle),
                icon = Icons.Rounded.Link,
                onClick = { onNavigateToSub(SettingsSubSection.LINKS) },
                showDivider = true
            )
            SettingsItemRow(
                title = stringResource(R.string.setting_section_about),
                subtitle = stringResource(R.string.setting_section_about_subtitle),
                icon = Icons.Rounded.Info,
                onClick = { onNavigateToSub(SettingsSubSection.ABOUT) },
                showDivider = false
            )
        }

        Spacer(modifier = Modifier.height(if (isPlayerActive) 110.dp else 40.dp))
    }
}

// General subscreen layout
@Composable
private fun GeneralSubScreen(isPlayerActive: Boolean) {
    val internalScrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(internalScrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SettingsGroupCard(title = stringResource(R.string.setting_subgroup_localization)) {
            SettingsInteractiveRow(title = stringResource(R.string.setting_app_language), subtitle = stringResource(R.string.setting_app_language_default), icon = Icons.Rounded.Language, showDivider = false)
        }
        SettingsGroupCard(title = stringResource(R.string.setting_subgroup_system)) {
            SettingsToggleRow(title = stringResource(R.string.setting_keep_screen_on), subtitle = stringResource(R.string.setting_keep_screen_on_desc), icon = Icons.Rounded.LightMode, showDivider = true)
            SettingsToggleRow(title = stringResource(R.string.setting_stop_on_recents_remove), subtitle = stringResource(R.string.setting_stop_on_recents_remove_desc), icon = Icons.Rounded.ClearAll, showDivider = true)
            SettingsToggleRow(title = stringResource(R.string.setting_resume_on_bluetooth), subtitle = stringResource(R.string.setting_resume_on_bluetooth_desc), icon = Icons.Rounded.Bluetooth, showDivider = false)
        }
        Spacer(modifier = Modifier.height(if (isPlayerActive) 110.dp else 16.dp))
    }
}

// Appearance subscreen layout
@Composable
private fun AppearanceSubScreen(isPlayerActive: Boolean) {
    val internalScrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(internalScrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SettingsGroupCard(title = stringResource(R.string.setting_subgroup_theme)) {
            SettingsInteractiveRow(title = stringResource(R.string.setting_theme), subtitle = stringResource(R.string.setting_theme_dark_amoled), icon = Icons.Rounded.DarkMode, showDivider = false)
        }
        SettingsGroupCard(title = stringResource(R.string.setting_subgroup_navigation)) {
            SettingsInteractiveRow(title = stringResource(R.string.setting_default_tab), subtitle = stringResource(R.string.setting_default_tab_home), icon = Icons.Rounded.Home, showDivider = false)
        }
        Spacer(modifier = Modifier.height(if (isPlayerActive) 110.dp else 16.dp))
    }
}

// Player subscreen layout
@Composable
private fun PlayerSubScreen(queueLimit: Int, onQueueLimitChange: (Int) -> Unit, isPlayerActive: Boolean) {
    val internalScrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(internalScrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SettingsGroupCard(title = stringResource(R.string.setting_subgroup_audio_engine)) {
            SettingsInteractiveRow(title = stringResource(R.string.setting_audio_quality), subtitle = stringResource(R.string.setting_audio_quality_high), icon = Icons.Rounded.HighQuality, showDivider = true)
            SettingsToggleRow(title = stringResource(R.string.setting_audio_fade), subtitle = stringResource(R.string.setting_audio_fade_desc), icon = Icons.Rounded.Waves, showDivider = true)
            SettingsInteractiveRow(title = stringResource(R.string.setting_audio_fade_duration), subtitle = stringResource(R.string.setting_audio_fade_duration_default), icon = Icons.Rounded.Timer, showDivider = true)
            SettingsToggleRow(title = stringResource(R.string.setting_audio_normalization), subtitle = stringResource(R.string.setting_audio_normalization_desc), icon = Icons.Rounded.Equalizer, showDivider = false)
        }
        SettingsGroupCard(title = stringResource(R.string.setting_subgroup_silence)) {
            SettingsToggleRow(title = stringResource(R.string.setting_skip_silence), subtitle = stringResource(R.string.setting_skip_silence_desc), icon = Icons.AutoMirrored.Rounded.VolumeMute, showDivider = true)
            SettingsToggleRow(title = stringResource(R.string.setting_auto_skip_silence), subtitle = stringResource(R.string.setting_auto_skip_silence_desc), icon = Icons.Rounded.AutoMode, showDivider = true)
            SettingsToggleRow(title = stringResource(R.string.setting_pause_on_muted), subtitle = stringResource(R.string.setting_pause_on_muted_desc), icon = Icons.AutoMirrored.Rounded.VolumeOff, showDivider = false)
        }
        SettingsGroupCard(title = stringResource(R.string.setting_subgroup_queue)) {
            SettingsInteractiveRow(
                title = stringResource(R.string.setting_quick_choices),
                subtitle = "${stringResource(R.string.setting_quick_choices_desc)} $queueLimit",
                icon = Icons.Rounded.FlashOn,
                showDivider = false,
                onClick = { onQueueLimitChange(queueLimit + 25) }
            )
        }
        SettingsGroupCard(title = stringResource(R.string.setting_subgroup_utility)) {
            SettingsToggleRow(title = stringResource(R.string.setting_sleep_timer), subtitle = stringResource(R.string.setting_sleep_timer_desc), icon = Icons.Rounded.Snooze, showDivider = false)
        }
        Spacer(modifier = Modifier.height(if (isPlayerActive) 110.dp else 16.dp))
    }
}

// Content filters subscreen layout
@Composable
private fun ContentSubScreen(isPlayerActive: Boolean) {
    val internalScrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(internalScrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SettingsGroupCard(title = stringResource(R.string.setting_subgroup_filters)) {
            SettingsInteractiveRow(title = stringResource(R.string.setting_content_language), subtitle = stringResource(R.string.setting_content_language_global), icon = Icons.Rounded.Translate, showDivider = true)
            SettingsInteractiveRow(title = stringResource(R.string.setting_content_country), subtitle = stringResource(R.string.setting_content_country_br), icon = Icons.Rounded.Place, showDivider = false)
        }
        Spacer(modifier = Modifier.height(if (isPlayerActive) 110.dp else 16.dp))
    }
}

// Privacy subscreen layout
@Composable
private fun PrivacySubScreen(
    onClearPlaybackHistory: () -> Unit,
    onClearSearchHistory: () -> Unit,
    isPlayerActive: Boolean
) {
    val internalScrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(internalScrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SettingsGroupCard(title = stringResource(R.string.setting_subgroup_playback_history)) {
            SettingsToggleRow(title = stringResource(R.string.setting_pause_playback_history), subtitle = stringResource(R.string.setting_pause_playback_history_desc), icon = Icons.Rounded.HistoryToggleOff, showDivider = true)
            SettingsActionRow(title = stringResource(R.string.setting_clear_playback_history), subtitle = stringResource(R.string.setting_clear_playback_history_desc), icon = Icons.Rounded.DeleteSweep, showDivider = false, onClick = onClearPlaybackHistory)
        }
        SettingsGroupCard(title = stringResource(R.string.setting_subgroup_search_history)) {
            SettingsToggleRow(title = stringResource(R.string.setting_pause_search_history), subtitle = stringResource(R.string.setting_pause_search_history_desc), icon = Icons.AutoMirrored.Rounded.ManageSearch, showDivider = true)
            SettingsActionRow(title = stringResource(R.string.setting_clear_search_history), subtitle = stringResource(R.string.setting_clear_search_history_desc), icon = Icons.Rounded.HistoryEdu, showDivider = false, onClick = onClearSearchHistory)
        }
        SettingsGroupCard(title = stringResource(R.string.setting_subgroup_security)) {
            SettingsToggleRow(title = stringResource(R.string.setting_disable_screenshots), subtitle = stringResource(R.string.setting_disable_screenshots_desc), icon = Icons.Rounded.NoPhotography, showDivider = false)
        }
        Spacer(modifier = Modifier.height(if (isPlayerActive) 110.dp else 16.dp))
    }
}

// Storage info subscreen layout
@Composable
private fun StorageSubScreen(isPlayerActive: Boolean) {
    val internalScrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(internalScrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SettingsGroupCard(title = stringResource(R.string.setting_section_storage)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = stringResource(R.string.setting_storage_coming_soon), fontFamily = Poppins, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(modifier = Modifier.height(if (isPlayerActive) 110.dp else 16.dp))
    }
}

// Backup subscreen layout
@Composable
private fun BackupSubScreen(isPlayerActive: Boolean) {
    val internalScrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(internalScrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SettingsGroupCard(title = stringResource(R.string.setting_subgroup_backup_management)) {
            SettingsActionRow(title = stringResource(R.string.setting_backup_create), subtitle = stringResource(R.string.setting_backup_create_desc), icon = Icons.Rounded.Backup, showDivider = true, onClick = {})
            SettingsActionRow(title = stringResource(R.string.setting_backup_restore), subtitle = stringResource(R.string.setting_backup_restore_desc), icon = Icons.Rounded.RestorePage, showDivider = false, onClick = {})
        }
        SettingsGroupCard(title = stringResource(R.string.setting_subgroup_external_import)) {
            SettingsActionRow(title = stringResource(R.string.setting_import_playlist), subtitle = stringResource(R.string.setting_import_playlist_desc), icon = Icons.Rounded.FileDownload, showDivider = false, onClick = {})
        }
        Spacer(modifier = Modifier.height(if (isPlayerActive) 110.dp else 16.dp))
    }
}

// Links subscreen layout
@Composable
private fun LinksSubScreen(isPlayerActive: Boolean) {
    val internalScrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(internalScrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SettingsGroupCard(title = stringResource(R.string.setting_section_links)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = stringResource(R.string.setting_links_supported_desc), fontFamily = Poppins, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(modifier = Modifier.height(if (isPlayerActive) 110.dp else 16.dp))
    }
}

// About subscreen layout
@Composable
private fun AboutSubScreen(isPlayerActive: Boolean) {
    val internalScrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(internalScrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SettingsGroupCard(title = stringResource(R.string.setting_section_about)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = stringResource(R.string.setting_about_credits), fontFamily = Poppins, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(modifier = Modifier.height(if (isPlayerActive) 110.dp else 16.dp))
    }
}

// Core container card
@Composable
fun SettingsGroupCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontFamily = Poppins,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            ),
            content = content
        )
    }
}

// Standard row item
@Composable
fun SettingsItemRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    showDivider: Boolean,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 76.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(primaryColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = primaryColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 15.sp), color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall.copy(fontFamily = Poppins, fontSize = 12.sp, lineHeight = 16.sp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
        }
        if (showDivider) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), thickness = 1.dp)
        }
    }
}

// Toggle row implementation
@Composable
private fun SettingsToggleRow(title: String, subtitle: String, icon: ImageVector, showDivider: Boolean) {
    var checked by remember { mutableStateOf(false) }
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 76.dp)
                .clickable { checked = !checked }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(primaryColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = primaryColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(text = title, fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, fontFamily = Poppins, fontSize = 12.sp, lineHeight = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(checked = checked, onCheckedChange = { checked = it })
        }
        if (showDivider) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), thickness = 1.dp)
        }
    }
}

// Interactive value selector row
@Composable
private fun SettingsInteractiveRow(title: String, subtitle: String, icon: ImageVector, showDivider: Boolean, onClick: () -> Unit = {}) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 76.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(primaryColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = primaryColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(text = title, fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, fontFamily = Poppins, fontSize = 12.sp, color = primaryColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
        }
        if (showDivider) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), thickness = 1.dp)
        }
    }
}

// Action button destructive row
@Composable
private fun SettingsActionRow(title: String, subtitle: String, icon: ImageVector, showDivider: Boolean, onClick: () -> Unit) {
    val errorColor = MaterialTheme.colorScheme.error

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 76.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(errorColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = errorColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(text = title, fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, fontFamily = Poppins, fontSize = 12.sp, lineHeight = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
            }
        }
        if (showDivider) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), thickness = 1.dp)
        }
    }
}
