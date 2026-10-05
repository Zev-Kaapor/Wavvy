package com.wavvy.app.features.auth.ui

// Compose layouts and foundations
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.GradientButton
import com.wavvy.app.core.designsystem.components.QuietButton
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.features.auth.ui.components.AuthBackdrop
import com.wavvy.app.features.auth.ui.components.GoogleBadge

// Second screen, a photo with only the title and the two ways in at the bottom
@Composable
fun LoginScreen(
    onGoogleClick: () -> Unit,
    onSkipClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimens = WavvyTheme.dimens

    AuthBackdrop(
        photo = R.drawable.auth_login,
        focusBias = AuthDimens.LoginPhotoFocusBias,
        modifier = modifier,
        onBack = onBackClick
    ) {
        Text(
            text = stringResource(R.string.login_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(dimens.spaceExtraSmall))

        Text(
            text = stringResource(R.string.login_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(dimens.spaceLarge))

        GradientButton(
            text = stringResource(R.string.login_google),
            onClick = onGoogleClick,
            leading = { GoogleBadge() }
        )

        Spacer(modifier = Modifier.height(dimens.spaceSmall))

        QuietButton(
            text = stringResource(R.string.login_skip),
            onClick = onSkipClick,
            leading = {
                Box(
                    modifier = Modifier.size(AuthDimens.GoogleBadge),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = WavvyIcons.Login,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(AuthDimens.GuestIcon)
                    )
                }
            }
        )
    }
}
