package com.wavvy.app.features.podcast.data

// Project resources
import com.wavvy.app.features.home.data.HomeItem

// One way to order the episodes, the one in use has a check
data class PodcastSort(
    val title: String,
    val token: String,
    val isSelected: Boolean
)

// A button of the row under the buttons of a podcast, a filter that has the token that asks the episodes under it
// The first one is the order of the episodes and has the ways to order them instead of a token
data class PodcastChip(
    val title: String,
    val token: String?,
    val sorts: List<PodcastSort>
)

// A list of episodes, the token that asks the next ones and the message YouTube Music gives when there are none
data class PodcastEpisodes(
    val episodes: List<HomeItem>,
    val continuation: String?,
    val message: String?
)

// The page of a podcast, who makes it, how it is told, the buttons and the first episodes
data class PodcastPage(
    val title: String,
    val author: String?,
    val authorPhoto: String?,
    val coverUrl: String?,
    val description: String?,
    // The words of the button that saves the podcast, in the language of the request
    val saveLabel: String?,
    val chips: List<PodcastChip>,
    val episodes: PodcastEpisodes
)
