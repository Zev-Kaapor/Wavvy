package com.wavvy.app.core.download

// Image loading
import coil3.intercept.Interceptor
import coil3.request.ImageResult

// Gives every picture of the app that was saved with a download from its file, whatever size it is asked in, so it shows with no internet
object DownloadArtInterceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val data = request.data
        if (data !is String || !data.startsWith("http")) return chain.proceed()

        val saved = DownloadArt.localOrRemote(request.context, data)
        return if (saved != null && saved != data) chain.withRequest(request.newBuilder().data(saved).build()).proceed() else chain.proceed()
    }
}
