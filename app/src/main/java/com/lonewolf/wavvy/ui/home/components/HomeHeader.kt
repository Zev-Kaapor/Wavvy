package com.lonewolf.wavvy.ui.home.components

// Compose layouts and foundations
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
// Material 3 components
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.data.SavedAccount
import com.lonewolf.wavvy.ui.common.components.ProfileDropdown
import com.lonewolf.wavvy.ui.theme.Poppins

// Home screen header
@Composable
fun HomeHeader(
    modifier: Modifier = Modifier,
    isAuthenticated: Boolean,
    userName: String?,
    userHandle: String?,
    userProfilePicture: String?,
    onNavigateToSettings: () -> Unit,
    onLoginClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onSwitchAccount: () -> Unit = {},
    onAccountSelected: (SavedAccount) -> Unit = {},
    onDismissAccountSwitcher: () -> Unit = {},
    savedAccounts: List<SavedAccount> = emptyList(),
    showAccountSwitcher: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()

    // Visual state configuration
    val logoColor = if (isDark) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    val logoShadow = if (isDark) Shadow(color = logoColor.copy(alpha = 0.4f), blurRadius = 15f) else null

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 36.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // App logo
        Text(
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        color = logoColor,
                        fontWeight = FontWeight.Black,
                        shadow = logoShadow
                    )
                ) {
                    append(stringResource(R.string.logo_Home))
                }
            },
            fontFamily = Poppins,
            fontSize = 26.sp,
            letterSpacing = (-0.5).sp
        )

        // Profile section
        Box {
            // Profile button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    .clickable { expanded = true },
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = userProfilePicture,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(600)) togetherWith fadeOut(animationSpec = tween(600))
                    },
                    label = "profile_picture_transition"
                ) { picture ->
                    if (isAuthenticated && !picture.isNullOrBlank()) {
                        AsyncImage(
                            model = picture,
                            contentDescription = stringResource(R.string.cd_profile_button),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = stringResource(R.string.cd_profile_button),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Profile menu
            ProfileDropdown(
                expanded = expanded,
                isAuthenticated = isAuthenticated,
                userName = userName,
                userEmail = userHandle,
                userProfilePicture = userProfilePicture,
                savedAccounts = savedAccounts,
                showAccountSwitcher = showAccountSwitcher,
                onDismiss = { expanded = false },
                onNavigateToLogin = onLoginClick,
                onSignOut = onSignOutClick,
                onNavigateToSettings = onNavigateToSettings,
                onSwitchAccount = onSwitchAccount,
                onAccountSelected = onAccountSelected,
                onDismissAccountSwitcher = onDismissAccountSwitcher
            )
        }
    }
}
