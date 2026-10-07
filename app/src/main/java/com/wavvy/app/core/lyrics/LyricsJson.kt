package com.wavvy.app.core.lyrics

// JSON parsing
import org.json.JSONArray
import org.json.JSONObject

// Writes lyrics as text, with the timing of each line and of each word, so the ones picked by hand can be kept on the device
internal fun SongLyrics.toJson(): String =
    JSONObject()
        .put("synced", isSynced)
        .put("title", title)
        .put("artist", artist)
        .put(
            "lines",
            JSONArray().apply {
                lines.forEach { line ->
                    put(
                        JSONObject()
                            .put("t", line.timeMs)
                            .put("x", line.text)
                            .apply {
                                if (line.words.isNotEmpty()) {
                                    put(
                                        "w",
                                        JSONArray().apply {
                                            line.words.forEach { word ->
                                                put(JSONObject().put("x", word.text).put("s", word.startMs).put("e", word.endMs))
                                            }
                                        }
                                    )
                                }
                            }
                    )
                }
            }
        )
        .toString()

// Reads lyrics written by toJson, empty when the text is not valid or has no lines
internal fun songLyricsFromJson(text: String): SongLyrics? = runCatching {
    val root = JSONObject(text)
    val lines = root.getJSONArray("lines")
    val parsed = (0 until lines.length()).map { index ->
        val line = lines.getJSONObject(index)
        val words = line.optJSONArray("w")?.let { array ->
            (0 until array.length()).map { position ->
                val word = array.getJSONObject(position)
                LyricWord(word.getString("x"), word.getLong("s"), word.getLong("e"))
            }
        }.orEmpty()
        LyricLine(line.getLong("t"), line.getString("x"), words)
    }

    SongLyrics(
        lines = parsed,
        isSynced = root.getBoolean("synced"),
        title = root.optString("title").takeIf { it.isNotEmpty() },
        artist = root.optString("artist").takeIf { it.isNotEmpty() }
    ).takeIf { parsed.isNotEmpty() }
}.getOrNull()
