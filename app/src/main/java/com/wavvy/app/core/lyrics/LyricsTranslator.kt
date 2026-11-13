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
import java.net.URLEncoder
import java.util.Locale

// Lines of lyrics in another language, and the language they were written in
class LyricsTranslation(
    val lines: List<String>,
    val sourceLanguage: String
)

// Where Chinese is written in the traditional characters
private val TraditionalChineseCountries = setOf("TW", "HK", "MO")

// The code the translator asks for a language of the app or of the device, the ones it has under another name go to that one
// and the ones it does not know, such as Nynorsk, go to the closest
fun translationLanguageOf(locale: Locale): String = when (locale.language) {
    "zh" -> if (locale.script == "Hant" || locale.country in TraditionalChineseCountries) "zh-TW" else "zh-CN"
    "pt" -> if (locale.country == "PT") "pt-PT" else "pt"
    "nb", "nn" -> "no"
    "in" -> "id"
    "iw" -> "he"
    "fil" -> "tl"
    else -> locale.language
}

// Translates lyrics with the free Google Translate endpoints, no key and no account, as RiMusic does, so any song can be translated
internal object LyricsTranslator {
    // Endpoint RiMusic uses, and the one of the Chrome translator as a second way when the first is busy
    private const val GtxUrl = "https://translate.googleapis.com/translate_a/single"
    private const val ChromeUrl = "https://clients5.google.com/translate_a/t"
    private const val UserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
    private const val AutoLanguage = "auto"
    private const val LineBreakCost = 3

    // The most an address may carry, the endpoints answer a bad request to a longer one, as the notes of a release are
    private const val GroupLimit = 6000

    // The lines go in groups that fit in an address, one request for each, and the translations come back in the same order
    suspend fun translate(lines: List<String>, targetLanguage: String): Result<LyricsTranslation> {
        val target = translationLanguageOf(Locale.forLanguageTag(targetLanguage))
        val groups = mutableListOf<MutableList<String>>()
        var size = 0
        for (line in lines) {
            val cost = URLEncoder.encode(line, "UTF-8").length + LineBreakCost
            if (groups.isEmpty() || size + cost > GroupLimit) {
                groups += mutableListOf<String>()
                size = 0
            }
            groups.last() += line
            size += cost
        }

        val translated = ArrayList<String>(lines.size)
        var source = AutoLanguage
        for ((index, group) in groups.withIndex()) {
            val part = translateSplitting(group, target).getOrElse { return Result.failure(it) }
            translated += part.lines
            if (index == 0) source = part.sourceLanguage
        }
        return Result.success(LyricsTranslation(translated, source))
    }

    // A group the endpoints could not keep line by line goes in two halves, until each one is answered
    private suspend fun translateSplitting(lines: List<String>, target: String): Result<LyricsTranslation> {
        val whole = translateGroup(lines, target)
        if (whole.isSuccess || lines.size < 2) return whole
        val middle = lines.size / 2
        val first = translateSplitting(lines.subList(0, middle), target).getOrElse { return Result.failure(it) }
        val second = translateSplitting(lines.subList(middle, lines.size), target).getOrElse { return Result.failure(it) }
        return Result.success(LyricsTranslation(first.lines + second.lines, first.sourceLanguage))
    }

    // All the lines of a group in one request, the line breaks keep each translation in front of its line
    private suspend fun translateGroup(lines: List<String>, target: String): Result<LyricsTranslation> {
        val text = lines.joinToString("\n")
        var lastError: Throwable = IllegalStateException("Translation unavailable")

        for (attempt in listOf(::viaGtx, ::viaChrome)) {
            try {
                val (translated, source) = attempt(text, target)
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
