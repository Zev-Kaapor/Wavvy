package com.wavvy.app.features.home.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.clickableNoIndication
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.features.profile.ui.LocalProfile
import com.wavvy.app.features.profile.ui.components.ProfileAvatar

// Isologo on the left, notifications and profile on the right, kept below the camera cutout
@Composable
fun HomeHeader(
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimens = WavvyTheme.dimens
    val profile = LocalProfile.current
    val isologo = if (WavvyTheme.isDark) R.drawable.wavvy_isologo_on_dark else R.drawable.wavvy_isologo_on_light

    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(horizontal = dimens.screenPadding, vertical = dimens.spaceMedium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Image(
            painter = painterResource(isologo),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier.height(HomeDimens.LogoHeight)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall)
        ) {
            // Bare bell, the touch area is larger than the icon so it is easy to hit
            Box(
                modifier = Modifier
                    .clickableNoIndication(onClick = onNotificationsClick)
                    .minimumInteractiveComponentSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = WavvyIcons.Bell,
                    contentDescription = stringResource(R.string.cd_notifications),
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(HomeDimens.BellIcon)
                )
            }

            // The touch area is larger than the button, so it is easy to hit
            ProfileAvatar(
                photo = profile.photo,
                size = HomeDimens.ProfileButton,
                modifier = Modifier
                    .clickableNoIndication(onClick = onProfileClick)
                    .minimumInteractiveComponentSize()
            )
        }
    }
}
