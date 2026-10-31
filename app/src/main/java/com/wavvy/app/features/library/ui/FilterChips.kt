package com.wavvy.app.features.library.ui

// Compose animation
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
// Coroutines
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyMotion

// The cross before the filter that is on, which turns it off
@Composable
internal fun ClearChip(onClick: () -> Unit, modifier: Modifier = Modifier, height: Dp = LibraryDimens.ChipHeight) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(width = LibraryDimens.ClearWidth, height = height)
            .clip(RoundedCornerShape(LibraryDimens.ChipCorner))
            .background(MaterialTheme.colorScheme.onBackground)
            .clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = WavvyIcons.Close,
            contentDescription = stringResource(R.string.cd_close),
            tint = MaterialTheme.colorScheme.background,
            modifier = Modifier.size(LibraryDimens.ClearIcon)
        )
    }
}

// A row of filters that scrolls sideways and goes back to its start whenever the scroll key changes, so the one that was chosen is in sight at its new place
// Every filter is always in the row, even the ones off the screen, so each can slide from where it was
@Composable
internal fun FilterChipRow(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    scrollKey: Any? = null,
    content: @Composable RowScope.() -> Unit
) {
    val scrollState = rememberScrollState()
    LaunchedEffect(scrollKey) { scrollState.animateScrollTo(0) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .horizontalScroll(scrollState)
            .padding(contentPadding),
        content = content
    )
}

// The place of a filter in the row, it fades when it comes and goes and slides when the ones around it change, the gap is its own so a closed one leaves none behind
// A filter that goes keeps its width until it has faded, so what moves does not cross what is still there, and only then the room is closed
// The one that is not meant to fade out, such as the cross, leaves at once so the others close its room in the same move
@Composable
internal fun ChipSlot(gap: Dp, modifier: Modifier = Modifier, isVisible: Boolean = true, fadesOut: Boolean = true, content: @Composable () -> Unit) {
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(WavvyMotion.ExpandMillis),
        label = "ChipSlot"
    )

    if (isVisible || (fadesOut && alpha > 0f)) {
        Box(
            modifier = modifier
                .slideToPlace()
                .graphicsLayer { this.alpha = alpha }
                .padding(end = gap)
        ) { content() }
    }
}

// Where the last placement was, kept across recompositions without being read by them
private class PlaceMemory { var last: IntOffset? = null }

// Starts from where the item was and moves to where it is whenever its place in the row changes
@Composable
private fun Modifier.slideToPlace(): Modifier {
    val scope = rememberCoroutineScope()
    val shift = remember { Animatable(IntOffset.Zero, IntOffset.VectorConverter) }
    val memory = remember { PlaceMemory() }

    return this
        .onPlaced { coordinates ->
            val now = coordinates.positionInParent().round()
            val before = memory.last
            memory.last = now
            if (before != null && before != now) {
                scope.launch {
                    shift.snapTo(shift.value + before - now)
                    shift.animateTo(IntOffset.Zero, spring())
                }
            }
        }
        .offset { shift.value }
}

// The filters of a row from a list, with none on they are all there in their order, with one on it goes to the start with a cross before it that turns it off and the others come after it
@Composable
internal fun <T : Any> FilterChips(
    options: List<T>,
    selected: T?,
    id: (T) -> Any,
    gap: Dp,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    hasClear: Boolean = true,
    clearHeight: Dp = LibraryDimens.ChipHeight,
    leading: @Composable RowScope.() -> Unit = {},
    chip: @Composable (option: T, isOn: Boolean) -> Unit
) {
    val order = if (selected != null) listOf(selected) + options.filter { it != selected } else options

    FilterChipRow(modifier = modifier, contentPadding = contentPadding, scrollKey = selected) {
        leading()

        if (hasClear) {
            ChipSlot(gap = gap, isVisible = selected != null, fadesOut = false) { ClearChip(onClick = onClear, height = clearHeight) }
        }

        order.forEach { option ->
            key(id(option)) { ChipSlot(gap = gap) { chip(option, option == selected) } }
        }
    }
}
