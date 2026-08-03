package com.wavvy.app.core.designsystem.components

// Android resource configuration
import android.content.res.Configuration
// Compose animations
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
// Compose layouts and foundations
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// Material icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
// Material 3 components
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
// Compose state and lifecycle hooks
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
// Third-party image loading
import coil.compose.AsyncImage
// Project resources and domain models
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.theme.Poppins
import com.wavvy.app.core.designsystem.theme.accentCyan
import com.wavvy.app.core.designsystem.theme.luminance

// User account and settings dropdown menu
@Composable
fun ProfileDropdown(
    expanded: Boolean,
    isAuthenticated: Boolean,
    isGuestActive: Boolean = false,
    userName: String?,
    userEmail: String?,
    userProfilePicture: String?,
    onDismiss: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onSignOut: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    var isTransitioning by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val popupOffset = remember(isLandscape) { if (isLandscape) IntOffset(-180, 60) else IntOffset(-120, 80) }
    val dropdownWidth = remember(isLandscape) { if (isLandscape) 300.dp else 260.dp }

    LaunchedEffect(expanded) {
        if (expanded) isTransitioning = true
    }

    if (expanded || isTransitioning) {
        Popup(
            onDismissRequest = { if (isTransitioning) onDismiss() },
            properties = PopupProperties(focusable = true),
            offset = popupOffset
        ) {
            AnimatedVisibility(
                visible = expanded && isTransitioning,
                enter = fadeIn(tween(200)) + scaleIn(
                    initialScale = 0.4f,
                    transformOrigin = TransformOrigin(1f, 0f),
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
                ),
                exit = fadeOut(tween(150)) + scaleOut(
                    targetScale = 0.4f,
                    transformOrigin = TransformOrigin(1f, 0f),
                    animationSpec = tween(150)
                )
            ) {
                DisposableEffect(Unit) {
                    onDispose { isTransitioning = false }
                }

                Surface(
                    modifier = Modifier
                        .width(dropdownWidth)
                        .padding(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = if (isDark) 16.dp else 8.dp
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        AnimatedContent(
                            targetState = userName,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(1500)) togetherWith
                                        fadeOut(animationSpec = tween(1300))
                            },
                            label = "header_identity_transition"
                        ) { currentName ->
                            if (isAuthenticated) {
                                LoggedInHeader(
                                    userName = currentName,
                                    userSubtitle = userEmail,
                                    userProfilePicture = userProfilePicture,
                                    onLogoutClick = {
                                        onDismiss()
                                        showLogoutDialog = true
                                    }
                                )
                            } else if (isGuestActive) {
                                GuestHeader(userName = currentName)
                            } else {
                                LoggedOutHeader()
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                        )

                        ProfileMenuItem(
                            icon = if (isAuthenticated || isGuestActive) Icons.Default.SwapHoriz else Icons.AutoMirrored.Filled.Login,
                            text = if (isAuthenticated || isGuestActive) stringResource(R.string.menu_switch_account) else stringResource(R.string.menu_login),
                            tint = if (isDark) MaterialTheme.accentCyan else MaterialTheme.colorScheme.onSurface,
                            onClick = {
                                onDismiss()
                                onNavigateToLogin()
                            }
                        )

                        ProfileMenuItem(
                            icon = Icons.Default.Extension,
                            text = stringResource(R.string.menu_integrations),
                            onClick = { onDismiss() }
                        )

                        ProfileMenuItem(
                            icon = Icons.Default.Settings,
                            text = stringResource(R.string.menu_settings),
                            onClick = {
                                onDismiss()
                                onNavigateToSettings()
                            }
                        )
                    }
                }
            }
        }
    }

    if (showLogoutDialog) {
        LogoutConfirmationDialog(
            onDismissRequest = { showLogoutDialog = false },
            onConfirmLogout = onSignOut
        )
    }
}

// Guest user header presentation
@Composable
private fun LoggedOutHeader() {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = stringResource(R.string.menu_welcome),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = Poppins,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                text = stringResource(R.string.menu_create_account),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontFamily = Poppins
                )
            )
        }
    }
}

// Active guest identity header presentation
@Composable
private fun GuestHeader(userName: String?) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = userName ?: stringResource(R.string.menu_default_user),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = Poppins,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.menu_guest_mode_subtitle),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontFamily = Poppins
                )
            )
        }
    }
}

// Authenticated user header presentation
@Composable
private fun LoggedInHeader(
    userName: String?,
    userSubtitle: String?,
    userProfilePicture: String?,
    onLogoutClick: () -> Unit
) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!userProfilePicture.isNullOrBlank()) {
            AsyncImage(
                model = userProfilePicture,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = userName ?: stringResource(R.string.menu_default_user),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = Poppins,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!userSubtitle.isNullOrBlank()) {
                Text(
                    text = userSubtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontFamily = Poppins
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(onClick = onLogoutClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = stringResource(R.string.menu_logout),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

// Reusable menu option row layout
@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    text: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint.copy(alpha = 0.85f),
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = Poppins,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = tint
            )
        )
    }
}

// Sign out confirmation dialog
@Composable
private fun LogoutConfirmationDialog(
    onDismissRequest: () -> Unit,
    onConfirmLogout: () -> Unit
) {
    var animateIn by remember { mutableStateOf(false) }
    var isTransitioning by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        animateIn = true
    }

    if (animateIn || isTransitioning) {
        Popup(
            onDismissRequest = { animateIn = false },
            properties = PopupProperties(focusable = true),
            alignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = animateIn && isTransitioning,
                enter = fadeIn(tween(200)) + scaleIn(
                    initialScale = 0.4f,
                    transformOrigin = TransformOrigin(1f, 0f),
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
                ),
                exit = fadeOut(tween(150)) + scaleOut(
                    targetScale = 0.4f,
                    transformOrigin = TransformOrigin(1f, 0f),
                    animationSpec = tween(150)
                )
            ) {
                DisposableEffect(Unit) {
                    onDispose {
                        isTransitioning = false
                        onDismissRequest()
                    }
                }

                Surface(
                    modifier = Modifier
                        .width(340.dp)
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 24.dp
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = stringResource(R.string.dialog_logout_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = Poppins,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.dialog_logout_message),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontFamily = Poppins,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            ),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { animateIn = false }) {
                                Text(
                                    text = stringResource(R.string.dialog_cancel),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = Poppins,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = {
                                    onConfirmLogout()
                                    animateIn = false
                                }
                            ) {
                                Text(
                                    text = stringResource(R.string.menu_logout),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = Poppins,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
