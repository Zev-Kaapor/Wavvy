package com.wavvy.app.features.podcast.ui

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
// Time
import kotlinx.coroutines.delay
// Project resources
import com.wavvy.app.R

// The time that is left for a moment, in milliseconds, ticking each second until it comes, none when there is no moment to wait for
@Composable
internal fun rememberRemaining(at: Long?): Long? {
    val now by produceState(System.currentTimeMillis(), at) {
        if (at == null) return@produceState
        while (System.currentTimeMillis() < at) {
            delay(EpisodeDimens.CountdownTickMillis)
            value = System.currentTimeMillis()
        }
        value = System.currentTimeMillis()
    }
    return at?.let { it - now }
}

// The time that is left with every unit from the biggest that is not empty, such as 1d 12h 12m 05s, the ones after the first always have two digits so it does not move as it ticks
@Composable
internal fun countdownText(remainingMs: Long): String {
    val total = (remainingMs / MillisPerSecond).coerceAtLeast(0L)
    val days = total / SecondsPerDay
    val hours = total % SecondsPerDay / SecondsPerHour
    val minutes = total % SecondsPerHour / SecondsPerMinute
    val seconds = total % SecondsPerMinute

    return when {
        days > 0 -> stringResource(R.string.episode_countdown_days, days, hours, minutes, seconds)
        hours > 0 -> stringResource(R.string.episode_countdown_hours, hours, minutes, seconds)
        minutes > 0 -> stringResource(R.string.episode_countdown_minutes, minutes, seconds)
        else -> stringResource(R.string.episode_countdown_seconds, seconds)
    }
}

// A wide pill with only the time that is left, or the words that say it is soon when the moment is not known
@Composable
internal fun CountdownPill(remaining: Long?, height: Dp, container: Color, content: Color, textStyle: TextStyle, paddingX: Dp, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(height)
            .clip(CircleShape)
            .background(container)
            .defaultMinSize(minWidth = height)
            .padding(horizontal = paddingX)
    ) {
        Text(
            text = remaining?.let { countdownText(it) } ?: stringResource(R.string.episode_soon),
            style = MaterialTheme.typography.bodyMedium.merge(textStyle),
            color = content,
            maxLines = 1
        )
    }
}

// The seconds in each unit of time, and the milliseconds in a second
private const val MillisPerSecond = 1000L
private const val SecondsPerMinute = 60L
private const val SecondsPerHour = 3600L
private const val SecondsPerDay = 86400L
