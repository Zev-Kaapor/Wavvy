package com.wavvy.app.features.player.ui.components

// Compose and the resources of the app
import android.content.Context
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.OverlaySheet
import com.wavvy.app.core.designsystem.components.Toasts
import com.wavvy.app.core.playback.SleepTimer
import com.wavvy.app.features.playlist.ui.SortChoice
import com.wavvy.app.features.playlist.ui.SortChoiceSheet

// The times that can be chosen, in minutes
private val SleepMinutes = listOf(5, 10, 15, 30, 45, 60)

// The sleep timer asked from the menu of the song that plays, a choice of times and of the end of the song, and a way to turn it off when it is running
object SleepTimerSheet {
    fun show(context: Context) {
        val resources = context.resources
        val isActive = SleepTimer.state.value.isActive

        val choices = buildList {
            if (isActive) {
                add(SortChoice(resources.getString(R.string.sleep_off), false) { SleepTimer.cancel() })
            }
            SleepMinutes.forEach { minutes ->
                add(SortChoice(resources.getString(R.string.sleep_minutes, minutes), false) {
                    SleepTimer.startIn(minutes)
                    Toasts.show(resources.getString(R.string.sleep_set, minutes))
                })
            }
            add(SortChoice(resources.getString(R.string.sleep_end_of_song), false) {
                SleepTimer.startAtEndOfSong()
                Toasts.show(resources.getString(R.string.sleep_set_end))
            })
        }

        OverlaySheet.show { SortChoiceSheet(title = resources.getString(R.string.menu_sleep_timer), choices = choices, onDismiss = OverlaySheet::dismiss) }
    }
}
