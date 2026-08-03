package com.wavvy.app.core.data.utils

// Image utility extensions
fun String.resize(
    width: Int? = null,
    height: Int? = null
): String {
    if (width == null && height == null) return this

    // Domain validation
    val isGoogleCdn = this.contains("googleusercontent.com") || this.contains("ggpht.com")
    val isYtimg = this.contains("i.ytimg.com")

    if (isGoogleCdn) {
        if (this.contains("/profile/picture/")) return this
        val w = width ?: height!!
        val h = height ?: width!!

        // Pattern matching and size substitution
        if (this.contains(Regex("w\\d+-h\\d+"))) {
            return this.replace(Regex("w\\d+-h\\d+"), "w$w-h$h")
        }

        val baseUrl = this.split("=w", "=s", "=h", limit = 2)[0]

        // Parameter assignment and formatting
        return if ((this.contains("=w") && this.contains("-h")) || (width != null && height != null)) {
            "$baseUrl=w$w-h$h-p-l90-rj"
        } else {
            "$baseUrl=s$w-p-l90-rj"
        }
    } else if (isYtimg) {
        // Thumbnail resolution replacement
        val cleanUrl = this.substringBefore('?')
        return cleanUrl
            .replace("hqdefault.jpg", "maxresdefault.jpg")
            .replace("mqdefault.jpg", "maxresdefault.jpg")
            .replace("sddefault.jpg", "maxresdefault.jpg")
    }
    return this
}
