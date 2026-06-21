package com.lonewolf.wavvy.data

// Image extension
fun String.resize(
    width: Int? = null,
    height: Int? = null
): String {
    if (width == null && height == null) return this
    // Domain validation
    val isGoogleCdn = this.contains("googleusercontent.com") || this.contains("ggpht.com")
    val isYtimg = this.contains("i.ytimg.com")
    if (isGoogleCdn) {
        // Skip static system placeholders to prevent parsing corruption
        if (this.contains("/profile/picture/")) return this
        val w = width ?: height!!
        val h = height ?: width!!
        // Pattern matching
        if (this.contains(Regex("w\\d+-h\\d+"))) {
            return this.replace(Regex("w\\d+-h\\d+"), "w$w-h$h")
        }
        // Base url parsing
        val baseUrl = this.split("=w", "=s", "=h", limit = 2)[0]
        // Parameter assignment
        return if ((this.contains("=w") && this.contains("-h")) || (width != null && height != null)) {
            "$baseUrl=w$w-h$h-p-l90-rj"
        } else {
            "$baseUrl=s$w-p-l90-rj"
        }
    } else if (isYtimg) {
        val w = width ?: height!!
        // Size adjustment
        return if (w > 480) {
            // Strip structural query parameters to isolate standard maxres resolution
            val cleanUrl = this.split("?", limit = 2)[0]
            cleanUrl.replace("hqdefault.jpg", "maxresdefault.jpg")
                .replace("mqdefault.jpg", "maxresdefault.jpg")
                .replace("sddefault.jpg", "maxresdefault.jpg")
        } else if (w > 320) {
            val cleanUrl = this.split("?", limit = 2)[0]
            cleanUrl.replace("mqdefault.jpg", "hqdefault.jpg")
        } else {
            this
        }
    }
    return this
}
