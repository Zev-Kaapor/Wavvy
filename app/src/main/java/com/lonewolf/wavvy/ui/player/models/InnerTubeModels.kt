package com.lonewolf.wavvy.ui.player.models

import com.google.gson.annotations.SerializedName

data class NextResponse(
    @SerializedName("contents") val contents: Contents?
)

data class Contents(
    @SerializedName("singleColumnMusicWatchNextResultsRenderer") val singleColumnMusicWatchNextResultsRenderer: SingleColumnMusicWatchNextResultsRenderer?
)

data class SingleColumnMusicWatchNextResultsRenderer(
    @SerializedName("tabbedRenderer") val tabbedRenderer: TabbedRenderer?
)

data class TabbedRenderer(
    @SerializedName("watchNextTabbedResultsRenderer") val watchNextTabbedResultsRenderer: WatchNextTabbedResultsRenderer?
)

data class WatchNextTabbedResultsRenderer(
    @SerializedName("tabs") val tabs: List<Tab>?
)

data class Tab(
    @SerializedName("tabRenderer") val tabRenderer: TabRenderer?
)

data class TabRenderer(
    @SerializedName("content") val content: TabContent?
)

data class TabContent(
    @SerializedName("sectionListRenderer") val sectionListRenderer: SectionListRenderer?
)

data class SectionListRenderer(
    @SerializedName("contents") val contents: List<SectionContent>?
)

data class SectionContent(
    @SerializedName("musicPlaylistShelfRenderer") val musicPlaylistShelfRenderer: MusicPlaylistShelfRenderer?
)

data class MusicPlaylistShelfRenderer(
    @SerializedName("playlistPanelRenderer") val playlistPanelRenderer: PlaylistPanelRenderer?
)

data class PlaylistPanelRenderer(
    @SerializedName("contents") val contents: List<PlaylistPanelContent>?,
    @SerializedName("continuations") val continuations: List<ContinuationWrapper>?
)

data class PlaylistPanelContent(
    @SerializedName("playlistPanelVideoRenderer") val playlistPanelVideoRenderer: PlaylistPanelVideoRenderer?
)

data class PlaylistPanelVideoRenderer(
    @SerializedName("videoId") val videoId: String?,
    @SerializedName("title") val title: RunsWrapper?,
    @SerializedName("longBylineText") val longBylineText: RunsWrapper?,
    @SerializedName("lengthText") val lengthText: RunsWrapper?,
    @SerializedName("thumbnail") val thumbnail: ThumbnailWrapper?,
    @SerializedName("navigationEndpoint") val navigationEndpoint: NavigationEndpoint?
)

data class RunsWrapper(
    @SerializedName("runs") val runs: List<Run>?
)

data class Run(
    @SerializedName("text") val text: String?,
    @SerializedName("navigationEndpoint") val navigationEndpoint: NavigationEndpoint?
)

data class ThumbnailWrapper(
    @SerializedName("thumbnails") val thumbnails: List<ThumbnailItem>?
)

data class ThumbnailItem(
    @SerializedName("url") val url: String?,
    @SerializedName("width") val width: Int?,
    @SerializedName("height") val height: Int?
)

data class NavigationEndpoint(
    @SerializedName("watchEndpoint") val watchEndpoint: WatchEndpointWrapper?
)

data class WatchEndpointWrapper(
    @SerializedName("musicVideoType") val musicVideoType: String?
)

data class ContinuationWrapper(
    @SerializedName("nextContinuationData") val nextContinuationData: NextContinuationData?
)

data class NextContinuationData(
    @SerializedName("continuation") val continuation: String?
)
