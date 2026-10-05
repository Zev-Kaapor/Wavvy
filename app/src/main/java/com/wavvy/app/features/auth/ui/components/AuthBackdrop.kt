package com.wavvy.app.features.auth.ui.components

// Android resources
import androidx.annotation.DrawableRes
// Compose layouts and foundations
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
// Material 3 components
import androidx.compose.material3.MaterialTheme
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
// Project resources
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.features.auth.ui.AuthDimens

// Photo that fades into the background with the content of the screen over it, at the bottom in portrait
// and on the other half in landscape, with an optional back button
@Composable
fun AuthBackdrop(
    @DrawableRes photo: Int,
    focusBias: Float,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val dimens = WavvyTheme.dimens
    val background = MaterialTheme.colorScheme.background

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(background)
    ) {
        if (maxWidth > maxHeight) {
            // Photo on the left fading to the right, the content on the other half
            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    AuthPhoto(photo = photo, focusBias = focusBias, modifier = Modifier.fillMaxSize())
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    AuthDimens.PhotoFadeStart to Color.Transparent,
                                    1f to background
                                )
                            )
                    )
                }

                AuthContent(
                    alignment = Alignment.Center,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    content = content
                )
            }
        } else {
            // Photo on the top fading to the background, the content at the bottom
            Box(modifier = Modifier.fillMaxSize()) {
                AuthPhoto(
                    photo = photo,
                    focusBias = focusBias,
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(AuthDimens.PhotoHeightFraction)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                AuthDimens.PhotoFadeStart to Color.Transparent,
                                AuthDimens.PhotoHeightFraction to background
                            )
                        )
                )

                AuthContent(
                    alignment = Alignment.BottomCenter,
                    modifier = Modifier.fillMaxSize(),
                    content = content
                )
            }
        }

        // Round back button at the top start, over the photo
        if (onBack != null) {
            RoundBackButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = dimens.screenPadding, vertical = dimens.spaceSmall)
            )
        }
    }
}

// Decorative photo, cropped around the face
@Composable
private fun AuthPhoto(
    @DrawableRes photo: Int,
    focusBias: Float,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(photo),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Crop,
        alignment = BiasAlignment(0f, focusBias)
    )
}

// Content centered in its area, kept clear of the cutout and the system bars
@Composable
private fun AuthContent(
    alignment: Alignment,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val dimens = WavvyTheme.dimens

    Box(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = dimens.screenPadding, vertical = dimens.spaceExtraLarge),
        contentAlignment = alignment
    ) {
        Column(
            modifier = Modifier.widthIn(max = AuthDimens.ContentMaxWidth),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}
