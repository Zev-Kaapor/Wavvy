package com.wavvy.app.features.comments.ui

// UI styling and utilities
import androidx.compose.ui.unit.dp

// Sizes and limits of the sheet of comments, measured on the one of YouTube
object CommentsDimens {
    // How much of the height the sheet takes standing up, lying down it takes all it can
    const val SheetFraction = 0.8f

    // The title of the sheet and of the replies, where it starts and where the buttons beside it sit
    val HeaderHeight = 56.dp
    val HeaderStart = 16.dp
    val HeaderEnd = 4.dp
    val HeaderBackGap = 12.dp

    // The chips of the orders
    val ChipsHorizontal = 16.dp
    val ChipsBottom = 12.dp
    val ChipGap = 8.dp

    // The notice of YouTube on top of the comments
    val NoticeHorizontal = 12.dp
    val NoticeVertical = 12.dp

    // A comment, the photo of who wrote it and the space around it
    val CommentStart = 12.dp
    val CommentEnd = 16.dp
    val CommentTop = 16.dp
    val Avatar = 24.dp
    val AvatarGap = 12.dp
    val TextGap = 2.dp
    val TranslateTop = 12.dp
    val ActionsTop = 8.dp

    // The name of the artist of the album in its pill, and the mark of a checked channel
    val CreatorHorizontal = 6.dp
    val CreatorVertical = 1.dp
    val VerifiedIcon = 12.dp
    val VerifiedGap = 2.dp

    // The like, the dislike and the reply under a comment, how tall and wide they are and the gap before the count
    val ActionHeight = 40.dp
    val ActionIcon = 18.dp
    val LikeMinWidth = 40.dp
    val ActionWidth = 50.dp
    val CountGap = 6.dp

    // The line that ties a comment to its replies, the curve it makes into the link that opens them, and the link with its arrow
    val ThreadLine = 1.dp
    val ThreadLineGap = 4.dp
    val ThreadCurve = 10.dp
    val RepliesVertical = 8.dp
    val RepliesArrow = 18.dp

    // The space at the end of the list, and the spinner while more comments come
    val ListBottom = 16.dp
    val MoreSpinner = 24.dp
    val MorePadding = 16.dp

    // How many rows from the end the next comments are asked for
    const val LoadMoreThreshold = 5

    // How many comments are translated at the same time
    const val TranslateParallel = 4

    // Placeholder of the comments, how many there are, the height of its lines and their widths
    const val SkeletonRows = 6
    val SkeletonLine = 12.dp
    val SkeletonGap = 8.dp
    const val SkeletonHeadFraction = 0.45f
    const val SkeletonTextFraction = 0.9f
    const val SkeletonShortFraction = 0.6f
    val SkeletonActionWidth = 110.dp
}
