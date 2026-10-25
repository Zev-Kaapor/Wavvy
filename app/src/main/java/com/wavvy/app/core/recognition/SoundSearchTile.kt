package com.wavvy.app.core.recognition

// Android quick settings and intents
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.net.toUri
// Project resources
import com.wavvy.app.R

// The tile of the quick settings that searches a song by its sound, the microphone cannot be used from here, so it opens the screen that listens over the app that is open
class SoundSearchTile : TileService() {
    override fun onStartListening() {
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            label = getString(R.string.tile_sound_label)
            icon = Icon.createWithResource(this@SoundSearchTile, R.drawable.ic_tile_sound)
            updateTile()
        }
    }

    // The shade closes and the service listens over what is open, without pausing it
    // Without the permission to draw over the apps the settings of it open, and without the microphone a screen asks for it
    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        val intent = if (SoundSearchService.canShow(this)) {
            Intent(this, SoundSearchActivity::class.java)
        } else {
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri())
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
