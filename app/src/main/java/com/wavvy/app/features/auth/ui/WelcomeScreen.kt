package com.wavvy.app.features.auth.ui

// Compose layouts and foundations
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
// Kotlin utilities
import kotlin.random.Random
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.GradientButton
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.features.auth.ui.components.AuthBackdrop

// First screen, a photo that fades into the background with the isologo, a curiosity and the start button
@Composable
fun WelcomeScreen(
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimens = WavvyTheme.dimens
    val factTitles = stringArrayResource(R.array.welcome_fact_titles)
    val facts = stringArrayResource(R.array.welcome_facts)

    // One curiosity per launch, kept while the screen turns
    val factIndex = rememberSaveable { Random.nextInt(facts.size) }

    AuthBackdrop(
        photo = R.drawable.auth_welcome,
        focusBias = AuthDimens.WelcomePhotoFocusBias,
        modifier = modifier
    ) {
        Image(
            painter = painterResource(R.drawable.wavvy_isologo_on_dark),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier
                .align(Alignment.Start)
                .height(AuthDimens.LogoHeight)
        )

        Spacer(modifier = Modifier.height(dimens.spaceMedium))

        Text(
            text = factTitles[factIndex],
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(dimens.spaceExtraSmall))

        Text(
            text = facts[factIndex],
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(dimens.spaceExtraLarge))

        GradientButton(text = stringResource(R.string.welcome_start), onClick = onStart)
    }
}
