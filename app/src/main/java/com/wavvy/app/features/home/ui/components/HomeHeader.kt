package com.wavvy.app.features.home.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.clickableNoIndication
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.network.Connection
import com.wavvy.app.core.network.rememberConnection
import com.wavvy.app.core.history.PlayHistory
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
        // The isologo and, at its top end, how the device is connected
        Row(verticalAlignment = Alignment.Top) {
            Image(
                painter = painterResource(isologo),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.height(HomeDimens.LogoHeight)
            )

            val connection = rememberConnection()
            Icon(
                imageVector = when (connection) {
                    Connection.Wifi -> WavvyIcons.Wifi
                    Connection.Cellular -> WavvyIcons.Cellular
                    Connection.Offline -> WavvyIcons.WifiOff
                },
                contentDescription = stringResource(
                    when (connection) {
                        Connection.Wifi -> R.string.cd_wifi
                        Connection.Cellular -> R.string.cd_cellular
                        Connection.Offline -> R.string.cd_offline
                    }
                ),
                tint = if (connection == Connection.Offline) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onBackground.copy(alpha = HomeDimens.ConnectionAlpha)
                },
                modifier = Modifier
                    .padding(start = HomeDimens.ConnectionGap)
                    .size(HomeDimens.ConnectionIcon)
            )
        }

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

                // The number of news about the artists the user follows that were not read
                val context = LocalContext.current
                val unseen by remember { PlayHistory.unreadReleases(context) }.collectAsState(initial = 0)

                if (unseen > 0) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(x = HomeDimens.BellBadgeOffsetX, y = HomeDimens.BellBadgeOffsetY)
                            .defaultMinSize(minWidth = HomeDimens.BellBadgeSize, minHeight = HomeDimens.BellBadgeSize)
                            .background(MaterialTheme.colorScheme.error, CircleShape)
                            .padding(horizontal = HomeDimens.BellBadgePadding)
                    ) {
                        Text(
                            text = unseen.toString(),
                            style = HomeType.BellBadge,
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                }
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
