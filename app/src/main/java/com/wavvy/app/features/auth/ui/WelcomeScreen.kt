package com.wavvy.app.features.auth.ui

// Compose animation
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
// Compose layouts and foundations
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
// Kotlin utilities
import kotlin.random.Random
import kotlinx.coroutines.delay
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.GradientButton
import com.wavvy.app.core.designsystem.components.QuietButton
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyMotion
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.features.auth.ui.components.AuthBackdrop
import com.wavvy.app.features.auth.ui.components.GoogleBadge

// First screen, a photo that fades into the background with the isologo, a curiosity that changes by itself and the two ways in
@Composable
fun WelcomeScreen(
    onGoogleClick: () -> Unit,
    onSkipClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimens = WavvyTheme.dimens
    val factTitles = stringArrayResource(R.array.welcome_fact_titles)
    val facts = stringArrayResource(R.array.welcome_facts)

    // Starts on a random curiosity and keeps the place while the screen turns
    var factIndex by rememberSaveable { mutableIntStateOf(Random.nextInt(facts.size)) }

    // Next curiosity after a few seconds
    LaunchedEffect(facts.size) {
        while (true) {
            delay(WavvyMotion.WelcomeFactMillis.toLong())
            factIndex = (factIndex + 1) % facts.size
        }
    }

    AuthBackdrop(
        photo = R.drawable.auth_welcome,
        focusBias = AuthDimens.WelcomePhotoFocusBias,
        modifier = modifier
    ) {
        Image(
            painter = painterResource(R.drawable.wavvy_isologo),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier
                .align(Alignment.Start)
                .height(AuthDimens.LogoHeight)
        )

        Spacer(modifier = Modifier.height(dimens.spaceMedium))

        // The title and the text fade together into the next curiosity
        AnimatedContent(
            targetState = factIndex,
            transitionSpec = {
                fadeIn(tween(WavvyMotion.WelcomeFactFadeMillis)) togetherWith
                    fadeOut(tween(WavvyMotion.WelcomeFactFadeMillis))
            },
            modifier = Modifier.fillMaxWidth(),
            label = "WelcomeFact"
        ) { index ->
            Column {
                Text(
                    text = factTitles[index],
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(dimens.spaceExtraSmall))

                Text(
                    text = facts[index],
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(dimens.spaceExtraLarge))

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
                        imageVector = WavvyIcons.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(AuthDimens.GuestIcon)
                    )
                }
            }
        )
    }
}
