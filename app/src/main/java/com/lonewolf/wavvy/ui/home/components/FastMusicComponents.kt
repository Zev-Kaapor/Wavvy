package com.lonewolf.wavvy.ui.home.components

// Compose foundation and layout
import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyGridLayoutInfo
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 and icons
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
// State and UI tools
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.data.models.QuickPick
import com.lonewolf.wavvy.data.resize
import com.lonewolf.wavvy.ui.common.components.sections.SectionTitle
import com.lonewolf.wavvy.ui.common.components.sheets.SongOptionsBottomSheet
import com.lonewolf.wavvy.ui.theme.Poppins

// Fast music section
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FastMusicGrid(
    quickPicks: List<QuickPick>,
    isLoading: Boolean,
    onItemClick: (QuickPick) -> Unit,
    onPlayAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMusicForOptions by remember { mutableStateOf<QuickPick?>(null) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Adaptive grid rows and height based on orientation
    val gridRows = if (isLandscape) 3 else 4
    val gridHeight = if (isLandscape) 190.dp else 250.dp
    val skeletonCount = if (isLandscape) 9 else 10

    // Fallback data tracking for unauthenticated contexts
    val showSkeleton = quickPicks.isEmpty()
    val gridState = rememberLazyGridState()

    // Snap management
    val snapLayoutInfoProvider = remember(gridState) {
        object : SnapLayoutInfoProvider {
            private val layoutInfo: LazyGridLayoutInfo
                get() = gridState.layoutInfo

            override fun calculateApproachOffset(velocity: Float, decayOffset: Float): Float = 0f

            override fun calculateSnapOffset(velocity: Float): Float {
                val bounds = calculateSnappingOffsetBounds()
                return when {
                    velocity < 0 -> bounds.start
                    velocity > 0 -> bounds.endInclusive
                    else -> 0f
                }
            }

            private fun calculateSnappingOffsetBounds(): ClosedFloatingPointRange<Float> {
                var lowerBoundOffset = Float.NEGATIVE_INFINITY
                var upperBoundOffset = Float.POSITIVE_INFINITY

                layoutInfo.visibleItemsInfo.forEach { item ->
                    val offset = calculateDistanceToDesiredSnapPosition(item)

                    if (offset <= 0 && offset > lowerBoundOffset) {
                        lowerBoundOffset = offset
                    }

                    if (offset >= 0 && offset < upperBoundOffset) {
                        upperBoundOffset = offset
                    }
                }

                return lowerBoundOffset.rangeTo(upperBoundOffset)
            }

            private fun calculateDistanceToDesiredSnapPosition(item: LazyGridItemInfo): Float {
                val containerSize = layoutInfo.viewportSize.width - layoutInfo.beforeContentPadding - layoutInfo.afterContentPadding
                val desiredDistance = (containerSize / 2f - item.size.width / 2f)
                val itemCurrentPosition = item.offset.x.toFloat()

                return itemCurrentPosition - desiredDistance
            }
        }
    }
    val flingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider)

    Column(modifier = modifier.fillMaxWidth()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SectionTitle(text = stringResource(R.string.section_title_fast_choices))

            OutlinedButton(
                onClick = onPlayAllClick,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text(
                    text = stringResource(R.string.cd_play_all),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        // Horizontal grid
        LazyHorizontalGrid(
            rows = GridCells.Fixed(gridRows),
            state = gridState,
            modifier = Modifier
                .height(gridHeight)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            flingBehavior = flingBehavior
        ) {
            if (showSkeleton) {
                items(
                    count = skeletonCount,
                    key = { index: Int -> "fast_skeleton_$index" }
                ) {
                    FastMusicCard(
                        title = null,
                        artists = null,
                        thumbnailUrl = null,
                        isSkeleton = true,
                        isLoading = false,
                        onClick = { },
                        onMenuAction = { }
                    )
                }
            } else {
                items(
                    count = quickPicks.size,
                    key = { index: Int -> quickPicks[index].videoId }
                ) { index ->
                    val pick = quickPicks[index]
                    FastMusicCard(
                        title = pick.title,
                        artists = pick.artists,
                        thumbnailUrl = pick.thumbnailUrl,
                        isSkeleton = false,
                        isLoading = isLoading,
                        onClick = { onItemClick(pick) },
                        onMenuAction = { selectedMusicForOptions = pick }
                    )
                }
            }
        }
    }

    // Options sheet
    selectedMusicForOptions?.let { pick ->
        val fallbackArtist = stringResource(R.string.default_artist_name)
        val cleanArtistsList = remember(pick.artists) {
            pick.artists.map { it.trim() }.filter { it.isNotBlank() }
        }

        SongOptionsBottomSheet(
            songTitle = pick.title,
            artistNames = if (cleanArtistsList.isNotEmpty()) cleanArtistsList else listOf(fallbackArtist),
            thumbnailUrl = pick.thumbnailUrl,
            isSimplified = true,
            onDismiss = { selectedMusicForOptions = null },
            onActionClick = { _ -> selectedMusicForOptions = null }
        )
    }
}

// Music item card
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FastMusicCard(
    title: String?,
    artists: List<String>?,
    thumbnailUrl: String?,
    onClick: () -> Unit,
    onMenuAction: () -> Unit,
    modifier: Modifier = Modifier,
    isSkeleton: Boolean = false,
    isLoading: Boolean = false
) {
    val containerColor = MaterialTheme.colorScheme.surfaceVariant

    var imageLoaded by remember(thumbnailUrl) { mutableStateOf(false) }
    val textLoaded by remember { mutableStateOf(!isSkeleton) }

    val imageAlpha by animateFloatAsState(
        targetValue = if (isLoading) 0f else if (imageLoaded || isSkeleton) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "image_fade"
    )

    val textAlpha by animateFloatAsState(
        targetValue = if (isLoading) 0f else if (textLoaded) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "text_fade"
    )

    Row(
        modifier = modifier
            .width(280.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor.copy(alpha = 0.3f))
            // Clicks enabled even in skeleton mode
            .combinedClickable(
                onClick = onClick,
                onLongClick = onMenuAction
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Image artwork
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f)
                .background(containerColor)
                .graphicsLayer(alpha = imageAlpha)
        ) {
            if (!isSkeleton && !thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = thumbnailUrl.resize(width = 160, height = 160),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    onSuccess = { imageLoaded = true },
                    onError = { imageLoaded = true }
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Text details
        Column(
            modifier = Modifier
                .weight(1f)
                .graphicsLayer(alpha = textAlpha)
        ) {
            if (isSkeleton) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(containerColor)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.35f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(containerColor.copy(alpha = 0.5f))
                )
            } else {
                Text(
                    text = title.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = Poppins,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val cleanArtistsList = remember(artists) {
                    artists?.map { it.trim() }?.filter { it.isNotBlank() }.orEmpty()
                }

                val artistText = if (cleanArtistsList.isNotEmpty()) {
                    cleanArtistsList.joinToString(", ")
                } else {
                    stringResource(R.string.default_artist_name)
                }

                Text(
                    text = artistText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = Poppins,
                        fontSize = 12.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Action menu
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(0.dp, 8.dp, 8.dp, 0.dp))
                .combinedClickable(
                    onClick = onMenuAction
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
