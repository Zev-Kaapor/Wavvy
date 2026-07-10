package com.lonewolf.wavvy.ui.settings

// Compose layouts and foundations
import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
// Material 3 and icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.ShortText
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
// UI styling and windowing
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.theme.Poppins
import com.lonewolf.wavvy.ui.theme.accentCyan

// Settings sections enumeration mapping icons and localized string titles
enum class SettingsTab(val icon: ImageVector, val titleRes: Int) {
    GENERAL(Icons.Default.Tune, R.string.setting_section_general),
    APPEARANCE(Icons.Default.Palette, R.string.setting_section_appearance),
    PLAYER(Icons.Default.MusicNote, R.string.setting_section_player),
    CONTENT(Icons.AutoMirrored.Filled.ShortText, R.string.setting_section_content),
    PRIVACY(Icons.Default.PrivacyTip, R.string.setting_section_privacy),
    STORAGE(Icons.Default.SdCard, R.string.setting_section_storage),
    BACKUP(Icons.Default.Backup, R.string.setting_section_backup),
    LINKS(Icons.AutoMirrored.Filled.OpenInNew, R.string.setting_section_links),
    ABOUT(Icons.Default.Info, R.string.menu_welcome)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    queueLimit: Int,
    onQueueLimitChange: (Int) -> Unit,
    onClearLocalHistory: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var currentTab by remember { mutableStateOf(SettingsTab.GENERAL) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(currentTab.titleRes),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = Poppins,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            if (!isLandscape) {
                SettingsBottomTabs(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLandscape) {
                SettingsNavigationRail(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                when (currentTab) {
                    SettingsTab.GENERAL -> {
                        GeneralSettingsContent(
                            queueLimit = queueLimit,
                            onQueueLimitChange = onQueueLimitChange,
                            onClearLocalHistory = onClearLocalHistory
                        )
                    }
                    SettingsTab.APPEARANCE -> {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.setting_section_appearance), style = MaterialTheme.typography.bodyLarge.copy(fontFamily = Poppins))
                        }
                    }
                    SettingsTab.PLAYER -> {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.setting_section_player), style = MaterialTheme.typography.bodyLarge.copy(fontFamily = Poppins))
                        }
                    }
                    SettingsTab.CONTENT -> {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.setting_section_content), style = MaterialTheme.typography.bodyLarge.copy(fontFamily = Poppins))
                        }
                    }
                    SettingsTab.PRIVACY -> {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.setting_section_privacy), style = MaterialTheme.typography.bodyLarge.copy(fontFamily = Poppins))
                        }
                    }
                    SettingsTab.STORAGE -> {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.setting_section_storage), style = MaterialTheme.typography.bodyLarge.copy(fontFamily = Poppins))
                        }
                    }
                    SettingsTab.BACKUP -> {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.setting_section_backup), style = MaterialTheme.typography.bodyLarge.copy(fontFamily = Poppins))
                        }
                    }
                    SettingsTab.LINKS -> {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.setting_section_links), style = MaterialTheme.typography.bodyLarge.copy(fontFamily = Poppins))
                        }
                    }
                    SettingsTab.ABOUT -> {
                        AboutSettingsPlaceholder()
                    }
                }
            }
        }
    }
}

// General preferences controls layout
@Composable
private fun GeneralSettingsContent(
    queueLimit: Int,
    onQueueLimitChange: (Int) -> Unit,
    onClearLocalHistory: () -> Unit
) {
    var sliderValue by remember(queueLimit) { mutableFloatStateOf(queueLimit.toFloat()) }

    Text(
        text = stringResource(R.string.queue_title),
        style = MaterialTheme.typography.labelLarge.copy(
            fontFamily = Poppins,
            fontWeight = FontWeight.Bold,
            color = if (isSystemInDarkTheme()) MaterialTheme.accentCyan else MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier.padding(vertical = 8.bindDp())
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.setting_queue_size_label),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = Poppins,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    text = sliderValue.toInt().toString(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Bold,
                        color = if (isSystemInDarkTheme()) MaterialTheme.accentCyan else MaterialTheme.colorScheme.primary
                    )
                )
            }

            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { onQueueLimitChange(sliderValue.toInt()) },
                valueRange = 10f..100f,
                steps = 9,
                colors = SliderDefaults.colors(
                    thumbColor = if (isSystemInDarkTheme()) MaterialTheme.accentCyan else MaterialTheme.colorScheme.primary,
                    activeTrackColor = if (isSystemInDarkTheme()) MaterialTheme.accentCyan else MaterialTheme.colorScheme.primary
                )
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = stringResource(R.string.setting_section_data_privacy),
        style = MaterialTheme.typography.labelLarge.copy(
            fontFamily = Poppins,
            fontWeight = FontWeight.Bold,
            color = if (isSystemInDarkTheme()) MaterialTheme.accentCyan else MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier.padding(vertical = 8.bindDp())
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClearLocalHistory)
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DeleteForever,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = stringResource(R.string.setting_clear_history_title),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = Poppins,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    text = stringResource(R.string.setting_clear_history_subtitle),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = Poppins,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

// About presentation view placeholder
@Composable
private fun AboutSettingsPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.menu_welcome),
            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = Poppins)
        )
    }
}

// Portable extension layer mapping dp values securely
private fun Int.bindDp() = this.dp

// Bottom integrated tabs structure inheriting bar dynamics
@Composable
private fun SettingsBottomTabs(
    currentTab: SettingsTab,
    onTabSelected: (SettingsTab) -> Unit
) {
    val scrollState = rememberScrollState()

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .horizontalScroll(scrollState),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsTab.entries.forEach { tab ->
                ScrollableSettingsTabItem(
                    tab = tab,
                    isSelected = currentTab == tab,
                    onClick = { onTabSelected(tab) }
                )
            }
        }

        // Small thin progress bar
        val indicatorWidth by animateFloatAsState(
            targetValue = if (scrollState.maxValue > 0) 0.3f else 1f, label = "indicator_width"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth(indicatorWidth)
                .height(2.dp)
                .padding(horizontal = 16.dp)
                .background(
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(1.dp)
                )
                .align(Alignment.Start)
        )
    }
}

// Side integrated tabs layout structure for orientation handling
@Composable
private fun SettingsNavigationRail(
    currentTab: SettingsTab,
    onTabSelected: (SettingsTab) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(64.dp)
            .background(Color.Transparent)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        SettingsTab.entries.forEach { tab ->
            ColumnSettingsTabItem(
                tab = tab,
                isSelected = currentTab == tab,
                onClick = { onTabSelected(tab) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

// Tab Item variant mapped for horizontal scrolling containers
@Composable
private fun ScrollableSettingsTabItem(
    tab: SettingsTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    SettingsTabContent(
        tab = tab,
        isSelected = isSelected,
        modifier = Modifier
            .fillMaxHeight()
            .width(56.dp),
        onClick = onClick
    )
}

// Tab Item variant strictly bound to ColumnScope for landscape view
@Composable
private fun ColumnSettingsTabItem(
    tab: SettingsTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    SettingsTabContent(
        tab = tab,
        isSelected = isSelected,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        onClick = onClick
    )
}

// Core Tab styling, click interaction, and animation handling logic
@Composable
private fun SettingsTabContent(
    tab: SettingsTab,
    isSelected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val targetColor = if (isSelected) {
        if (isDark) MaterialTheme.accentCyan else MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }

    val contentColor by animateColorAsState(targetValue = targetColor, label = "nav_color")

    // Slightly larger icon as requested
    val iconSize by animateDpAsState(
        targetValue = if (isSelected) 28.dp else 24.dp,
        animationSpec = tween(200),
        label = "nav_size"
    )

    Box(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(iconSize)
        )
    }
}
