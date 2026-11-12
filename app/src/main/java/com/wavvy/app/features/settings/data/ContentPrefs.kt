package com.wavvy.app.features.settings.data

// Android context
import android.content.Context
// Coroutines
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
// Project resources
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.data.HomeItemKind
import com.wavvy.app.features.home.data.PlaylistPagePrefix

// The ids of the lists YouTube Music makes by itself, the liked songs, the episodes for later and watch later, and the start of the ids of the Shorts
private const val LikedId = "LM"
private const val EpisodesLaterId = "SE"
private const val WatchLaterId = "WL"
private const val ShortsPrefix = "SS"

// The countries YouTube Music answers for, it refuses any other code with an error and then nothing loads
val ContentCountries = setOf(
    "AE", "AM", "AR", "AT", "AU", "AZ", "BA", "BD", "BE", "BG", "BH", "BO", "BR", "BY", "CA", "CH", "CL", "CO", "CR", "CY", "CZ", "DE", "DK", "DO", "DZ", "EC", "EE",
    "EG", "ES", "FI", "FR", "GB", "GE", "GH", "GR", "GT", "HK", "HN", "HR", "HU", "ID", "IE", "IL", "IN", "IQ", "IS", "IT", "JM", "JO", "JP", "KE", "KH", "KR", "KW",
    "KZ", "LA", "LB", "LI", "LK", "LT", "LU", "LV", "LY", "MA", "MD", "ME", "MK", "MT", "MX", "MY", "NG", "NI", "NL", "NO", "NP", "NZ", "OM", "PA", "PE", "PG", "PH",
    "PK", "PL", "PR", "PT", "PY", "QA", "RO", "RS", "RU", "SA", "SE", "SG", "SI", "SK", "SN", "SV", "TH", "TN", "TR", "TW", "TZ", "UA", "UG", "US", "UY", "VE", "VN",
    "YE", "ZA", "ZW"
)

// What the user chose for the content, read at once by the code that filters the lists, which cannot wait for the store
object ContentPrefs {
    // True when videos show in the Home and in the queue
    @Volatile
    var showVideos = false
        private set

    // True when explicit songs and albums stay out of the Home and the search
    @Volatile
    var hideExplicit = false
        private set

    // The two letters of the country of the content, none follows the device
    @Volatile
    var country: String? = null
        private set

    // The ids of the lists of the library the user does not want to see, the ones that YouTube Music makes by itself
    @Volatile
    private var hiddenLists: Set<String> = emptySet()

    // True when the playlists of Shorts, which have ids that start with SS, stay out of the library
    @Volatile
    private var hideShorts = false

    private val mutableChanges = MutableStateFlow(0)

    // Counts the changes of what shapes the content, the screens that keep lists ask again when it moves
    val changes: StateFlow<Int> = mutableChanges.asStateFlow()

    private var isFollowing = false

    // Takes the choices and tells when one that shapes the lists is not what it was
    private fun apply(settings: ContentSettings) {
        // A country that YouTube Music does not answer for, chosen before the list was cut down, is left out and the device country is used
        val chosenCountry = settings.country?.takeIf { it in ContentCountries }
        val isChanged = showVideos != settings.showVideos || hideExplicit != settings.hideExplicit || country != chosenCountry
        showVideos = settings.showVideos
        hideExplicit = settings.hideExplicit
        hideShorts = settings.hideShorts
        country = chosenCountry
        if (isChanged) mutableChanges.value += 1
    }

    // True when a list of the library is one that the user chose to hide
    fun isHiddenInLibrary(item: HomeItem): Boolean {
        if (item.kind != HomeItemKind.Playlist) return false

        val id = item.id.removePrefix(PlaylistPagePrefix)
        return id in hiddenLists || (hideShorts && id.startsWith(ShortsPrefix))
    }

    // Reads the choice once and then each time it changes, called when the app starts
    @Synchronized
    fun follow(context: Context) {
        if (isFollowing) return
        isFollowing = true

        val store = SettingsStore(context)

        // The first values are read before any screen asks for the content, so the start is not counted as a change
        apply(runBlocking { store.content.first() })
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            store.content.collect { apply(it) }
        }
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            store.library.collect {
                hiddenLists = buildSet {
                    if (it.hideLiked) add(LikedId)
                    if (it.hideEpisodes) add(EpisodesLaterId)
                    if (it.hideWatchLater) add(WatchLaterId)
                }
            }
        }
    }
}
