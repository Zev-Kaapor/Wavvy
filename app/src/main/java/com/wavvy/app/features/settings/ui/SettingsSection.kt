package com.wavvy.app.features.settings.ui

// Android resources and icons
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.icons.WavvyIcons

// A part of the settings, with the words that name it, the line that says what is in it and its icon
enum class SettingsSection(
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val icon: ImageVector,
    // False while the screen of the section is not built, so its row only stands in its place
    val isReady: Boolean = false
) {
    Appearance(R.string.settings_appearance, R.string.settings_appearance_subtitle, WavvyIcons.Palette, isReady = true),
    Notifications(R.string.settings_notifications, R.string.settings_notifications_subtitle, WavvyIcons.Bell, isReady = true),
    Player(R.string.settings_player, R.string.settings_player_subtitle, WavvyIcons.MusicNote),
    Content(R.string.settings_content, R.string.settings_content_subtitle, WavvyIcons.Checklist, isReady = true),
    Downloads(R.string.settings_downloads, R.string.settings_downloads_subtitle, WavvyIcons.Download, isReady = true),
    Storage(R.string.settings_storage, R.string.settings_storage_subtitle, WavvyIcons.Storage, isReady = true),
    Privacy(R.string.settings_privacy, R.string.settings_privacy_subtitle, WavvyIcons.History, isReady = true),
    Backup(R.string.settings_backup, R.string.settings_backup_subtitle, WavvyIcons.Cloud),
    Links(R.string.settings_links, R.string.settings_links_subtitle, WavvyIcons.Link, isReady = true),
    About(R.string.settings_about, R.string.settings_about_subtitle, WavvyIcons.Info, isReady = true)
}

// The sections that sit together in a card under a name
enum class SettingsGroup(@StringRes val titleRes: Int, val sections: List<SettingsSection>) {
    App(R.string.settings_group_app, listOf(SettingsSection.Appearance, SettingsSection.Notifications)),
    Playback(R.string.settings_group_playback, listOf(SettingsSection.Player, SettingsSection.Content, SettingsSection.Downloads)),
    Data(R.string.settings_group_data, listOf(SettingsSection.Storage, SettingsSection.Privacy, SettingsSection.Backup)),
    More(R.string.settings_group_more, listOf(SettingsSection.Links, SettingsSection.About))
}
