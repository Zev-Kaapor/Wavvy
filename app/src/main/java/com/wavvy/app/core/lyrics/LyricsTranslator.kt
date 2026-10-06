package com.wavvy.app.core.lyrics

// Ktor networking
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
// JSON parsing
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
// Coroutines
import kotlinx.coroutines.CancellationException

// Lines of lyrics in another language, and the language they were written in
class LyricsTranslation(
    val lines: List<String>,
    val sourceLanguage: String
)

// Translates lyrics with the free Google Translate endpoints, no key and no account, as RiMusic does, so any song can be translated
internal object LyricsTranslator {
    // Endpoint RiMusic uses, and the one of the Chrome translator as a second way when the first is busy
    private const val GtxUrl = "https://translate.googleapis.com/translate_a/single"
    private const val ChromeUrl = "https://clients5.google.com/translate_a/t"
    private const val UserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
    private const val AutoLanguage = "auto"

    // All the lines in one request, the line breaks keep each translation in front of its line
    suspend fun translate(lines: List<String>, targetLanguage: String): Result<LyricsTranslation> {
        val text = lines.joinToString("\n")
        var lastError: Throwable = IllegalStateException("Translation unavailable")

        for (attempt in listOf(::viaGtx, ::viaChrome)) {
            try {
                val (translated, source) = attempt(text, targetLanguage)
                val translatedLines = translated.split("\n")
                if (translatedLines.size == lines.size) return Result.success(LyricsTranslation(translatedLines.map { it.trim() }, source))
                lastError = IllegalStateException("Translation changed the number of lines")
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                lastError = error
            }
        }
        return Result.failure(lastError)
    }

    // Answer is a list of pieces, each with its translation first, and the source language third
    private suspend fun viaGtx(text: String, target: String): Pair<String, String> {
        val body = lyricsHttp.get(GtxUrl) {
            header("User-Agent", UserAgent)
            parameter("client", "gtx")
            parameter("dt", "t")
            parameter("ie", "UTF-8")
            parameter("oe", "UTF-8")
            parameter("sl", AutoLanguage)
            parameter("tl", target)
            parameter("hl", target)
            parameter("q", text)
        }.bodyAsText()

        val root = lyricsJson.parseToJsonElement(body).jsonArray
        val translated = root[0].jsonArray.joinToString("") { piece ->
            (piece as? JsonArray)?.getOrNull(0)?.jsonPrimitive?.contentOrNull.orEmpty()
        }
        val source = root.getOrNull(2)?.jsonPrimitive?.contentOrNull ?: AutoLanguage
        return translated to source
    }

    // Answer is the translation and the source language in a pair
    private suspend fun viaChrome(text: String, target: String): Pair<String, String> {
        val body = lyricsHttp.get(ChromeUrl) {
            header("User-Agent", UserAgent)
            parameter("client", "dict-chrome-ex")
            parameter("sl", AutoLanguage)
            parameter("tl", target)
            parameter("q", text)
        }.bodyAsText()

        val first = lyricsJson.parseToJsonElement(body).jsonArray[0]
        return if (first is JsonArray) {
            first[0].jsonPrimitive.content to (first.getOrNull(1)?.jsonPrimitive?.contentOrNull ?: AutoLanguage)
        } else {
            first.jsonPrimitive.content to AutoLanguage
        }
    }
}
