package com.wavvy.app.features.notifications.ui

// Android application, text and view model
import android.app.Application
import android.content.Context
import android.text.format.DateUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
// Coroutines and reactive flows
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.core.history.PlayHistory
import com.wavvy.app.core.history.ReleaseEntity
import com.wavvy.app.features.notifications.data.NotificationItem
import com.wavvy.app.features.notifications.data.NotificationLink
import com.wavvy.app.features.notifications.data.NotificationText
import com.wavvy.app.features.notifications.data.sentenceRes

// Marks that stand for the artist and the title in the sentence of a notification, so they can be written in bold
private const val ArtistMark = "\u0001"
private const val TitleMark = "\u0002"
private val MarkPattern = Regex("[\u0001\u0002]")

// The news about the artists the user follows, read from the history of the device, and what can be done with them
class NotificationsViewModel(application: Application) : AndroidViewModel(application) {
    // Empty until the first answer of the database, so the screen does not flash a message that is not true
    val items: StateFlow<List<NotificationItem>?> = PlayHistory.announcedReleases(application)
        .map { releases -> releases.map { it.toNotification(application) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // The news were seen, the badge of the bell goes away
    fun markAllRead() {
        viewModelScope.launch { PlayHistory.markReleasesRead(getApplication()) }
    }

    fun delete(item: NotificationItem) {
        viewModelScope.launch { PlayHistory.dismissRelease(getApplication(), item.id) }
    }
}

// A release as the notification the Activity shows, with the artist and the title in bold
private fun ReleaseEntity.toNotification(context: Context): NotificationItem {
    val sentence = context.getString(
        sentenceRes(),
        ArtistMark,
        TitleMark
    )

    val message = mutableListOf<NotificationText>()
    var cursor = 0
    MarkPattern.findAll(sentence).forEach { mark ->
        if (mark.range.first > cursor) message += NotificationText(sentence.substring(cursor, mark.range.first), isBold = false)
        message += NotificationText(if (mark.value == ArtistMark) artistName else title, isBold = true)
        cursor = mark.range.last + 1
    }
    if (cursor < sentence.length) message += NotificationText(sentence.substring(cursor), isBold = false)

    return NotificationItem(
        id = id,
        avatarUrl = artistPhoto,
        message = message,
        timeText = DateUtils.getRelativeTimeSpanString(seenAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString(),
        coverUrl = coverUrl,
        link = NotificationLink(browseId = id, videoId = null),
        isNew = !isRead
    )
}
