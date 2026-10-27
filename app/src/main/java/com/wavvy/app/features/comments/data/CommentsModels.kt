package com.wavvy.app.features.comments.data

// One comment of an album, or one reply to a comment
data class Comment(
    val id: String,
    val author: String,
    val authorPhotoUrl: String?,
    // The channel of who wrote it, which opens their page, and true when it is the channel of an artist
    val authorChannelId: String?,
    val isArtist: Boolean,
    val text: String,
    val publishedTime: String,
    // How many liked it as YouTube writes it, empty when nobody did
    val likeCount: String,
    // How many replies it has as YouTube writes them, and what asks them, none when it has none
    val repliesText: String?,
    val replies: CommentReplies?,
    val isReply: Boolean,
    // The artist of the album and the channels YouTube checked are marked
    val isCreator: Boolean,
    val isVerified: Boolean,
    // True when the artist gave it a heart, with the words YouTube says about it
    val isHearted: Boolean = false,
    val heartText: String? = null
)

// What asks the replies of a comment
data class CommentReplies(val feedId: String, val params: String)

// A page of comments and the token of the next one
data class CommentPage(
    val comments: List<Comment>,
    val continuation: String?
)

// The orders of the comments as YouTube names them, the ones it picks first and the newest
enum class CommentSort(val value: String) {
    Top("SORT_BY_BEST"),
    Newest("SORT_BY_NEWEST")
}
