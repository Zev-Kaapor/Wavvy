package com.lonewolf.wavvy.ui.player.components

// Android and Intent
import android.content.Context
import android.content.Intent
import android.widget.Toast
// Compose animation and core
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
// Compose foundation and layout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
// Material 3 and icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
// Material 3 components
import androidx.compose.material3.*
// State and UI tools
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.theme.accentCyan

// Side action panel for quick song management
@Composable
fun SongSideActions(
    songUrl: String?,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Fixed colors for player context (always dark theme style)
    val pillBackgroundColor = Color.White.copy(alpha = 0.12f)
    val iconTint = MaterialTheme.accentCyan
    val dividerColor = Color.White.copy(alpha = 0.15f)

    // Pill container for actions
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(pillBackgroundColor)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Share action
        AnimatedActionIcon(
            onClick = { shareMusic(context, songUrl) }
        ) { mod ->
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Share",
                tint = iconTint,
                modifier = mod.size(20.dp)
            )
        }

        // Divider line
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(16.dp)
                .background(dividerColor)
        )

        // Favorite toggle action
        AnimatedActionIcon(
            onClick = onFavoriteClick
        ) { mod ->
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "Favorite",
                tint = iconTint,
                modifier = mod.size(22.dp)
            )
        }
    }
}

// Reusable animated icon box for tactile feedback
@Composable
private fun AnimatedActionIcon(
    onClick: () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Smooth spring scale animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "IconScale"
    )

    // Interactive icon container
    Box(
        modifier = Modifier
            .size(32.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content(Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        })
    }
}

// Logic to share music URL via system intent
private fun shareMusic(context: Context, url: String?) {
    if (url.isNullOrEmpty()) {
        Toast.makeText(
            context,
            context.getString(R.string.error_share_empty),
            Toast.LENGTH_SHORT
        ).show()
        return
    }

    // Launch share intent
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, url)
    }
    context.startActivity(Intent.createChooser(intent, "Share via"))
}
