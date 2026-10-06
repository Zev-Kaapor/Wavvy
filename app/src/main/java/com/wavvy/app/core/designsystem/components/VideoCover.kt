package com.wavvy.app.core.designsystem.components

// Android graphics
import android.graphics.Bitmap
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.Icon
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
// Image loading
import coil3.size.Size
import coil3.transform.Transformation
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme

// Wide side over the short side of a video picture
private const val VideoAspect = 16f / 9f

// Corner and darkness of the badge behind the camera
private val BadgeCorner = 4.dp
private const val BadgeBackgroundAlpha = 0.55f

// Cuts the square in the middle of a video picture, leaving out the black bars of the smaller pictures, so videos look like covers
object VideoSquareCrop : Transformation() {
    override val cacheKey: String = "video_square_crop"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        // The picture of the video fills the whole width, so its height is the width over the video shape
        val side = minOf(input.height, (input.width / VideoAspect).toInt()).coerceAtLeast(1)
        return Bitmap.createBitmap(input, (input.width - side) / 2, (input.height - side) / 2, side, side)
    }
}

// Camera on a dark corner that tells a video from a song on its cover
@Composable
fun VideoBadge(
    iconSize: Dp,
    padding: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(BadgeCorner))
            .background(WavvyTheme.colors.tileScrim.copy(alpha = BadgeBackgroundAlpha))
            .padding(padding)
    ) {
        Icon(
            imageVector = WavvyIcons.VideoCamera,
            contentDescription = stringResource(R.string.cd_video),
            tint = WavvyTheme.colors.videoBadge,
            modifier = Modifier.size(iconSize)
        )
    }
}
