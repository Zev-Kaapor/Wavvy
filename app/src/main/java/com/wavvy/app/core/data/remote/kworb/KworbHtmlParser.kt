package com.wavvy.app.core.data.remote.kworb

// HTML parsing engine
import org.jsoup.Jsoup

// Parses raw kworb HTML into structured chart entries
object KworbHtmlParser {

    // Global trending music table
    fun parseTrendingMusic(html: String): List<KworbChartEntry> {
        val doc = Jsoup.parse(html)
        val table = doc.selectFirst("table#trendingmusic") ?: doc.selectFirst("table")
        ?: return emptyList()

        val entries = mutableListOf<KworbChartEntry>()
        val rows = table.select("tbody tr")

        for (row in rows) {
            val cells = row.select("td")
            if (cells.size < 3) continue

            val position = cells[0].text().trim().toIntOrNull() ?: continue

            val linkEl = cells[2].selectFirst("a") ?: continue
            val fullTitle = linkEl.text().trim()
            if (fullTitle.isBlank()) continue

            val videoId = extractVideoId(linkEl.attr("href"))
            val (artist, title) = splitArtistTitle(fullTitle)

            entries.add(
                KworbChartEntry(
                    position = position,
                    title = title,
                    artist = artist,
                    videoId = videoId
                )
            )
        }

        return entries
    }

    // Country weekly chart
    fun parseCountryWeekly(html: String): List<KworbChartEntry> =
        parseInsightsTable(html, tableId = "weeklytable")

    // Country daily chart
    fun parseCountryDaily(html: String): List<KworbChartEntry> =
        parseInsightsTable(html, tableId = "dailytable")

    // Shared table parsing for daily and weekly insights pages
    private fun parseInsightsTable(html: String, tableId: String): List<KworbChartEntry> {
        val doc = Jsoup.parse(html)
        val table = doc.selectFirst("table#$tableId") ?: return emptyList()
        val entries = mutableListOf<KworbChartEntry>()

        val rows = table.select("tbody tr")
        for ((index, row) in rows.withIndex()) {
            val titleDiv = row.selectFirst("td.text.mp div") ?: continue
            val fullTitle = titleDiv.text().trim()
            if (fullTitle.isBlank()) continue

            val (artist, title) = splitArtistTitle(fullTitle)

            entries.add(
                KworbChartEntry(
                    position = index + 1,
                    title = title,
                    artist = artist
                )
            )
        }

        return entries
    }

    // Extract video ID from link
    private fun extractVideoId(href: String): String? {
        val match = Regex("/video/([A-Za-z0-9_-]{6,})\\.html").find(href)
        return match?.groupValues?.get(1)
    }

    // Split raw title string into artist and song title
    private fun splitArtistTitle(fullTitle: String): Pair<String?, String> {
        val separatorIndex = fullTitle.indexOf(" - ")
        if (separatorIndex == -1) {
            return null to fullTitle
        }
        val artist = fullTitle.substring(0, separatorIndex).trim().ifBlank { null }
        val title = fullTitle.substring(separatorIndex + 3).trim()
        return artist to title.ifBlank { fullTitle }
    }
}
