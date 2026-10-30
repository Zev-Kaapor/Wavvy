package com.wavvy.app.core.designsystem.components

// Compose layouts and foundations
import androidx.compose.foundation.lazy.LazyListState
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow

// How many items at the start of a list are added up to know how far it was scrolled, past them the color is gone
private const val MaxLeadingItems = 16

// How much of the color on top of a page is left, from one to none, as the list goes down, so it fades with the scroll and does not go off when an item leaves
// It is counted by how many pixels the list was scrolled in all, the items that already left added to what is left of the one on top, and not by the first item that shows
// so a short item or an empty one at the start cannot end the fade before its time, the distance is how many pixels it takes to be gone
@Composable
fun rememberBackdropFade(listState: LazyListState, distance: () -> Float): () -> Float {
    // The height of each item at the start, kept as they show, so the ones that left are known
    val sizes = remember(listState) { HashMap<Int, Int>() }
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.map { it.index to it.size } }.collect { items -> items.forEach { (index, size) -> sizes[index] = size } }
    }

    return fade@{
        val total = distance()
        val first = listState.firstVisibleItemIndex
        if (total <= 0f || first > MaxLeadingItems) return@fade 0f

        var scrolled = listState.firstVisibleItemScrollOffset.toFloat()
        for (index in 0 until first) scrolled += sizes[index] ?: return@fade 0f
        (1f - scrolled / total).coerceIn(0f, 1f)
    }
}
