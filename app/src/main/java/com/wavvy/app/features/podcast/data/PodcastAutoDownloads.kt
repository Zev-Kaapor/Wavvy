package com.wavvy.app.features.podcast.data

// Android context
import android.content.Context
// Coroutines and reactive flows
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.core.download.DownloadFolder
import com.wavvy.app.core.download.Downloads
import com.wavvy.app.features.home.ui.toPlayableTrack
import com.wavvy.app.features.settings.data.AutoPodcast
import com.wavvy.app.features.settings.data.SettingsStore

// Downloads by themselves the newest episodes of the podcasts the user chose, when the app opens and at once when the option is turned on,
// Android does not let a download start while the app is closed, so the app has to open for the new ones to come
object PodcastAutoDownloads {
    // How many of the newest episodes of a podcast are downloaded
    const val NewestEpisodes = 3

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Turns the option on or off for a podcast, turning it on downloads its newest episodes at once
    fun set(context: Context, podcast: AutoPodcast, isOn: Boolean) {
        val appContext = context.applicationContext
        scope.launch {
            SettingsStore(appContext).setAutoPodcast(podcast, isOn)
            if (isOn) check(appContext, podcast)
        }
    }

    // Looks at every podcast that has the option on
    suspend fun check(context: Context) {
        SettingsStore(context).autoPodcasts.first().forEach { podcast -> runCatching { check(context, podcast) } }
    }

    // Downloads the newest episodes that were not taken care of yet, so one the user took out is not downloaded again,
    // an episode that is not out yet has no length and waits for the next time
    private suspend fun check(context: Context, podcast: AutoPodcast) {
        val store = SettingsStore(context)
        val handled = store.autoEpisodes.first()
        val page = PodcastRepository(context).load(podcast.id).getOrNull() ?: return
        val folder = DownloadFolder(podcast.id, podcast.title, podcast.coverUrl)

        val fresh = page.episodes.episodes.filter { it.durationText != null }.take(NewestEpisodes).filter { it.id !in handled }
        fresh.forEach { episode -> episode.toPlayableTrack()?.let { Downloads.enqueue(context, it, folder) } }
        store.addAutoEpisodes(fresh.map { it.id })
    }
}
