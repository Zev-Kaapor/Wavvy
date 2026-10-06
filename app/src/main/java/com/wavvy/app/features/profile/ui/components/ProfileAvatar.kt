package com.wavvy.app.features.profile.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.features.profile.ui.ProfileDimens

// Round avatar with the gradient ring, the photo of the account or the person icon when there is none
@Composable
fun ProfileAvatar(
    photo: ImageBitmap?,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val colors = WavvyTheme.colors
    val ring = Brush.linearGradient(
        colors = listOf(colors.gradientStart, colors.gradientMiddle, colors.gradientEnd),
        start = Offset(0f, Float.POSITIVE_INFINITY),
        end = Offset(Float.POSITIVE_INFINITY, 0f)
    )

    Box(
        modifier = modifier
            .size(size)
            .border(ProfileDimens.AvatarRing, ring, CircleShape)
            .padding(ProfileDimens.AvatarRing + ProfileDimens.AvatarRingGap)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = ProfileDimens.AvatarContainerAlpha)),
        contentAlignment = Alignment.Center
    ) {
        if (photo != null) {
            Image(
                bitmap = photo,
                contentDescription = stringResource(R.string.cd_profile),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = WavvyIcons.Person,
                contentDescription = stringResource(R.string.cd_profile),
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(size * ProfileDimens.AvatarIconFraction)
            )
        }
    }
}
