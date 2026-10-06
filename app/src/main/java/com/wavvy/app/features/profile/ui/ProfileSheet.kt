package com.wavvy.app.features.profile.ui

// Compose animation
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.ZeroCornerSize
// Material 3 components
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
// Kotlin utilities and coroutines
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.clickableNoIndication
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyMotion
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.features.profile.ui.components.ProfileAvatar

// Menu of the account that rises from the bottom over the app, closed by the dimmed area, the back button or dragging down
@Composable
fun ProfileSheet(
    visible: Boolean,
    profile: Profile,
    onDismiss: () -> Unit,
    onSignOut: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = visible, onBack = onDismiss)

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(WavvyMotion.SheetMillis)),
            exit = fadeOut(tween(WavvyMotion.SheetMillis))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = ProfileDimens.ScrimAlpha))
                    .clickableNoIndication(onClick = onDismiss)
            )
        }

        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(WavvyMotion.SheetMillis)) { it },
            exit = slideOutVertically(tween(WavvyMotion.SheetMillis)) { it }
        ) {
            SheetContent(
                profile = profile,
                onDismiss = onDismiss,
                onSignOut = onSignOut,
                onSignIn = onSignIn
            )
        }
    }
}

// Surface of the menu with the account on top, a line and the list below
@Composable
private fun SheetContent(
    profile: Profile,
    onDismiss: () -> Unit,
    onSignOut: () -> Unit,
    onSignIn: () -> Unit
) {
    val dimens = WavvyTheme.dimens
    val scope = rememberCoroutineScope()
    val dismissDistance = with(LocalDensity.current) { ProfileDimens.DismissDistance.toPx() }
    val dragged = remember { Animatable(0f) }
    val shape = MaterialTheme.shapes.extraLarge.copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize)

    Column(
        modifier = Modifier
            .widthIn(max = ProfileDimens.MenuMaxWidth)
            .fillMaxWidth()
            .offset { IntOffset(0, dragged.value.roundToInt()) }
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    scope.launch { dragged.snapTo((dragged.value + delta).coerceAtLeast(0f)) }
                },
                onDragStopped = {
                    if (dragged.value > dismissDistance) onDismiss() else dragged.animateTo(0f)
                }
            )
            .clip(shape)
            .background(WavvyTheme.colors.elevated)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
            .padding(bottom = dimens.spaceLarge),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Handle that shows the menu can be dragged
        Box(
            modifier = Modifier
                .padding(vertical = dimens.spaceMedium)
                .size(width = ProfileDimens.HandleWidth, height = ProfileDimens.HandleHeight)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outline)
        )

        AccountHeader(profile = profile, onSignOut = onSignOut, onSignIn = onSignIn)

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = dimens.screenPadding, vertical = dimens.spaceMedium),
            thickness = dimens.hairline,
            color = MaterialTheme.colorScheme.outline
        )

        // These screens do not exist yet, so the items only close the menu
        MenuItem(icon = WavvyIcons.Person, label = stringResource(R.string.profile_your_profile), onClick = onDismiss)
        MenuItem(icon = WavvyIcons.Integrations, label = stringResource(R.string.profile_integrations), onClick = onDismiss)
        MenuItem(icon = WavvyIcons.Settings, label = stringResource(R.string.profile_settings), onClick = onDismiss)
    }
}

// Photo, name and user name, with the sign out icon at the end, or the sign in icon for a guest
@Composable
private fun AccountHeader(
    profile: Profile,
    onSignOut: () -> Unit,
    onSignIn: () -> Unit
) {
    val dimens = WavvyTheme.dimens

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.screenPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium)
    ) {
        ProfileAvatar(photo = profile.photo, size = ProfileDimens.MenuAvatar)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (profile.isSignedIn) profile.name.orEmpty() else stringResource(R.string.profile_guest),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // The user name for an account, an invitation to sign in for a guest
            val subtitle = if (profile.isSignedIn) profile.handle else stringResource(R.string.profile_guest_hint)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Sign out in red for an account, sign in for a guest
        val signedIn = profile.isSignedIn
        Box(
            modifier = Modifier
                .clickableNoIndication(onClick = if (signedIn) onSignOut else onSignIn)
                .minimumInteractiveComponentSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (signedIn) WavvyIcons.SignOut else WavvyIcons.SignIn,
                contentDescription = stringResource(if (signedIn) R.string.cd_sign_out else R.string.cd_sign_in),
                tint = if (signedIn) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(ProfileDimens.ActionIcon)
            )
        }
    }
}

// Row of the list with an icon and a name
@Composable
private fun MenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val dimens = WavvyTheme.dimens

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ProfileDimens.ItemHeight)
            .clickableNoIndication(onClick = onClick)
            .padding(horizontal = dimens.screenPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceLarge)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(ProfileDimens.ItemIcon)
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
