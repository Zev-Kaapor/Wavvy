package com.wavvy.app.features.profile.ui

// UI styling and utilities
import androidx.compose.ui.unit.dp

// Sizes of the profile avatar and of the profile menu
object ProfileDimens {
    // Gradient ring around the avatar, the gap between the ring and the photo, and the icon as a share of the avatar
    val AvatarRing = 2.dp
    val AvatarRingGap = 2.dp
    const val AvatarIconFraction = 0.5f

    // How opaque the circle behind the photo is
    const val AvatarContainerAlpha = 0.7f

    // Avatar in the header of the menu
    val MenuAvatar = 56.dp

    // Widest the menu gets, so it does not stretch on wide screens
    val MenuMaxWidth = 560.dp

    // Small bar at the top of the menu that shows it can be dragged, and how far it is dragged to close
    val HandleWidth = 32.dp
    val HandleHeight = 4.dp
    val DismissDistance = 96.dp

    // How dark the screen gets behind the menu
    const val ScrimAlpha = 0.5f

    // Icon of the action at the end of the header, and of each item of the list with its height
    val ActionIcon = 24.dp
    val ItemIcon = 24.dp
    val ItemHeight = 56.dp

    // The screen of the profile, the banner with the photo, the photo, and how much the banner shows of the colors of the app for a guest
    val BannerHeight = 250.dp
    val BigAvatar = 100.dp
    const val PlainBannerAlpha = 0.45f

    // The room between the parts, over the name, over a caption and at the end of the screen
    val SectionTop = 24.dp
    val NameTop = 12.dp
    val CaptionTop = 8.dp
    val ScreenBottom = 24.dp

    // The chips of the periods, the tiles of the numbers, their corners and the room inside them
    val ChipGap = 8.dp
    val TileGap = 12.dp
    val TileCorner = 20.dp
    val TilePadding = 16.dp

    // The covers and the round photos of the carousels, with the room between them and their corners
    val CarouselGap = 14.dp
    val CoverSize = 132.dp
    val ArtistSize = 96.dp
    val CoverCorner = 12.dp

    // The count of times listened beside the menu of a row, how much it tints the background, its corners, its least width and the room inside it, and where the mark of the refresh sits
    const val CountAlpha = 0.14f
    val CountCorner = 12.dp
    val CountMinWidth = 72.dp
    val CountPaddingHorizontal = 10.dp
    val CountPaddingVertical = 4.dp
    val RefreshTop = 8.dp

    // The placeholders while the numbers load, how many rows and photos, the size of a cover and the lines beside it
    const val SkeletonSongs = 3
    const val SkeletonArtists = 5
    val SkeletonCover = 48.dp
    val SkeletonTitleWidth = 180.dp
    val SkeletonLineWidth = 110.dp
    val SkeletonLineHeight = 14.dp
    val SkeletonLineGap = 8.dp

    // The card of the activity, the icon over each number, the picture of the artist heard the most and the room beside it
    val StatIcon = 22.dp
    val TopArtistPhoto = 72.dp
    val TopArtistGap = 16.dp

    // The box where the words about the user are written
    val DialogCorner = 28.dp
    val DialogPadding = 24.dp
    val DialogGap = 12.dp
}
