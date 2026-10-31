package com.wavvy.app.features.artist.ui

// Android application and view model
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
// Coroutines and reactive flows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.Toasts
import com.wavvy.app.core.innertube.MusicOrigin
import com.wavvy.app.core.lyrics.LyricsTranslator
import com.wavvy.app.core.navigation.ArtistIdArg
import com.wavvy.app.features.artist.data.ArtistPage
import com.wavvy.app.features.artist.data.ArtistProfile
import com.wavvy.app.features.artist.data.ArtistRepository
import com.wavvy.app.features.settings.data.SettingsStore

// What the page is doing, waiting for the answer, showing it or failed
enum class ArtistStatus { Loading, Content, Error }

// Everything the page of an artist draws
data class ArtistUiState(
    val status: ArtistStatus = ArtistStatus.Loading,
    val page: ArtistPage? = null,
    // The description in the language of the device, only when it was written in another one, and whether it is the one shown
    val translation: String? = null,
    val showTranslation: Boolean = false,
    // What MusicBrainz adds about the artist, which arrives a little after the page
    val profile: ArtistProfile? = null
)

// Loads the page of the artist the route points to, follows the subscription and translates the description
class ArtistViewModel(
    application: Application,
    savedState: SavedStateHandle
) : AndroidViewModel(application) {
    val id: String = requireNotNull(savedState[ArtistIdArg])
    private val repository = ArtistRepository(application)
    private val store = SettingsStore(application)

    // Whether the user is told about the releases of the artist, which is the case until the bell is turned off
    val isNotifying: StateFlow<Boolean> = store.mutedArtists.map { id !in it }.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val mutableState = MutableStateFlow(ArtistUiState())
    val state: StateFlow<ArtistUiState> = mutableState.asStateFlow()

    // Link of the page on YouTube Music, for sharing
    val shareUrl: String
        get() = "$MusicOrigin/channel/$id"

    init {
        load()
    }

    // Asks the page, again after a failure
    fun load() {
        mutableState.value = ArtistUiState(status = ArtistStatus.Loading)
        viewModelScope.launch {
            repository.load(id)
                .onSuccess { page ->
                    mutableState.value = ArtistUiState(status = ArtistStatus.Content, page = page)
                    page.description?.let(::prepareTranslation)
                    loadProfile(page)
                }
                .onFailure { mutableState.value = ArtistUiState(status = ArtistStatus.Error) }
        }
    }

    // Subscribes or takes the subscription away at once and asks the account, the old state comes back when the account refuses
    fun toggleSubscription() {
        val page = mutableState.value.page ?: return
        val subscription = page.subscription ?: return
        val subscribe = !subscription.isSubscribed

        mutableState.update { it.copy(page = page.copy(subscription = subscription.copy(isSubscribed = subscribe))) }
        viewModelScope.launch {
            // A new subscription tells about the releases from the start, and one that ends takes its choice with it
            store.setArtistNotifications(id, isOn = true)
            repository.setSubscribed(subscription, subscribe).onFailure {
                mutableState.update { current -> current.copy(page = current.page?.copy(subscription = subscription)) }
            }
        }
    }

    // Turns the notices of the releases of the artist on or off, and says which it is
    fun toggleNotifications() {
        val isOn = !isNotifying.value
        viewModelScope.launch {
            store.setArtistNotifications(id, isOn)
            val name = mutableState.value.page?.name.orEmpty()
            val context = getApplication<Application>()
            Toasts.show(context.getString(if (isOn) R.string.artist_notify_on else R.string.artist_notify_off, name))
        }
    }

    // Shows the translated description or the original
    fun toggleTranslation() {
        mutableState.update { it.copy(showTranslation = !it.showTranslation) }
    }

    // Asks MusicBrainz about the artist, the page does not wait for it
    private fun loadProfile(page: ArtistPage) {
        viewModelScope.launch {
            repository.profile(listOfNotNull(page.subscription?.channelId, id), page.name).onSuccess { profile ->
                mutableState.update { it.copy(profile = profile) }
            }
        }
    }

    // Translates the description once, and keeps the translation only when it was written in another language than the one of the device
    private fun prepareTranslation(description: String) {
        viewModelScope.launch {
            val target = Locale.getDefault().language
            val lines = description.split("\n")

            LyricsTranslator.translate(lines, target).onSuccess { result ->
                if (!result.sourceLanguage.equals(target, ignoreCase = true)) {
                    mutableState.update { it.copy(translation = result.lines.joinToString("\n")) }
                }
            }
        }
    }
}
