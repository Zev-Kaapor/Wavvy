package com.wavvy.app.features.player.ui.components

// UI styling and utilities
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Sizes and timings of the player, the same the old Wavvy used
object PlayerDimens {
    // No size, for elevations and widths that start empty
    val None = 0.dp

    // Pill of the mini player, its height, corners and border, and how much of the width it takes standing and lying
    val MiniHeight = 64.dp
    val MiniCorner = 32.dp
    val MiniBorder = 1.dp
    const val MiniBorderAlpha = 0.23f
    const val MiniWidthFraction = 0.92f
    const val MiniWidthFractionLandscape = 0.55f

    // Border once the pill starts to open, thinner and fading out as it grows
    val OpeningBorder = 0.5.dp

    // Space between the pill and the navigation bar, and between the pill and the bottom when lying
    val MiniGap = 5.dp
    val MiniBottomLandscape = 20.dp

    // Fade over the content behind the pill, how far above the bottom of the pill it reaches standing and lying, and how strong its middle is
    val ShadeAbovePill = 160.dp
    val ShadeAbovePillLandscape = 50.dp
    const val ShadeMiddleAlpha = 0.7f
    const val ShadeFadeMillis = 400

    // The pill rises and fades in when it first shows up
    const val EntranceOffsetPx = 150f
    const val EntranceFadeMillis = 500
    const val EntranceDamping = 0.82f
    const val EntranceStiffness = 350f

    // Opening and closing the player
    const val ExpandDamping = 0.85f
    const val ExpandStiffness = 400f

    // A release faster than this follows the fling, a slower one moves on once a quarter of the way is dragged
    val FlingVelocity = 125.dp
    const val PositionalThreshold = 0.25f

    // Steps the shadow and the colors of the text and icons change in while opening, instead of on every frame
    const val TextSteps = 10
    const val ColorSteps = 20

    // Below this share the player counts as the pill, above it the full player shows its content, fading in
    const val CollapsedThreshold = 0.1f
    const val ContentStart = 0.4f
    const val ContentFadeSpeed = 2f

    // Tone the open player is lifted to
    val ExpandedElevation = 3.dp

    // Dragging the pill down this far, or flinging it this fast, closes it, and sideways this share of its width changes song
    const val DismissDistancePx = 300f
    const val DismissVelocity = 800f
    const val DismissFadeMillis = 300
    const val SwipeFraction = 0.2f

    // Cover of the mini player, its size, place and corners
    val MiniCover = 44.dp
    val MiniCoverStart = 16.dp
    val MiniCoverTop = 10.dp
    val MiniCoverCorner = 22.dp

    // Cover of the open player, its corners, its size and place lying, how far from the top it sits, and its size on short screens
    val ExpandedCoverCorner = 16.dp
    val ExpandedCoverLandscape = 280.dp
    val CoverLandscapeOffset = 40.dp
    const val ExpandedCoverTopFraction = 0.12f
    val ShortScreenHeight = 700.dp
    const val ShortScreenCoverFraction = 0.82f

    // The edges of the open cover fade into the backdrop, from this share of the opening and this strong
    const val CoverFadeStart = 0.5f
    const val CoverFadeStrength = 0.4f

    // Blurred copy of the cover behind the open player, blurred once on a tiny copy, and the dark that keeps the controls readable, stop and strength
    const val BackdropBlurRadius = 8
    const val BackdropBlurPasses = 3
    val BackdropShade = listOf(0f to 0.5f, 0.2f to 0.15f, 0.4f to 0f, 0.55f to 0f, 0.68f to 0.45f, 0.85f to 0.85f, 1f to 1f)

    // Ring of the song progress around the cover, its line, its distance from the cover, and when it is gone while opening
    val RingStroke = 2.dp
    val RingGap = 3.dp
    const val RingRefreshMillis = 200L
    const val RingFadeEnd = 0.2f

    // Pictures of the cover asked in these sizes, and the share of the cover the note of the placeholder takes
    const val CoverRequestSize = 1080
    const val BackdropRequestSize = 64
    const val PlaceholderIconFraction = 0.4f
    const val PlaceholderIconAlpha = 0.2f

    // Title and artist in the pill, where they start, how much they overlap the button and how much of their room the lines take
    val InfoStart = 76.dp
    val InfoTop = 10.dp
    val InfoButtonOverlap = 25.dp
    val InfoSpacing = (-4).dp
    const val InfoWidthFraction = 0.85f

    // Title and artist in the open player, where they go standing and lying, and the space between them
    val InfoStartExpanded = 30.dp
    val InfoStartLandscape = 370.dp
    val InfoTopLandscape = 75.dp
    val InfoAboveBottom = 255.dp
    val InfoSpacingExpanded = 6.dp
    val InfoSpacingLandscape = (-2).dp
    val InfoSpacingLandscapeExpanded = 4.dp
    val TitleTopExpanded = 8.dp

    // Sizes the title and the artist are laid out in, the smaller ones on narrow screens, and the sizes they show at in the pill
    val TitleSize = 18.sp
    val TitleSizeSmall = 17.sp
    val ArtistSize = 16.sp
    val ArtistSizeSmall = 13.sp
    const val MiniTitleSize = 14f
    const val MiniArtistSize = 11f
    val SmallScreenWidth = 370.dp

    // Shadow under the title and the artist over the picture
    const val TextShadowAlpha = 0.8f
    const val TextShadowBlur = 24f

    // Person icon before the artist, and when the artist turns bold while opening
    val ArtistIcon = 14.dp
    val ArtistIconExpanded = 18.dp
    val ArtistIconEnd = 4.dp
    const val ArtistIconAlpha = 0.7f
    const val ArtistBoldStart = 0.5f

    // Names that do not fit slide once, after a wait
    const val MarqueeIterations = 1
    const val MarqueeDelayMillis = 3000
    val MarqueeVelocity = 30.dp

    // Share and favorite next to the title, their room, place and when they fade in
    val SideActionsWidth = 110.dp
    val SideActionsMargin = 10.dp
    val SideActionsMarginLandscape = 40.dp
    val SideActionsEnd = 30.dp
    val SideActionsEndLandscape = 60.dp
    val SideActionsBelowInfo = 12.dp
    val SideActionsTopLandscape = 85.dp
    const val SideActionsStart = 0.7f
    const val SideActionsFadeSpeed = 3.33f
    val SideActionsPaddingHorizontal = 12.dp
    val SideActionsPaddingVertical = 6.dp
    val SideActionsSpacing = 8.dp
    val SideAction = 32.dp
    val ShareIcon = 20.dp
    val FavoriteIcon = 22.dp
    val DividerWidth = 1.dp
    val DividerHeight = 16.dp
    const val SideActionsBackgroundAlpha = 0.12f
    const val DividerAlpha = 0.15f

    // Buttons that shrink a little while pressed
    const val PressedScale = 0.85f

    // Play button of the pill, its place from the end and the top, its size, corners and icon
    val ButtonEndInset = 56.dp
    val ButtonTop = 12.dp
    val Button = 40.dp
    val ButtonCorner = 20.dp
    val ButtonIcon = 24.dp
    const val ButtonContainerAlpha = 0.08f
    const val ButtonExpandedAlpha = 0.1f
    const val ButtonPressedScale = 0.95f
    const val PressMinMillis = 120L

    // Controls of the open player, their size standing and lying, corners, icons, gap and place
    val ControlsWidth = 160.dp
    val ControlsWidthLandscape = 180.dp
    val ControlsHeight = 68.dp
    val ControlsHeightLandscape = 72.dp
    val ControlsCorner = 50.dp
    val ControlsIcon = 32.dp
    val ControlsGap = 8.dp
    val ControlsGapLandscape = 5.dp
    val ControlsAboveToolbar = 20.dp
    val ControlsTopLandscape = 230.dp
    val ControlsRowPadding = 24.dp
    val ControlsRowInset = 48.dp
    val ControlsAreaStartLandscape = 320.dp
    val SkipCorner = 18.dp
    const val ControlsRowWidthFactor = 2.1f
    const val SquishShiftDivisor = 2.6f
    const val PlayWeightBase = 1.5f

    // Skip buttons fade in near the end of the opening
    const val SkipStart = 0.8f
    const val SkipFadeLength = 0.15f

    // How much room each control takes, resting and while one of them is pressed
    const val SideWeight = 0.7f
    const val SideWeightPressed = 0.86f
    const val SideWeightOtherPressed = 0.65f
    const val SideWeightMainPressed = 0.55f
    const val PlayWeight = 1.6f
    const val PlayWeightPressed = 1.85f
    const val WeightDamping = 0.6f
    const val WeightStiffness = 500f

    // The icon turns half a circle between play and pause, and becomes a spinning arc while the song loads
    const val PlayingRotation = 180f
    const val RotationDamping = 0.6f
    const val LoadingStrokeFraction = 0.09f
    const val MorphMillis = 450
    const val SpinMillis = 900
    const val LoadingSweep = 270f
    const val FullTurn = 360f
    const val IconStartScale = 0.5f

    // Toolbar at the bottom of the open player, its room from the edges, its height and how faint the unselected icons are
    val ToolbarHeight = 56.dp
    val ToolbarSide = 20.dp
    val ToolbarBottomGesture = 20.dp
    val ToolbarBottomInsetGap = 8.dp
    val ToolbarPaddingHorizontal = 12.dp
    val ToolbarPaddingVertical = 2.dp
    val ToolbarSpacing = 4.dp
    val ToolbarButton = 52.dp
    val ToolbarIcon = 24.dp
    val ToolbarWidthLandscape = 540.dp
    val GestureNavMaxInset = 24.dp
    const val ToolbarInactiveAlpha = 0.6f
    const val ToolbarBackgroundAlpha = 0.08f
    val RepeatOneSize = 9.sp
    const val QueueBouncePx = -4f
    const val QueueBounceMillis = 100

    // Arrow that closes the open player
    val MinimizeTop = 16.dp
    val MinimizeStart = 8.dp
    val MinimizeButton = 48.dp
    val MinimizeIcon = 28.dp

    // Content of the open player, its side room, where the seekbar sits standing and lying, and its fades
    val ContentSide = 24.dp
    const val CoverAreaFraction = 0.42f
    val SeekbarAboveControls = 45.dp
    val SeekbarTopLandscape = 135.dp
    val SeekbarSideLandscape = 16.dp
    const val ContentFadeInMillis = 300
    const val ContentFadeOutMillis = 250

    // Seekbar, its touch height, side room, line thicknesses and thumb, and how it grows while dragged
    val SeekbarHeight = 48.dp
    val SeekbarSide = 16.dp
    val SeekbarTrack = 3.dp
    val SeekbarActive = 5.dp
    val ThumbHalo = 11.dp
    val Thumb = 5.dp
    const val ThumbHaloAlpha = 0.2f
    const val ThumbDragScale = 1.4f
    const val TrackAlpha = 0.2f
    const val WaveMutedAlpha = 0.6f
    const val WaveTranslucentAlpha = 0.35f
    const val WaveMillis = 4000
    const val SeekSnapDistance = 0.02f
    const val SeekGlideMillis = 500
    const val ProgressRefreshMillis = 250L
    val TimeTextSize = 11.sp
    const val TimeSlideMillis = 300

    // Lyrics mode, the dark over the player, how the cover and the song fade out, and how often the position is read for the lines
    const val LyricsDimAlpha = 0.35f
    const val LyricsDimMillis = 600
    const val LyricsStart = 0.8f
    const val LyricsFullStart = 0.9f
    const val LyricsCoverFadeMillis = 400
    const val LyricsFadeMillis = 600
    const val LyricsRefreshMillis = 100L
    const val LyricsLoadingTimeoutMillis = 15_000L

    // Area over the cover that opens the lyrics, its height share and its top
    const val LyricsTapHeightFraction = 0.6f
    val LyricsTapTop = 80.dp

    // Room of the lyrics overlay standing and lying, and of the list under the song name
    val LyricsTop = 40.dp
    val LyricsBottom = 320.dp
    val LyricsBottomLandscape = 100.dp
    val LyricsListTop = 80.dp
    val LyricsHeaderSide = 64.dp
    val LyricsHeaderTopLandscape = 20.dp
    val LyricsTitleSize = 16.sp
    val LyricsTitleLineHeight = 28.sp
    val LyricsArtistSize = 12.sp
    const val LyricsArtistAlpha = 0.7f
    val LyricsMenuButton = 40.dp
    val LyricsMenuIcon = 24.dp

    // Lines of the lyrics, their sizes timed and plain, standing and lying, and their room
    val LyricSize = 20.sp
    val LyricSizeLandscape = 28.sp
    val LyricLineHeight = 20.sp
    val LyricLineHeightLandscape = 34.sp
    val PlainLyricSize = 18.sp
    val PlainLyricLineHeight = 24.sp
    val LyricSide = 24.dp
    val LyricGap = 6.dp
    val LyricGapLandscape = 12.dp
    val PlainLyricGap = 8.dp
    val PlainLyricsTop = 40.dp
    val PlainLyricsBottom = 100.dp
    const val LyricsCenterFraction = 0.25f
    const val LyricsCenterFractionLandscape = 0.5f
    const val LyricsEndFraction = 0.75f

    // The line being sung is bright and big, the ones around it fade and shrink like a wheel
    const val LyricInactiveAlpha = 0.45f
    const val LyricInactiveScale = 0.95f
    const val LyricNearScale = 0.92f
    const val LyricNearScaleLandscape = 0.85f
    const val LyricFarScale = 0.85f
    const val LyricFarScaleLandscape = 0.75f
    const val LyricOtherLineAlpha = 0.8f
    const val LyricShadowAlpha = 0.7f
    const val LyricShadowOffsetY = 2f
    const val LyricShadowBlur = 15f
    const val LyricScrollMillis = 450
    const val UnsungWordAlpha = 0.45f
    const val LyricJumpMillis = 1000L
    const val LyricSeekGraceMillis = 500L

    // Text of the lines bigger or smaller by the chosen size
    const val LyricSmallScale = 0.85f
    const val LyricLargeScale = 1.15f

    // Panel of the lyrics options, its side room, the space between its sections and rows, and the reorder buttons
    val OptionsSide = 20.dp
    val OptionsSectionGap = 20.dp
    val OptionsRowGap = 8.dp
    val OptionsTitleGap = 4.dp
    val OptionsRowHeight = 56.dp
    val OptionsMoveButton = 40.dp
    val OptionsBottom = 32.dp

    // Manual search of the lyrics, the card of each result, how many lines of the song it shows, and its spinner
    val SearchResultCorner = 16.dp
    val SearchResultPadding = 16.dp
    val SearchSpinner = 32.dp

    // Lyrics going up in each result, the height of a line and of the whole preview, how many lines it shows around the middle,
    // how long a line takes to go by and how many lines of the song are shown before it starts over
    val SearchPreviewLine = 22.dp
    val SearchPreviewHeight = 88.dp
    const val SearchPreviewReach = 2
    const val SearchPreviewMillisPerLine = 2000
    const val SearchPreviewMaxLines = 24

    // Translation under each line
    val TranslationGap = 4.dp
    val TranslationIcon = 14.dp
    val TranslationIconGap = 4.dp
    val TranslationSize = 16.sp
    val TranslationLineHeight = 18.sp
    const val TranslationAlpha = 0.7f

    // Lyrics not found, the box with the note, and the loading dots
    val NotFoundBox = 80.dp
    val NotFoundCorner = 24.dp
    val NotFoundIcon = 48.dp
    val NotFoundGap = 24.dp
    val NotFoundTextGap = 8.dp
    val LyricsStatePadding = 24.dp
    const val NotFoundBoxAlpha = 0.08f
    const val NotFoundIconAlpha = 0.5f
    const val LyricsHintAlpha = 0.6f
    val LoadingDot = 14.dp
    val LoadingDotGap = 8.dp
    val LoadingTextGap = 32.dp
    const val LoadingDotTravelPx = 20f
    const val LoadingDotDelayMillis = 150L
    const val LoadingDotMillis = 400
    const val LoadingDotCount = 3
    const val LoadingDotAlpha = 0.8f

    // Queue over the open player, the area at the bottom that drags it up standing and lying, and when it can open
    val QueueDragArea = 170.dp
    val QueueDragAreaLandscape = 85.dp
    const val QueueShowStart = 0.8f
    const val QueueTapStart = 0.95f
    const val QueueOpenFraction = 0.1f

    // Pulls past the ends of the list, down to close and up to bring more songs, and how the list follows the pull down
    const val QueueClosePullPx = 90f
    const val QueueLoadPullPx = 300f
    const val QueuePullResistance = 0.5f
    const val QueuePullTranslation = 0.4f
    const val QueuePullFade = 0.5f

    // Header, list and bottom bar of the queue
    val QueueTopLandscape = 20.dp
    val QueueHeaderPadding = 2.dp
    val QueueTitleSize = 14.sp
    val QueueListBottom = 80.dp
    val QueueBarHeight = 72.dp
    val QueueBarSide = 16.dp

    // Songs of the queue, their room, cover, texts and the icon at their end
    val QueueItemGap = 4.dp
    val QueueItemSide = 16.dp
    val QueueItemCorner = 12.dp
    val QueueItemPadding = 12.dp
    val QueueCover = 50.dp
    val QueueCoverCorner = 8.dp
    const val QueueCoverRequestSize = 226
    val QueueSearchCorner = 28.dp
    val QueueSelectionGap = 24.dp
    val QueueCheckboxEnd = 12.dp
    val QueueMore = 32.dp
    val QueueMoreIcon = 20.dp
    val QueueMenuCover = 56.dp
    val QueueVideoBadgeIcon = 10.dp
    val QueueVideoBadgePadding = 3.dp
    val QueueVideoBadgeInset = 3.dp
    val QueueTextSide = 16.dp
    val QueueIndicator = 32.dp
    val QueueIndicatorStart = 8.dp
    val QueueIndicatorIcon = 20.dp
    val QueueLoadingIcon = 18.dp
    val QueueLoadingStroke = 2.dp
    const val QueueHistoryAlpha = 0.75f
    const val QueueArtistAlpha = 0.7f
    const val QueueHandleAlpha = 0.4f
    const val QueueDraggingScale = 1.02f

    // Swiping a song, right removes it and left plays it next, how far it goes and the color behind it
    const val QueueSwipeThreshold = 0.2f
    const val QueueSwipeLimit = 0.2f
    const val QueueSwipeFadeAlpha = 0.55f
    val QueueSwipeCorner = 24.dp
    val QueueSwipeIconSide = 20.dp
    val QueueSwipeIcon = 22.dp

    // Spinner at the end of the list while more songs come
    val QueueSpinner = 28.dp
    val QueueSpinnerStroke = 2.5.dp
    val QueueSpinnerPadding = 20.dp
    const val QueueSpinnerTrackAlpha = 0.3f
    const val QueueSpinnerIdleAlpha = 0.5f
    const val QueueSpinnerMinScale = 0.8f

    // Empty queue
    val QueueEmptyPadding = 32.dp
    val QueueEmptyIcon = 80.dp
    val QueueEmptyGap = 16.dp
    const val QueueEmptyIconAlpha = 0.8f

    // Bars that dance next to the song that plays, their lowest share and how long each one takes to rise
    val EqualizerSize = 20.dp
    val EqualizerGap = 2.dp
    val EqualizerBarHeight = 16.dp
    val EqualizerBarCorner = 1.dp
    val EqualizerBars = listOf(0.3f to 400, 0.5f to 600, 0.2f to 500)
}
