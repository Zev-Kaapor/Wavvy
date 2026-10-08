package com.wavvy.app.features.notifications.data

// A piece of the message of a notification, the names in it come in bold
data class NotificationText(
    val text: String,
    val isBold: Boolean
)

// Where a notification leads, a page by its id or a video to play
data class NotificationLink(
    val browseId: String?,
    val videoId: String?
)

// One notification, such as an artist that released something
data class NotificationItem(
    val id: String,
    val avatarUrl: String?,
    val message: List<NotificationText>,
    val timeText: String?,
    val coverUrl: String?,
    val link: NotificationLink?,
    // Not opened yet, which puts it under the new ones
    val isNew: Boolean
) {
    // The message as plain text
    val plainText: String get() = message.joinToString("") { it.text }
}
