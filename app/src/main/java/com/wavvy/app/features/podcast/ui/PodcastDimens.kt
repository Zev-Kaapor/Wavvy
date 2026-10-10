package com.wavvy.app.features.podcast.ui

// UI styling and utilities
import androidx.compose.ui.unit.dp

// Sizes of the page of a podcast as YouTube Music draws it, measured on the screen of a phone
object PodcastDimens {
    // The bar on top, its height, the little photo of who makes the podcast and the room it takes before its name
    val BarHeight = 56.dp
    val AuthorPhoto = 14.dp
    val AuthorGap = 6.dp
    val BarScrollThreshold = 240.dp
    const val BarFadeMillis = 200

    // The cover, its corners, and the room around the details under it
    val Side = 16.dp
    val CoverSize = 184.dp
    val CoverCorner = 4.dp
    val TitleTop = 18.dp
    val DescriptionTop = 8.dp
    val ActionsTop = 18.dp
    val ActionsBottom = 16.dp

    // The buttons under the description, the round ones and the one that saves, and the room between them
    val ActionSize = 40.dp
    val SaveWidth = 100.dp
    val ActionGap = 12.dp
    val ActionIcon = 20.dp

    // The row of filters, the height and corners of a filter, the room between them and the arrow of the one that orders
    val ChipHeight = 28.dp
    val ChipCorner = 8.dp
    val ChipGap = 6.dp
    val ChipPaddingX = 10.dp
    val ChipVertical = 6.dp
    val ChipArrow = 18.dp
    val SearchHeight = 52.dp

    // The part of the opening in which the field fades in, and in which it fades out when it closes
    const val SearchFadeFraction = 0.3f

    // An episode, its picture, the room around it and between its parts, the buttons and the line that ends it
    val ThumbnailWidth = 48.dp
    val ThumbnailCorner = 4.dp
    val RowTop = 12.dp
    val RowGap = 8.dp
    val DescriptionLines = 2
    val EpisodeAction = 40.dp
    val EpisodeActionIcon = 20.dp
    val EpisodeActionsStart = 4.dp
    val PillHeight = 28.dp
    val PillIcon = 16.dp
    val PillPaddingX = 10.dp
    val ProgressWidth = 36.dp
    val ProgressHeight = 4.dp
    val ProgressGap = 10.dp
    val SkeletonRows = 3
    val SkeletonLineHeight = 14.dp
    val SkeletonDescriptionHeight = 12.dp

    // The words that stand for a description that is long, how many letters of it show before the word that opens the rest
    const val DescriptionPreviewLetters = 130

    // Room kept between the end of the page and the mini player
    val MiniPlayerClearance = 30.dp
}
