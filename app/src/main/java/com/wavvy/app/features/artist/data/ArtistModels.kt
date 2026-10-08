package com.wavvy.app.features.artist.data

// Project resources
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.data.HomeSection

// The photo, the name and the subscribers of an artist, as the list of artists of a song shows them
data class ArtistSummary(
    val name: String,
    val photoUrl: String?,
    val subscribers: String?
)

// The subscription of the account to the artist, with the words YouTube Music uses for each state in the language of the request
data class ArtistSubscription(
    val channelId: String,
    val isSubscribed: Boolean,
    val countText: String?,
    val subscribedLabel: String?,
    val unsubscribedLabel: String?,
    val subscribeParams: String?
)

// What the newest release is, which tells the words of the card that promotes it
enum class ReleaseKind { Single, Ep, Album }

// The newest release of the artist, the card above the top songs
data class ArtistFeatured(
    val kind: ReleaseKind,
    val release: HomeItem
)

// Page of an artist, the shelves come in the order YouTube Music sends them, the top songs first
data class ArtistPage(
    val name: String,
    val bannerUrl: String?,
    // How many people listen to the artist in a month, as YouTube Music writes it
    val monthlyListeners: String?,
    val subscription: ArtistSubscription?,
    val featured: ArtistFeatured?,
    // The top songs, which are also the first shelf
    val topSongs: List<HomeItem>,
    val sections: List<HomeSection>,
    // How many views the artist has, and who they are
    val views: String?,
    val description: String?
)
