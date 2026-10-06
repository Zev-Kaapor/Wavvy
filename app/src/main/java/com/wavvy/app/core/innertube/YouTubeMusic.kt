package com.wavvy.app.core.innertube

// Network and security utilities
import java.security.MessageDigest
import java.util.Locale

// YouTube Music and the web client every request to it pretends to be
const val MusicOrigin = "https://music.youtube.com"
const val MusicApi = "$MusicOrigin/youtubei/v1"
const val ClientName = "WEB_REMIX"
const val ClientVersion = "1.20260707.12.00"
const val ClientId = "67"
const val WebUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"

// Language and country used when the device does not tell them
private const val FallbackLanguage = "en"
private const val FallbackCountry = "US"

// How many milliseconds make a second
private const val MillisPerSecond = 1000L

// Language and country the answers of YouTube Music come in
data class YouTubeLocale(val language: String, val country: String) {
    // Language with its region, as YouTube asks for it
    val hl: String get() = if (country.isEmpty()) language else "$language-$country"

    // Header that lists the language first and the plain language as a second choice
    val acceptLanguage: String get() = if (country.isEmpty()) language else "$hl,$language;q=0.9"
}

// Everything that tells YouTube who is asking, the cookies are empty for a guest
data class YouTubeSession(
    val cookies: String?,
    val visitorData: String?,
    val locale: YouTubeLocale
)

// Language and country of the device
fun deviceLocale(): YouTubeLocale {
    val device = Locale.getDefault()

    return YouTubeLocale(
        language = device.language.ifEmpty { FallbackLanguage },
        country = device.country.ifEmpty { FallbackCountry }
    )
}

// Signature that the requests of the signed in web player carry, made from the session cookie
fun signatureFor(cookies: String): String? {
    val sapisid = cookieValue(cookies, "SAPISID") ?: cookieValue(cookies, "__Secure-3PAPISID") ?: return null
    val timestamp = System.currentTimeMillis() / MillisPerSecond
    val digest = MessageDigest.getInstance("SHA-1").digest("$timestamp $sapisid $MusicOrigin".toByteArray())

    return "${timestamp}_${digest.joinToString("") { "%02x".format(it) }}"
}

// Value of one cookie in a cookie header
fun cookieValue(cookies: String, key: String): String? =
    Regex("(?:^|;\\s*)$key=([^;]+)").find(cookies)?.groupValues?.get(1)

// Cookie header with the language and the country of the request, so the account preferences do not win over them
fun cookiesWithLocale(cookies: String?, locale: YouTubeLocale): String {
    val parts = cookies.orEmpty().split(";").map { it.trim() }.filter { it.isNotEmpty() }
    val others = parts.filterNot { it.startsWith("PREF=", ignoreCase = true) }
    val kept = parts.filter { it.startsWith("PREF=", ignoreCase = true) }
        .flatMap { it.substringAfter("=").split("&") }
        .filterNot { it.startsWith("hl=") || it.startsWith("gl=") }
    val preferences = listOf("hl=${locale.hl}", "gl=${locale.country}") + kept

    return (others + "PREF=${preferences.joinToString("&")}").joinToString("; ")
}
