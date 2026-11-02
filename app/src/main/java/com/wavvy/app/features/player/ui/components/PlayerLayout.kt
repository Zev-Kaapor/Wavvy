package com.wavvy.app.features.player.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
// UI utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
// Math
import kotlin.math.roundToInt

// Space under the toolbar of the open player, a fixed margin with gesture navigation and above the buttons otherwise
@Composable
fun toolbarBottomPadding(): Dp {
    val navigation = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return if (navigation <= PlayerDimens.GestureNavMaxInset) PlayerDimens.ToolbarBottomGesture else navigation + PlayerDimens.ToolbarBottomInsetGap
}

// Height the toolbar takes from the bottom, with its space under it
@Composable
fun toolbarReservedHeight(): Dp = toolbarBottomPadding() + PlayerDimens.ToolbarHeight

// Where the parts of the open player go lying down, in two sides that line up with each other, all of it from the room the screen has
// The cover stays in the middle of the height of the screen, with the arrow in the corner before it, so the arrow never pushes it down
// The arrow and the tools stay on one line whatever opens, so a button never runs away from the finger, and the song takes the end under the tools
// With the lyrics or the queue open, which is the share given here as panel, the song goes to the start beside a smaller cover and they take the end
@Immutable
class LandscapeFrame(
    screenWidth: Dp,
    screenHeight: Dp,
    left: Dp,
    right: Dp,
    topInset: Dp,
    bottomInset: Dp
) {
    private val start = max(left, PlayerDimens.LandscapeRail)
    private val end = screenWidth - max(right, PlayerDimens.LandscapeMargin)
    private val top = max(topInset, PlayerDimens.LandscapeMargin)
    private val bottom = screenHeight - max(bottomInset, PlayerDimens.LandscapeMargin)

    // The cover, in the middle of the height of the screen
    private val bigCover = min(min(bottom - top, screenHeight * PlayerDimens.LandscapeCoverHeightFraction), screenWidth * PlayerDimens.LandscapeCoverFraction)
    val coverX = start
    private val coverY = top + (bottom - top - bigCover) / 2

    // The room each tool has around its icon, it stays out of the edges so the icons line up with them
    private val toolRoom = (PlayerDimens.LandscapeToolButton - PlayerDimens.LandscapeToolIcon) / 2

    // The line of the tools, at the top of the screen, the arrow in the middle of the room before the cover but never inside the cutout of the camera, and the last tool on the line of the end of the song
    val barTop = max(topInset, PlayerDimens.LandscapeBarTop)
    val barBottom = barTop + PlayerDimens.LandscapeToolButton
    val toolsCenter = barTop + PlayerDimens.LandscapeToolButton / 2
    val toolsStart = max(start - (PlayerDimens.LandscapeRail + PlayerDimens.LandscapeToolButton) / 2, left)
    val toolsEnd = end + toolRoom + PlayerDimens.LandscapeMoreEdge

    // The cast button is the tool before the last one, it comes from the pill so it is placed apart
    val castCenterX = toolsEnd - PlayerDimens.LandscapeToolButton * CastPlaceFromEnd

    // What the tools take of the side of the lyrics and of the queue, so a name over them stops before
    val toolsRoom = PlayerDimens.LandscapeToolButton * PlayerDimens.LandscapeToolCount - toolRoom - PlayerDimens.LandscapeMoreEdge

    // From the line of the seekbar to the end of its times, what shows of it, the rest of its height is only for the finger
    private val seekBlank = PlayerDimens.SeekbarHeight / 2
    private val seekShown = seekBlank + PlayerDimens.LandscapeTimesHeight

    // Beside the big cover the name, the seekbar and the controls are one group, in the middle of the height of the cover
    private val groupHeight = (PlayerDimens.LandscapeTitleHeight - PlayerDimens.LandscapeTitleBlank) +
        (PlayerDimens.SeekbarHeight + PlayerDimens.LandscapeTimesHeight + PlayerDimens.LandscapeSeekPush) +
        PlayerDimens.LandscapeSeekGap + PlayerDimens.ControlsHeightLandscape
    private val openTitleTop = coverY + (bigCover - groupHeight) / 2 - PlayerDimens.LandscapeTitleBlank
    private val openSeekTop = openTitleTop + PlayerDimens.LandscapeTitleHeight + PlayerDimens.LandscapeSeekPush
    private val openTransportTop = openSeekTop + PlayerDimens.SeekbarHeight + PlayerDimens.LandscapeTimesHeight + PlayerDimens.LandscapeSeekGap

    // With the lyrics or the queue open the smaller cover, the seekbar and the controls are one group too, around the same middle, with the name in the middle of the height of the cover
    private val panelGroupHeight = PlayerDimens.LandscapeSmallCover + PlayerDimens.LandscapePanelSeekGap + seekShown +
        PlayerDimens.LandscapePanelControlsGap + PlayerDimens.ControlsHeightLandscapePanel
    private val panelCoverY = coverY + (bigCover - panelGroupHeight) / 2
    private val panelTitleTop = panelCoverY + PlayerDimens.LandscapeSmallCover / 2 - PlayerDimens.LandscapeTitleCenterPanel
    private val panelSeekTop = panelCoverY + PlayerDimens.LandscapeSmallCover + (PlayerDimens.LandscapePanelSeekGap + seekShown + PlayerDimens.LandscapePanelControlsGap) / 2 - seekBlank
    private val panelTransportTop = panelCoverY + panelGroupHeight - PlayerDimens.ControlsHeightLandscapePanel
    private val openModesTop = openTransportTop + (PlayerDimens.ControlsHeightLandscape - PlayerDimens.LandscapeToolButton) / 2

    // The side the lyrics and the queue open in, the bar of the queue near the bottom of the screen so its list has the height
    private val songEnd = screenWidth * PlayerDimens.LandscapePanelFraction
    private val songStart = start + bigCover + PlayerDimens.LandscapeGap
    val sideStart = songEnd + PlayerDimens.LandscapeSideGap
    val sideEnd = max(right, PlayerDimens.LandscapeMargin)
    val queueBarBottom = max(bottomInset, PlayerDimens.LandscapeQueueBarBottom)
    val queueListBottom = queueBarBottom + PlayerDimens.LandscapeToolButton
    private val panelModesTop = screenHeight - queueListBottom

    fun coverSize(panel: Float): Dp = lerp(bigCover, PlayerDimens.LandscapeSmallCover, panel)
    fun coverTop(panel: Float): Dp = lerp(coverY, panelCoverY, panel)

    // The controls, bigger beside the big cover, and shuffle and repeat in the middle of their height there and on the line of the bar of the queue beside the smaller cover
    fun transportTop(panel: Float): Dp = lerp(openTransportTop, panelTransportTop, panel)
    fun transportHeight(panel: Float): Dp = lerp(PlayerDimens.ControlsHeightLandscape, PlayerDimens.ControlsHeightLandscapePanel, panel)
    fun modesTop(panel: Float): Dp = lerp(openModesTop, panelModesTop, panel)

    // The column of the song
    fun columnStart(panel: Float): Dp = lerp(songStart, start, panel)
    fun columnEnd(panel: Float): Dp = lerp(end, songEnd, panel)

    // The name of the song, it starts after the smaller cover and leaves room for share and favorite while they are beside it
    private val panelNameStart = start + PlayerDimens.LandscapeSmallCover + PlayerDimens.LandscapeSmallCoverGap
    fun titleStart(panel: Float): Dp = lerp(songStart, panelNameStart, panel)
    fun titleEnd(panel: Float): Dp = columnEnd(panel) - (PlayerDimens.SideActionsWidth + PlayerDimens.SideActionsMargin) * (1f - panel)
    fun titleTop(panel: Float): Dp = lerp(openTitleTop, panelTitleTop, panel)
    fun seekTop(panel: Float): Dp = lerp(openSeekTop, panelSeekTop, panel)

    // The pill of share and favorite, at the end of the line of the name beside the big cover
    private val actionsWidth = (PlayerDimens.SideActionsPaddingHorizontal + PlayerDimens.SideAction + PlayerDimens.SideActionsSpacing) * 2 + PlayerDimens.DividerWidth
    private val actionsHeight = PlayerDimens.SideAction + PlayerDimens.SideActionsPaddingVertical * 2
    val actionsY = openTitleTop + (PlayerDimens.LandscapeTitleBlank + PlayerDimens.LandscapeTitleHeight - actionsHeight) / 2
    fun actionsX(panel: Float): Dp = columnEnd(panel) - actionsWidth

    // The favorite travels from the heart of the pill, beside the big cover, to its place on the line of shuffle and repeat, beside the smaller one
    private val heartInPill = PlayerDimens.SideActionsPaddingHorizontal + PlayerDimens.SideAction + PlayerDimens.SideActionsSpacing * 2 +
        PlayerDimens.DividerWidth + PlayerDimens.SideAction / 2
    private val openFavoriteX = end - actionsWidth + heartInPill - PlayerDimens.LandscapeToolButton / 2
    private val openFavoriteTop = actionsY + actionsHeight / 2 - PlayerDimens.LandscapeToolButton / 2
    fun favoriteX(panel: Float): Dp = lerp(openFavoriteX, panelFavoriteX, panel)
    fun favoriteTop(panel: Float): Dp = lerp(openFavoriteTop, panelModesTop, panel)

    // Beside the big cover shuffle and repeat stay on the edges of the column with the controls between them
    // Beside the smaller cover the controls take the whole column, and shuffle, repeat and favorite go in sequence on the start of the line of the bar of the queue, in line with the songs
    private val modeRoom = (PlayerDimens.LandscapeToolButton - PlayerDimens.LandscapeModeIcon) / 2
    private val panelShuffleX = sideStart + PlayerDimens.LandscapeSideInset - modeRoom - PlayerDimens.LandscapeModeEdge
    private val panelFavoriteX = panelShuffleX + PlayerDimens.LandscapeToolButton * 2
    fun modesStart(panel: Float): Dp = lerp(songStart - modeRoom - PlayerDimens.LandscapeModeEdge, panelShuffleX, panel)
    fun modesEnd(panel: Float): Dp = lerp(end + modeRoom + PlayerDimens.LandscapeModeEdge, panelFavoriteX, panel)
    fun rowStart(panel: Float): Dp = lerp(songStart + PlayerDimens.LandscapeModesRoom, start, panel)
    fun rowWidth(panel: Float): Dp = lerp(end - songStart - PlayerDimens.LandscapeModesRoom * 2, songEnd - start, panel)
}

// How many buttons from the end the middle of the cast button is
private const val CastPlaceFromEnd = 1.5f

// The place of the open player lying down, from the room of the window without what the system takes of it
// The keyboard is left out, it goes over the player for a moment and the parts do not move for it
// So are the bars of the system, which in the immersive mode come and go and would make the parts jump, the margins already keep clear of them
@Composable
fun rememberLandscapeFrame(screenWidth: Dp, screenHeight: Dp): LandscapeFrame {
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val insets = WindowInsets.displayCutout
    val left = with(density) { insets.getLeft(density, direction).toDp() }
    val right = with(density) { insets.getRight(density, direction).toDp() }
    val top = with(density) { insets.getTop(density).toDp() }
    val bottom = with(density) { insets.getBottom(density).toDp() }

    return remember(screenWidth, screenHeight, left, right, top, bottom) {
        LandscapeFrame(screenWidth, screenHeight, left, right, top, bottom)
    }
}

// Puts what it is given at a place that changes while the player moves, read while measuring and placing so nothing is built again
// The anchors say which point of it goes on the place, zero for its start and one for its end
internal fun Modifier.placedAt(
    x: () -> Dp,
    y: () -> Dp,
    width: (() -> Dp)? = null,
    height: (() -> Dp)? = null,
    anchorX: Float = 0f,
    anchorY: Float = 0f
): Modifier = layout { measurable, constraints ->
    val fixedWidth = width?.invoke()?.roundToPx()?.coerceAtLeast(0)
    val fixedHeight = height?.invoke()?.roundToPx()?.coerceAtLeast(0)
    val placeable = measurable.measure(
        Constraints(
            minWidth = fixedWidth ?: 0,
            maxWidth = fixedWidth ?: constraints.maxWidth,
            minHeight = fixedHeight ?: 0,
            maxHeight = fixedHeight ?: constraints.maxHeight
        )
    )
    layout(constraints.maxWidth, constraints.maxHeight) {
        placeable.place(
            (x().toPx() - placeable.width * anchorX).roundToInt(),
            (y().toPx() - placeable.height * anchorY).roundToInt()
        )
    }
}

// The name over the side of the lyrics or of the queue, on the line of the tools and free of them
internal fun Modifier.sideHeader(frame: LandscapeFrame): Modifier = this
    .padding(start = PlayerDimens.LandscapeSideInset, top = frame.barTop, end = frame.toolsRoom)
    .height(PlayerDimens.LandscapeToolButton)

// What scrolls fades at the top and at the bottom of its place instead of being cut, this share of its height
internal fun Modifier.fadingEnds(fraction: Float): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                fraction to Color.Black,
                (1f - fraction) to Color.Black,
                1f to Color.Transparent
            ),
            blendMode = BlendMode.DstIn
        )
    }
