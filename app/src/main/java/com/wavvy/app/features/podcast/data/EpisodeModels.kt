package com.wavvy.app.features.podcast.data

// A piece of the description of an episode, the ones that link somewhere are written in white, and the ones with an address open it
data class EpisodeTextPart(
    val text: String,
    val isLink: Boolean,
    val url: String?
)

// The page of an episode, who makes it, how it is told and how much of it was heard
data class EpisodePage(
    // The video that plays the episode, and the podcast it belongs to
    val videoId: String,
    val title: String,
    val coverUrl: String?,
    val showName: String?,
    val showId: String?,
    // The views and the age as YouTube Music writes them, such as 293 mil visualizações and há 2 dias
    val views: String?,
    val age: String?,
    // The length as YouTube Music writes it, such as 1 h 56 min
    val durationText: String?,
    val progressPercent: Int,
    val description: List<EpisodeTextPart>
)
