package com.wavvy.app.features.settings.ui

// Android file sizes and widgets
import android.text.format.Formatter
import android.widget.Toast
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
// Coroutines and image loading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import coil3.SingletonImageLoader
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.ConfirmDialog
import com.wavvy.app.core.download.DownloadArt
import com.wavvy.app.core.download.DownloadPhase
import com.wavvy.app.core.download.Downloads
import com.wavvy.app.core.innertube.OfflineCache
import com.wavvy.app.features.settings.ui.components.SettingsActionRow
import com.wavvy.app.features.settings.ui.components.SettingsDivider
import com.wavvy.app.features.settings.ui.components.SettingsGroup
import com.wavvy.app.features.settings.ui.components.SettingsInfoRow
import com.wavvy.app.features.settings.ui.components.SettingsPage
import com.wavvy.app.core.designsystem.components.Toasts

// The storage, how much the downloads, the saved pages and the pictures take, and the ways to free it
@Composable
fun StorageSettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val downloads by Downloads.items.collectAsState()
    var isRemovingDownloads by remember { mutableStateOf(false) }

    // The sizes are read again whenever something is freed, they are read away from the screen because they walk through files
    var refresh by remember { mutableIntStateOf(0) }
    var pagesBytes by remember { mutableStateOf(0L) }
    var picturesBytes by remember { mutableStateOf(0L) }
    var cacheBytes by remember { mutableStateOf(0L) }
    LaunchedEffect(refresh, downloads.size) {
        withContext(Dispatchers.IO) {
            pagesBytes = OfflineCache.sizeBytes()
            picturesBytes = DownloadArt.sizeBytes(context)
            cacheBytes = SingletonImageLoader.get(context).diskCache?.size ?: 0L
        }
    }
    val downloadedBytes = downloads.values.filter { it.phase == DownloadPhase.Completed }.sumOf { it.bytes }
    fun size(bytes: Long) = Formatter.formatShortFileSize(context, bytes)

    if (isRemovingDownloads) {
        ConfirmDialog(
            title = stringResource(R.string.settings_storage_remove_downloads_title),
            text = stringResource(R.string.settings_storage_remove_downloads_text),
            confirmLabel = stringResource(R.string.download_remove_confirm),
            cancelLabel = stringResource(R.string.playlist_cancel),
            onConfirm = {
                isRemovingDownloads = false
                Downloads.removeAll(context, downloads.keys.toList())
            },
            onDismiss = { isRemovingDownloads = false }
        )
    }

    SettingsPage(title = stringResource(R.string.settings_storage), onBack = onBack, modifier = modifier) {
        SettingsGroup(title = stringResource(R.string.settings_group_in_use)) {
            SettingsInfoRow(title = stringResource(R.string.settings_storage_downloads), value = size(downloadedBytes))
            SettingsDivider()
            SettingsInfoRow(title = stringResource(R.string.settings_storage_pictures), value = size(picturesBytes))
            SettingsDivider()
            SettingsInfoRow(title = stringResource(R.string.settings_storage_pages), value = size(pagesBytes))
            SettingsDivider()
            SettingsInfoRow(title = stringResource(R.string.settings_storage_cache), value = size(cacheBytes))
        }

        SettingsGroup(title = stringResource(R.string.settings_group_clean)) {
            SettingsActionRow(
                title = stringResource(R.string.settings_storage_clear_pages),
                subtitle = stringResource(R.string.settings_storage_clear_pages_subtitle),
                onClick = {
                    OfflineCache.clear()
                    refresh++
                    Toasts.show(resources.getString(R.string.settings_storage_cleared))
                }
            )
            SettingsDivider()
            SettingsActionRow(
                title = stringResource(R.string.settings_storage_clear_cache),
                subtitle = stringResource(R.string.settings_storage_clear_cache_subtitle),
                onClick = {
                    scope.launch(Dispatchers.IO) { SingletonImageLoader.get(context).diskCache?.clear() }
                    refresh++
                    Toasts.show(resources.getString(R.string.settings_storage_cleared))
                }
            )
            SettingsDivider()
            SettingsActionRow(
                title = stringResource(R.string.settings_storage_remove_downloads),
                subtitle = stringResource(R.string.settings_storage_remove_downloads_subtitle),
                onClick = { isRemovingDownloads = true }
            )
        }
    }
}
