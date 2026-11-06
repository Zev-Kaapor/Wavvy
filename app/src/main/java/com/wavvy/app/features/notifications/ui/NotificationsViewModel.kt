package com.wavvy.app.features.notifications.ui

// Android application, text and view model
import android.app.Application
import android.content.Context
import android.text.format.DateUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
// Coroutines and reactive flows
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.history.PlayHistory
import com.wavvy.app.core.history.ReleaseEntity
import com.wavvy.app.features.notifications.data.NotificationItem
import com.wavvy.app.features.notifications.data.NotificationLink
import com.wavvy.app.features.notifications.data.NotificationText
import com.wavvy.app.features.notifications.data.sentenceRes
import com.wavvy.app.features.recap.data.RecapNotice
import com.wavvy.app.features.settings.data.SettingsStore

// Marks that stand for the artist and the title in the sentence of a notification, so they can be written in bold
private const val ArtistMark = "\u0001"
private const val TitleMark = "\u0002"
private val MarkPattern = Regex("[\u0001\u0002]")

// Start of the id of the notification of the retrospective, which is followed by its year
private const val RecapIdPrefix = "recap_"

// The news about the artists the user follows, read from the history of the device, and what can be done with them
class NotificationsViewModel(application: Application) : AndroidViewModel(application) {
    private val store = SettingsStore(application)

    // Empty until the first answer of the database, so the screen does not flash a message that is not true, the retrospective of the year comes first
    val items: StateFlow<List<NotificationItem>?> = combine(PlayHistory.announcedReleases(application), RecapNotice.notice(store)) { releases, recap ->
        listOfNotNull(recap?.toNotification(application)) + releases.map { it.toNotification(application) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // The news were seen, the badge of the bell goes away
    fun markAllRead() {
        viewModelScope.launch {
            PlayHistory.markReleasesRead(getApplication())
            RecapNotice.markRead(getApplication())
        }
    }

    fun delete(item: NotificationItem) {
        viewModelScope.launch {
            if (item.link?.isRecap == true) RecapNotice.dismiss(getApplication(), item.id.removePrefix(RecapIdPrefix).toInt()) else PlayHistory.dismissRelease(getApplication(), item.id)
        }
    }
}

// The retrospective of the year as a notification of the Activity, with its title in bold, it leads to the retrospective and has no photo or cover
private fun RecapNotice.Notice.toNotification(context: Context): NotificationItem =
    NotificationItem(
        id = RecapIdPrefix + year,
        avatarUrl = null,
        message = listOf(
            NotificationText(context.getString(R.string.recap_notice_title, year), isBold = true),
            NotificationText(" " + context.getString(R.string.recap_notice_text), isBold = false)
        ),
        timeText = DateUtils.getRelativeTimeSpanString(at, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString(),
        coverUrl = null,
        link = NotificationLink(browseId = null, videoId = null, isRecap = true),
        isNew = !isRead
    )

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
