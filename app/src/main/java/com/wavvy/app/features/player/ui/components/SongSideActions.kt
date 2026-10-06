package com.wavvy.app.features.player.ui.components

// Android context, intents and toasts
import android.content.Context
import android.content.Intent
import android.widget.Toast
// Compose animation
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
// Material 3 components
import androidx.compose.material3.Icon
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.innertube.MusicOrigin

// Pill next to the title with share and favorite, always in the colors of the dark player, as in the old Wavvy
@Composable
fun SongSideActions(
    songId: String?,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = WavvyTheme.colors

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(colors.onMedia.copy(alpha = PlayerDimens.SideActionsBackgroundAlpha))
            .padding(horizontal = PlayerDimens.SideActionsPaddingHorizontal, vertical = PlayerDimens.SideActionsPaddingVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PlayerDimens.SideActionsSpacing)
    ) {
        AnimatedActionIcon(onClick = { shareSong(context, songId) }) { scale ->
            Icon(
                imageVector = WavvyIcons.Share,
                contentDescription = stringResource(R.string.player_share),
                tint = colors.playerAccent,
                modifier = scale.size(PlayerDimens.ShareIcon)
            )
        }

        Box(
            modifier = Modifier
                .width(PlayerDimens.DividerWidth)
                .height(PlayerDimens.DividerHeight)
                .background(colors.onMedia.copy(alpha = PlayerDimens.DividerAlpha))
        )

        AnimatedActionIcon(onClick = onFavoriteClick) { scale ->
            Icon(
                imageVector = if (isFavorite) WavvyIcons.Favorite else WavvyIcons.FavoriteBorder,
                contentDescription = stringResource(R.string.player_favorite),
                tint = if (isFavorite) colors.playerLiked else colors.playerAccent,
                modifier = scale.size(PlayerDimens.FavoriteIcon)
            )
        }
    }
}

// Icon that shrinks a little while pressed
@Composable
private fun AnimatedActionIcon(
    onClick: () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) PlayerDimens.PressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "IconScale"
    )

    Box(
        modifier = Modifier
            .size(PlayerDimens.SideAction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content(
            Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
        )
    }
}

// Shares the YouTube Music link of the song through the apps of the device
internal fun shareSong(context: Context, songId: String?) {
    if (songId.isNullOrEmpty()) {
        Toast.makeText(context, context.getString(R.string.player_share_empty), Toast.LENGTH_SHORT).show()
        return
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "$MusicOrigin/watch?v=$songId")
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.player_share_via)))
}
