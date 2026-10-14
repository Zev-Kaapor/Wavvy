package com.wavvy.app.core.radio

// Android context and WebView
import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
// Coroutines
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
// Collections and JSON
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import org.json.JSONObject
import org.json.JSONTokener

// The site and the page that lists every station of Brazil, which the other lists share the shape of
const val RadiosOrigin = "https://www.radios.com.br"
const val RadiosBrazilPath = "/radio/pais/brasil/33"

// The start of the id of a station in the player, which tells it from a song of YouTube Music
const val RadioIdPrefix = "radio:"

// True when what plays is a station, which is live and has no end
fun isRadioTrack(id: String?): Boolean = id?.startsWith(RadioIdPrefix) == true

// The title the site shows while it checks that the visitor is a person
private const val CheckTitle = "Just a moment"

// How long the page may take to pass the check, how often it is looked at, and how long one request may take
private const val CheckTimeoutMillis = 30_000L
private const val CheckPollMillis = 500L
private const val RequestTimeoutMillis = 30_000L

// How many letters of the stuck page are put in the notice
private const val DescribeLength = 160

// The name the page uses to answer the app
private const val BridgeName = "RadiosBridge"

// A station of the list, the address opens its page, where the stream is
data class RadioStation(
    val id: String,
    val name: String,
    val url: String,
    val logoUrl: String?,
    val place: String?,
    val segments: List<String>,
    val onAir: String?
) {
    // The biggest picture the site has of the logo, which the player and the notification use
    val largeLogoUrl: String?
        get() = logoUrl?.let { LogoSize.replace(it, "/radio/xl/") }
}

// The folder of a picture of the site says its size
private val LogoSize = Regex("/radio/(?:sm|md|lg|xl)/")

// A place of the site that opens another list, with its name and its address
data class RadiosEntry(val title: String, val path: String)

// What a page of places holds, the link to all the stations of it, and the countries, the states and the main cities it lists
data class RadiosBrowse(val all: RadiosEntry?, val countries: List<RadiosEntry>, val states: List<RadiosEntry>, val cities: List<RadiosEntry>)

// One page of a list, with how many stations the whole list has
data class RadiosPage(val stations: List<RadioStation>, val total: Int)

// The sound of a station, its address and the kind the site says it is
data class RadioStream(val url: String, val mimeType: String) {
    // The sound comes as a list of pieces, which the player has to be told
    val isHls: Boolean
        get() = mimeType.contains("mpegurl", ignoreCase = true) || url.contains(".m3u8", ignoreCase = true)
}

// Why the site could not be read
class RadiosException(message: String) : Exception(message)

// Reads radios.com.br through a hidden page of the browser of the system, which passes the check the site makes of its visitors
// Every request is made from inside the page, so it leaves with the same identity the check accepted
object RadiosReader {
    private var webView: WebView? = null
    private var isReady = false
    private val gate = Mutex()
    private val waiting = ConcurrentHashMap<Int, CancellableContinuation<String>>()
    private val counter = AtomicInteger()
    private val browsed = ConcurrentHashMap<String, RadiosBrowse>()

    // The stations of a page of a list, the address is the one of the site without the page number
    suspend fun list(context: Context, path: String, page: Int): Result<RadiosPage> = runCatching {
        // The site counts its pages from zero
        val address = "$path${if ('?' in path) '&' else '?'}pg=${page - 1}"
        val json = JSONObject(run(context, ListScript.replace(AddressMark, JSONObject.quote(address))))
        val items = json.getJSONArray("items")
        RadiosPage(
            stations = (0 until items.length()).map { index ->
                val item = items.getJSONObject(index)
                RadioStation(
                    id = item.getString("id"),
                    name = item.getString("name"),
                    url = item.getString("url"),
                    logoUrl = item.optString("logo").ifEmpty { null }?.let { LogoSize.replace(it, "/radio/lg/") },
                    place = item.optString("place").ifEmpty { null },
                    segments = item.getJSONArray("segments").let { list -> (0 until list.length()).map(list::getString) },
                    onAir = item.optString("onAir").ifEmpty { null }
                )
            },
            total = json.optInt("total")
        )
    }

    // The places a page lists, the countries, the states of a country or the cities of a state, kept for the next time they are asked
    suspend fun browse(context: Context, path: String): Result<RadiosBrowse> = runCatching {
        browsed[path]?.let { return@runCatching it }

        val json = JSONObject(run(context, BrowseScript.replace(AddressMark, JSONObject.quote(path))))
        fun entries(name: String): List<RadiosEntry> = json.getJSONArray(name).let { list ->
            (0 until list.length()).map { index -> list.getJSONObject(index).let { RadiosEntry(it.getString("title"), it.getString("path")) } }
        }
        RadiosBrowse(
            all = json.optJSONObject("all")?.let { RadiosEntry(it.getString("title"), it.getString("path")) },
            countries = entries("countries"),
            states = entries("states"),
            cities = entries("cities")
        ).also { browsed[path] = it }
    }

    // The sound of a station, read from its page
    suspend fun stream(context: Context, station: RadioStation): Result<RadioStream> = runCatching {
        val json = JSONObject(run(context, StreamScript.replace(AddressMark, JSONObject.quote(station.url))))
        RadioStream(url = json.getString("src"), mimeType = json.optString("type"))
    }

    // Lets the page go, a new one is made the next time
    fun release() {
        webView?.destroy()
        webView = null
        isReady = false
    }

    // Runs a script in the page and gives what it answers, one at a time
    private suspend fun run(context: Context, script: String): String = gate.withLock {
        val page = ready(context)
        try {
            withTimeout(RequestTimeoutMillis) {
                // The page only takes calls from the main thread, and a page that is not on the screen never runs what is posted to it
                withContext(Dispatchers.Main) {
                    suspendCancellableCoroutine { continuation ->
                        val id = counter.incrementAndGet()
                        waiting[id] = continuation
                        continuation.invokeOnCancellation { waiting.remove(id) }
                        page.evaluateJavascript(script.replace(IdMark, id.toString()), null)
                    }
                }
            }
        } catch (failure: TimeoutCancellationException) {
            throw RadiosException("The page did not answer")
        } catch (failure: RadiosException) {
            // A refusal of the site means the check has to be passed again
            if (failure.message?.contains(Refused) == true) isReady = false
            throw failure
        }
    }

    // The page that is open on the site, made on the first call and waited for until the check lets it through
    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun ready(context: Context): WebView = withContext(Dispatchers.Main) {
        val page = webView ?: WebView(context.applicationContext).also { created ->
            created.settings.javaScriptEnabled = true
            created.addJavascriptInterface(Bridge, BridgeName)
            created.webViewClient = WebViewClient()
            webView = created
        }
        if (isReady) return@withContext page

        page.loadUrl(RadiosOrigin)
        try {
            withTimeout(CheckTimeoutMillis) {
                while (!(title(page).let { it.isNotEmpty() && !it.contains(CheckTitle, ignoreCase = true) })) delay(CheckPollMillis)
            }
        } catch (failure: TimeoutCancellationException) {
            throw RadiosException("The site did not let the page through: ${describe(page)}")
        }
        isReady = true
        page
    }

    // What the page that is stuck says, so the notice tells why it could not go on
    private suspend fun describe(page: WebView): String = suspendCancellableCoroutine { continuation ->
        page.evaluateJavascript("document.title + ' | ' + (document.body ? document.body.innerText.slice(0, $DescribeLength) : '')") { value ->
            continuation.resume(runCatching { JSONTokener(value).nextValue() as String }.getOrDefault(value))
        }
    }

    // The title of the page that is open, empty while it loads
    private suspend fun title(page: WebView): String = suspendCancellableCoroutine { continuation ->
        page.evaluateJavascript("document.title") { value -> continuation.resume(value.trim('"')) }
    }

    // What the script in the page answers with, by the number of the request
    object Bridge {
        @JavascriptInterface
        fun done(id: Int, text: String) {
            waiting.remove(id)?.resume(text)
        }

        @JavascriptInterface
        fun fail(id: Int, reason: String) {
            waiting.remove(id)?.resumeWithException(RadiosException(reason))
        }
    }

    // The words of a refusal of the site, and the places in the scripts that are filled in before they run
    private const val Refused = "HTTP 403"
    private const val IdMark = "__ID__"
    private const val AddressMark = "__ADDRESS__"

    // Reads the stations of a list page, the same cards the search and every list of the site use
    private val ListScript = """
        (async () => {
            try {
                const response = await fetch($AddressMark, { credentials: 'include' });
                if (!response.ok) throw new Error('HTTP ' + response.status);
                const page = new DOMParser().parseFromString(await response.text(), 'text/html');
                const items = [...page.querySelectorAll('.resultado')].map(card => {
                    const link = card.querySelector('.data h3 a');
                    const href = link ? link.href : '';
                    const match = href.match(/\/aovivo\/[^\/]+\/(\d+)/);
                    if (!match) return null;
                    const onAir = card.querySelector('.noar p');
                    return {
                        id: match[1],
                        name: link.textContent.trim(),
                        url: href,
                        logo: (card.querySelector('.logo img') || {}).src || '',
                        place: ((card.querySelector('p.localizacao a') || {}).textContent || '').trim(),
                        segments: [...card.querySelectorAll('.segmentos a')].map(item => item.textContent.trim()),
                        onAir: onAir ? onAir.textContent.replace(/^\s*No ar:\s*/, '').trim() : ''
                    };
                }).filter(item => item);
                const total = (page.querySelector('.total-results') || {}).textContent || '';
                const count = total.match(/(\d+)\s+resultados/);
                $BridgeName.done(__ID__, JSON.stringify({ items: items, total: count ? parseInt(count[1], 10) : items.length }));
            } catch (error) {
                $BridgeName.fail(__ID__, String(error));
            }
        })();
    """.trimIndent()

    // Reads the places of a page, the countries, the states and the cities that lead to other lists, and the link to every station of the place
    private val BrowseScript = """
        (async () => {
            try {
                const response = await fetch($AddressMark, { credentials: 'include' });
                if (!response.ok) throw new Error('HTTP ' + response.status);
                const page = new DOMParser().parseFromString(await response.text(), 'text/html');
                const content = page.querySelector('#content') || page;
                const result = { all: null, countries: [], states: [], cities: [] };
                const seen = new Set();
                for (const link of content.querySelectorAll('a[href]')) {
                    const path = new URL(link.href).pathname;
                    const title = link.textContent.replace(/\s+/g, ' ').trim();
                    if (!title || seen.has(path)) continue;
                    let list = null;
                    if (/^\/radio\/(pais|uf)\/[^\/]+\/\d+$/.test(path)) {
                        if (!result.all) result.all = { title: title, path: path };
                        seen.add(path);
                        continue;
                    }
                    if (/^\/lista\/pais\/[^\/]+\/\d+$/.test(path)) list = result.countries;
                    else if (/^\/lista\/uf\/[^\/]+\/\d+$/.test(path)) list = result.states;
                    else if (/^\/radio\/cidade\/[^\/]+\/\d+$/.test(path)) list = result.cities;
                    if (list) {
                        seen.add(path);
                        list.push({ title: title, path: path });
                    }
                }
                $BridgeName.done(__ID__, JSON.stringify(result));
            } catch (error) {
                $BridgeName.fail(__ID__, String(error));
            }
        })();
    """.trimIndent()

    // Reads the address of the sound from the script of the player of a station page
    private val StreamScript = """
        (async () => {
            try {
                const response = await fetch($AddressMark, { credentials: 'include' });
                if (!response.ok) throw new Error('HTTP ' + response.status);
                const text = await response.text();
                const match = text.match(/type:\s*"([^"]+)",\s*src:\s*"([^"]+)"/);
                if (!match) throw new Error('No stream in the page');
                $BridgeName.done(__ID__, JSON.stringify({ type: match[1], src: match[2] }));
            } catch (error) {
                $BridgeName.fail(__ID__, String(error));
            }
        })();
    """.trimIndent()
}
