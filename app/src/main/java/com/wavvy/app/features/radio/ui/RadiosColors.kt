package com.wavvy.app.features.radio.ui

// The colors of the stripes of the radios, a genre takes one by its number so it has the same color every time
object RadiosColors {
    private val Genres = listOf(
        0xFFE8453C, 0xFFF28B30, 0xFFF2C230, 0xFF7CB342, 0xFF26A69A, 0xFF29B6F6,
        0xFF5C6BC0, 0xFF9C5BD6, 0xFFD6579B, 0xFF8D6E63, 0xFF43A047, 0xFF1E88E5
    )

    // The color of the genre with the number the site gives it
    fun genre(id: Int): Long = Genres[id % Genres.size]

    // The stripes of the buttons that open the places and every station
    const val Places = 0xFF26A69A
    const val Stations = 0xFF29B6F6

    // The stripe of a capital
    const val Capital = 0xFFF2C230

    // The stripe of a place found by the search
    const val Match = 0xFF9C5BD6
}
