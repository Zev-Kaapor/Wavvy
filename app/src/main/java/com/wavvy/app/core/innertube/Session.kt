package com.wavvy.app.core.innertube

// Android context and web storage
import android.content.Context
import android.webkit.CookieManager
// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
// Project resources
import com.wavvy.app.features.auth.data.Entry
import com.wavvy.app.features.auth.data.EntryStore

// Who is asking YouTube Music, the language of the device, the visitor kept on it and the account when the user signed in with Google
suspend fun currentSession(context: Context): YouTubeSession {
    val locale = deviceLocale()
    val cookies = if (EntryStore(context).entry.first() == Entry.Google) {
        withContext(Dispatchers.Main) { CookieManager.getInstance().getCookie(MusicOrigin) }
    } else {
        null
    }

    return YouTubeSession(cookies = cookies, visitorData = VisitorStore(context).get(locale), locale = locale)
}
