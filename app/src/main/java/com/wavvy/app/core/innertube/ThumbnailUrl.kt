package com.wavvy.app.core.innertube

// Math
import kotlin.math.roundToInt

// Addresses of the pictures of YouTube, which carry the size they are served in, adapted from Metrolist (GPL-3.0)
private val GoogleUserContentSize =
    Regex("^(https://(?:lh3|yt3)\\.googleusercontent\\.com/[^?]*?)=w(\\d+)-h(\\d+)[^?]*(\\?.*)?$")
private val GgphtSize =
    Regex("^(https://yt3\\.ggpht\\.com/[^?=]+)=(?:s\\d+|w\\d+-h\\d+)[^?]*(\\?.*)?$")

// The same picture asked in another size, the missing side keeps the shape of the original, other addresses stay as they are
fun String.resize(width: Int? = null, height: Int? = null): String {
    if (width == null && height == null) return this

    GoogleUserContentSize.matchEntire(this)?.groupValues?.let { group ->
        val originalWidth = group[2].toInt()
        val originalHeight = group[3].toInt()
        val targetWidth = width ?: ((height!!.toDouble() * originalWidth) / originalHeight).roundToInt()
        val targetHeight = height ?: ((width!!.toDouble() * originalHeight) / originalWidth).roundToInt()

        return "${group[1]}=w${targetWidth.coerceAtLeast(1)}-h${targetHeight.coerceAtLeast(1)}-p-l90-rj${group[4]}"
    }

    GgphtSize.matchEntire(this)?.groupValues?.let { group ->
        return if (width != null && height != null) {
            "${group[1]}=w$width-h$height-p-l90-rj${group[2]}"
        } else {
            "${group[1]}=s${width ?: height}${group[2]}"
        }
    }

    return this
}

// Pictures of videos, wide and sometimes with black bars above and below
private val VideoThumbnail = Regex("^https://i\\d?\\.ytimg\\.com/vi(?:_webp)?/([^/]+)/.*")

// True when the picture is the one of a video
fun String.isVideoThumbnail(): Boolean = VideoThumbnail.matches(this)

// Largest picture of the same video, wide and without black bars, empty for other addresses
fun String.largestVideoThumbnail(): String? =
    VideoThumbnail.matchEntire(this)?.groupValues?.get(1)?.let { "https://i.ytimg.com/vi/$it/maxresdefault.jpg" }
