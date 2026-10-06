package com.wavvy.app.core.lyrics

// Ktor networking
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
// JSON parsing
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
// XML parsing
import org.w3c.dom.Element
import org.w3c.dom.Node
import javax.xml.parsers.DocumentBuilderFactory

// Lyrics timed word by word, as Apple Music has them, from the BetterLyrics API, and the reader of their TTML format, adapted from Metrolist (GPL-3.0)
internal object BetterLyrics {
    private const val LyricsUrl = "https://lyrics-api.boidu.dev/getLyrics"
    private const val UserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    private const val ParameterNamespace = "http://www.w3.org/ns/ttml#parameter"
    private const val MetadataNamespace = "http://www.w3.org/ns/ttml#metadata"
    private const val MillisPerSecond = 1000.0
    private const val SecondsPerMinute = 60.0
    private const val SecondsPerHour = 3600.0

    // Roles of spans that are not words of the line itself
    private const val BackgroundRole = "x-bg"
    private val skippedRoles = setOf("x-translation", "x-roman")

    // Piece of a word with its timing, several pieces without a space between them make one word
    private class Span(val text: String, val startMs: Long, val endMs: Long, val spaceAfter: Boolean)

    // Exact title and artist, a cleaned name can bring the lyrics of another version of the song
    suspend fun getLyrics(title: String, artist: String, durationSeconds: Int): Result<SongLyrics> = runCatching {
        val body = lyricsHttp.get(LyricsUrl) {
            header("User-Agent", UserAgent)
            header("Accept", "application/json")
            parameter("s", title)
            parameter("a", artist)
            if (durationSeconds > 0) parameter("d", durationSeconds)
        }.bodyAsText()

        val ttml = lyricsJson.parseToJsonElement(body).jsonObject["ttml"]?.jsonPrimitive?.contentOrNull?.trim()
            ?.takeIf { it.isNotEmpty() } ?: throw IllegalStateException("Lyrics unavailable")
        val lines = parse(ttml)
        if (lines.isEmpty()) throw IllegalStateException("Failed to parse lyrics")
        SongLyrics(lines.sortedBy { it.timeMs }, isSynced = true)
    }

    // Lines of a TTML document, each with its words, also used for the TTML of LyricsPlus and Paxsenix
    fun parse(ttml: String): List<LyricLine> {
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        // External entities are never needed and are turned off where the platform allows it
        runCatching { factory.setFeature("http://xml.org/sax/features/external-general-entities", false) }
        runCatching { factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
        runCatching { factory.isExpandEntityReferences = false }

        val root = factory.newDocumentBuilder().parse(ttml.byteInputStream()).documentElement
        val offsetMs = child(root, "head")?.let { child(it, "metadata") }?.let { child(it, "audio") }
            ?.getAttribute("lyricOffset")?.toDoubleOrNull()?.let { (it * MillisPerSecond).toLong() } ?: 0L

        val lines = mutableListOf<LyricLine>()
        child(root, "body")?.let { walk(it, lines, offsetMs) }
        return lines
    }

    // Goes down the document until each paragraph, which is a line
    private fun walk(element: Element, lines: MutableList<LyricLine>, offsetMs: Long) {
        if (nameOf(element) == "p") {
            parseLine(element, offsetMs)?.let(lines::addAll)
            return
        }
        forEachChild(element) { walk(it, lines, offsetMs) }
    }

    // A line and the background vocals sung over it, each as its own line
    private fun parseLine(p: Element, offsetMs: Long): List<LyricLine>? {
        val begin = timing(p, "begin").ifEmpty { firstSpanBegin(p) ?: return null }
        val startMs = timeMs(begin) + offsetMs
        val isBackgroundLine = attribute(p, "role") == BackgroundRole

        val spans = mutableListOf<Span>()
        val background = mutableListOf<LyricLine>()
        forEachChild(p) { child ->
            if (nameOf(child) != "span") return@forEachChild
            when (attribute(child, "role")) {
                BackgroundRole -> if (isBackgroundLine) addSpan(child, offsetMs, spans) else backgroundLine(child, startMs, offsetMs)?.let(background::add)
                in skippedRoles -> Unit
                else -> addSpan(child, offsetMs, spans)
            }
        }

        val words = mergeSpans(spans)
        val text = if (words.isEmpty()) directText(p).trim() else lineText(words)
        val main = text.takeIf { it.isNotEmpty() }?.let { LyricLine(startMs, it, words) }
        return listOfNotNull(main) + background
    }

    // Background vocals inside a line
    private fun backgroundLine(span: Element, parentStartMs: Long, offsetMs: Long): LyricLine? {
        val begin = timing(span, "begin")
        val startMs = if (begin.isNotEmpty()) timeMs(begin) + offsetMs else parentStartMs
        val spans = mutableListOf<Span>()
        var hasSpans = false
        forEachChild(span) { child ->
            if (nameOf(child) == "span") {
                hasSpans = true
                if (attribute(child, "role") !in skippedRoles) addSpan(child, offsetMs, spans)
            }
        }

        val words = mergeSpans(spans)
        val text = when {
            !hasSpans -> span.textContent.orEmpty().trim()
            words.isEmpty() -> directText(span).trim()
            else -> lineText(words)
        }
        return text.takeIf { it.isNotEmpty() }?.let { LyricLine(startMs, it, words) }
    }

    // A timed piece of a word, with whether a space follows it
    private fun addSpan(span: Element, offsetMs: Long, spans: MutableList<Span>) {
        val begin = timing(span, "begin")
        val end = timing(span, "end")
        if (begin.isEmpty() || end.isEmpty()) return

        val text = span.textContent.orEmpty()
        val next = span.nextSibling
        val spaceAfter = text.lastOrNull()?.isWhitespace() == true ||
            (next?.nodeType == Node.TEXT_NODE && next.textContent?.firstOrNull()?.isWhitespace() == true)
        spans.add(Span(text, timeMs(begin) + offsetMs, timeMs(end) + offsetMs, spaceAfter))
    }

    // Joins the pieces with no space between them into whole words
    private fun mergeSpans(spans: List<Span>): List<LyricWord> {
        if (spans.isEmpty()) return emptyList()
        val words = mutableListOf<LyricWord>()
        var text = StringBuilder(spans[0].text)
        var start = spans[0].startMs
        var end = spans[0].endMs

        for (i in 1 until spans.size) {
            val previous = spans[i - 1]
            val current = spans[i]
            if (previous.spaceAfter && !previous.text.endsWith('-')) {
                words.add(LyricWord(text.toString().trim(), start, end))
                text = StringBuilder(current.text)
                start = current.startMs
            } else {
                text.append(current.text)
            }
            end = current.endMs
        }
        words.add(LyricWord(text.toString().trim(), start, end))
        return words.filter { it.text.isNotEmpty() }
    }

    // Text of a line from its words
    private fun lineText(words: List<LyricWord>): String = words.joinToString(" ") { it.text }.replace("- ", "-").trim()

    // Text of an element without its translations and background vocals
    private fun directText(element: Element): String = buildString {
        var child = element.firstChild
        while (child != null) {
            when {
                child.nodeType == Node.TEXT_NODE -> append(child.textContent)
                child is Element && nameOf(child) == "span" && attribute(child, "role").let { it != BackgroundRole && it !in skippedRoles } ->
                    append(child.textContent)
            }
            child = child.nextSibling
        }
    }

    // Earliest start among the spans of a line that has no start of its own
    private fun firstSpanBegin(p: Element): String? {
        var best: String? = null
        var bestMs = Long.MAX_VALUE
        forEachChild(p) { child ->
            if (nameOf(child) != "span") return@forEachChild
            val begin = timing(child, "begin")
            if (begin.isNotEmpty() && timeMs(begin) < bestMs) {
                bestMs = timeMs(begin)
                best = begin
            }
        }
        return best
    }

    // Times written as seconds, minutes and seconds, hours, or with a unit
    private fun timeMs(time: String): Long {
        val value = time.trim()
        val first = value.indexOf(':')
        val seconds = when {
            first != -1 -> {
                val last = value.lastIndexOf(':')
                if (first == last) {
                    (value.substring(0, first).toIntOrNull() ?: 0) * SecondsPerMinute + (value.substring(first + 1).toDoubleOrNull() ?: 0.0)
                } else {
                    (value.substring(0, first).toIntOrNull() ?: 0) * SecondsPerHour +
                        (value.substring(first + 1, last).toIntOrNull() ?: 0) * SecondsPerMinute +
                        (value.substring(last + 1).toDoubleOrNull() ?: 0.0)
                }
            }
            value.endsWith("ms") -> (value.dropLast(2).toDoubleOrNull() ?: 0.0) / MillisPerSecond
            value.endsWith("h") -> (value.dropLast(1).toDoubleOrNull() ?: 0.0) * SecondsPerHour
            value.endsWith("m") -> (value.dropLast(1).toDoubleOrNull() ?: 0.0) * SecondsPerMinute
            value.endsWith("s") -> value.dropLast(1).toDoubleOrNull() ?: 0.0
            else -> value.toDoubleOrNull() ?: 0.0
        }
        return (seconds * MillisPerSecond).toLong()
    }

    // Timing attribute, plain or in the parameter namespace
    private fun timing(element: Element, name: String): String =
        element.getAttribute(name).ifEmpty { element.getAttributeNS(ParameterNamespace, name) }

    // Metadata attribute such as the role, prefixed, plain or in the metadata namespace
    private fun attribute(element: Element, name: String): String =
        element.getAttribute("ttm:$name").ifEmpty { element.getAttribute(name) }.ifEmpty { element.getAttributeNS(MetadataNamespace, name) }

    // First child element with a name
    private fun child(parent: Element, name: String): Element? {
        var found: Element? = null
        forEachChild(parent) { if (found == null && nameOf(it) == name) found = it }
        return found
    }

    // Each child element in order
    private inline fun forEachChild(parent: Element, action: (Element) -> Unit) {
        var child = parent.firstChild
        while (child != null) {
            if (child is Element) action(child)
            child = child.nextSibling
        }
    }

    // Name of an element without its namespace prefix
    private fun nameOf(element: Element): String = element.localName ?: element.nodeName.substringAfterLast(':')
}
