package com.wavvy.app.features.playlist.ui

// Android picker
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
// Compose layouts and foundations
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
// Image loading
import coil3.compose.AsyncImage
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.features.menu.ItemMenuDimens
import com.wavvy.app.features.playlist.data.PlaylistCover

// The cover of a playlist in the shape YouTube Music keeps, 16:9, with a dashed square in the middle that shows what the app cuts from it
// A tap opens the pictures of the device, the one that is chosen shows at once
@Composable
internal fun CoverPicker(pickedUri: Uri?, currentUrl: String?, onPicked: (Uri) -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(onPicked) }
    val outline = MaterialTheme.colorScheme.onSurface

    Column(modifier = Modifier.padding(horizontal = ItemMenuDimens.Side, vertical = PlaylistDimens.FieldVertical)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(PlaylistCover.RatioWidth / PlaylistCover.RatioHeight)
                .clip(RoundedCornerShape(PlaylistDimens.CoverCorner))
                .background(WavvyTheme.colors.chip)
                .clickable { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
        ) {
            val model: Any? = pickedUri ?: currentUrl
            if (model != null) {
                AsyncImage(model = model, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().aspectRatio(PlaylistCover.RatioWidth / PlaylistCover.RatioHeight))
            }

            // The camera in the middle of the square tells that the cover is changed there, on a dark round so it shows over any picture
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(PlaylistDimens.CoverCamera)
                    .clip(CircleShape)
                    .background(WavvyTheme.colors.tileScrim.copy(alpha = PlaylistDimens.CoverCameraAlpha))
            ) {
                Icon(imageVector = WavvyIcons.Camera, contentDescription = stringResource(R.string.playlist_cover_pick), tint = Color.White)
            }

            // What to do, on a dark corner at the bottom of the cover as the pin of the speed dial is
            Text(
                text = stringResource(R.string.playlist_cover_note),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(PlaylistDimens.CoverNoteInset)
                    .clip(RoundedCornerShape(PlaylistDimens.RowBadgeCorner))
                    .background(WavvyTheme.colors.tileScrim.copy(alpha = PlaylistDimens.RowBadgeAlpha))
                    .padding(horizontal = PlaylistDimens.CoverNotePaddingX, vertical = PlaylistDimens.CoverNotePaddingY)
            )

            // The square the app shows, as high as the cover, in the middle of it
            Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(PlaylistCover.RatioWidth / PlaylistCover.RatioHeight)) {
                val side = size.height
                drawRoundRect(
                    color = outline,
                    topLeft = Offset((size.width - side) / 2f, 0f),
                    size = Size(side, side),
                    cornerRadius = CornerRadius(PlaylistDimens.CoverSquareCorner.toPx()),
                    style = Stroke(
                        width = PlaylistDimens.CoverStroke.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(PlaylistDimens.CoverDash.toPx(), PlaylistDimens.CoverDash.toPx()))
                    )
                )
            }
        }
    }
}
