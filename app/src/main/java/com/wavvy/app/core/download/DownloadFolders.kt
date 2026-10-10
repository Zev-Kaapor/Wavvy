package com.wavvy.app.core.download

// Android context and files
import android.content.Context
import java.io.File
// JSON
import org.json.JSONArray
import org.json.JSONObject
// Coroutines and reactive flows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// The folders each downloaded song belongs to, a song can be in several, one for each playlist, album or podcast that has it
// Kept in a small file of the app, so a song gets into a new folder at any time without being downloaded again
object DownloadFolders {
    private val mutableMap = MutableStateFlow<Map<String, List<DownloadFolder>>>(emptyMap())
    val map: StateFlow<Map<String, List<DownloadFolder>>> = mutableMap.asStateFlow()

    private var isLoaded = false

    // Reads the file once, the first time the folders are asked
    @Synchronized
    fun initialize(context: Context) {
        if (isLoaded) return
        isLoaded = true

        val file = fileOf(context)
        if (!file.exists()) return
        runCatching {
            val json = JSONObject(file.readText())
            val loaded = json.keys().asSequence().associateWith { songId ->
                val array = json.getJSONArray(songId)
                (0 until array.length()).map { index ->
                    val folder = array.getJSONObject(index)
                    DownloadFolder(folder.getString(IdKey), folder.getString(TitleKey), folder.optString(CoverKey).takeIf { it.isNotEmpty() && it != NullText })
                }
            }
            mutableMap.value = loaded
        }
    }

    // Puts the song in the folders it was not in yet
    @Synchronized
    fun add(context: Context, songId: String, folders: List<DownloadFolder>) {
        initialize(context)
        if (folders.isEmpty()) return

        val current = mutableMap.value[songId].orEmpty()
        val added = folders.filter { folder -> current.none { it.id == folder.id } }.distinctBy { it.id }
        if (added.isEmpty()) return

        mutableMap.update { it + (songId to current + added) }
        persist(context)
    }

    // Forgets the folders of the songs that were taken out
    @Synchronized
    fun remove(context: Context, songIds: Collection<String>) {
        initialize(context)
        if (songIds.none { it in mutableMap.value }) return

        mutableMap.update { it - songIds.toSet() }
        persist(context)
    }

    private fun persist(context: Context) {
        val json = JSONObject()
        mutableMap.value.forEach { (songId, folders) ->
            json.put(songId, JSONArray().also { array ->
                folders.forEach { array.put(JSONObject().put(IdKey, it.id).put(TitleKey, it.title).put(CoverKey, it.coverUrl)) }
            })
        }
        runCatching { fileOf(context).writeText(json.toString()) }
    }

    private fun fileOf(context: Context) = File(context.applicationContext.filesDir, FileName)

    private const val FileName = "download_folders.json"
    private const val IdKey = "id"
    private const val TitleKey = "title"
    private const val CoverKey = "cover"
    private const val NullText = "null"
}
