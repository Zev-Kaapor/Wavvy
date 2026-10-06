package com.wavvy.app.core.lyrics

// Ktor networking
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.compression.ContentEncoding
// JSON parsing
import kotlinx.serialization.json.Json

// Timeouts of the requests for lyrics and translations
private const val RequestTimeoutMillis = 15_000L
private const val ConnectTimeoutMillis = 10_000L

// HTTP client shared by the lyrics sources and the translator
internal val lyricsHttp by lazy {
    HttpClient(OkHttp) {
        expectSuccess = true
        install(ContentEncoding) {
            gzip()
            deflate()
        }
        install(HttpTimeout) {
            requestTimeoutMillis = RequestTimeoutMillis
            connectTimeoutMillis = ConnectTimeoutMillis
        }
    }
}

// Lenient reader for the JSON the sources answer with
internal val lyricsJson = Json {
    isLenient = true
    ignoreUnknownKeys = true
}
