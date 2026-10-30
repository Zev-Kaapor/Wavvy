package com.wavvy.app.core.playback

// What the button that goes back does, it starts the song again as many times as the user chose before the next tap goes to the one that came before
class PreviousRule {
    // How many taps start the song again before the next one goes back, none goes back at once
    @Volatile
    var restarts: Int = 0

    private var taps = 0
    private var lastItem: String? = null

    // True when this tap starts the song again and false when it goes back
    // A tap counts after the one before it only while the song has not played far since, so a song that was listened to starts again from the first tap
    fun restartsNow(itemId: String?, positionMs: Long): Boolean {
        if (itemId != lastItem || positionMs > ConsecutiveMillis) taps = 0
        lastItem = itemId

        if (taps >= restarts) {
            taps = 0
            return false
        }
        taps++
        return true
    }

    private companion object {
        // How far into the song the next tap still comes right after the last one
        const val ConsecutiveMillis = 3000L
    }
}
