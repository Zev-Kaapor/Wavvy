package com.wavvy.app.features.search.data

// Project resources
import com.wavvy.app.features.home.data.HomeItem

// What a search can be narrowed to in the order of the chips, the parameters are the ones the web player of YouTube Music sends for each filter
enum class SearchCategory(val params: String?) {
    All(null),
    Songs("EgWKAQIIAWoKEAkQBRAKEAMQBA=="),
    Videos("EgWKAQIQAWoKEAkQChAFEAMQBA=="),
    Albums("EgWKAQIYAWoKEAkQChAFEAMQBA=="),
    Artists("EgWKAQIgAWoKEAkQChAFEAMQBA=="),
    CommunityPlaylists("EgeKAQQoAEABagoQAxAEEAoQCRAF"),
    FeaturedPlaylists("EgeKAQQoADgBagwQDhAKEAMQBRAJEAQ="),
    Podcasts("EgWKAQJQAWoKEAkQChAFEAMQBA=="),
    // The filter of YouTube Music brings episodes in another format, so they come from the search of everything
    Episodes(null),
    Profiles("EgWKAQJYAWoSEAUQCRADEAQQEBAVEAoQDhAR")
}

// A page of results, the best match comes only in the search of everything and the continuation asks for the next page of a filter
data class SearchPage(
    val topMatch: HomeItem?,
    val items: List<HomeItem>,
    val continuation: String?
)

// What the account keeps, the latest searches in the order of the account and the older ones found by their first letter
data class AccountHistory(
    val recent: List<SearchSuggestion.Words>,
    val older: List<SearchSuggestion.Words>
)

// What appears under the field while it is typed, words to complete it and songs or artists that match it
sealed interface SearchSuggestion {
    // A search made before comes with the token that removes it from the history of the account
    data class Words(val text: String, val feedbackToken: String? = null, val isHistory: Boolean = false) : SearchSuggestion
    data class Match(val item: HomeItem) : SearchSuggestion
}
