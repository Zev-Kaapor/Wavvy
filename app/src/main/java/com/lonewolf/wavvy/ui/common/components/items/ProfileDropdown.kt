package com.lonewolf.wavvy.ui.common.components.items

// Animation mechanics
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
// Foundation and interaction
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 and icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
// UI styling and windowing
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil.compose.AsyncImage
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.theme.Poppins
import com.lonewolf.wavvy.ui.theme.accentCyan

// Profile menu dropdown popup
@Composable
fun ProfileDropdown(
    expanded: Boolean,
    isAuthenticated: Boolean,
    userEmail: String?,
    userProfilePicture: String?,
    onDismiss: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onSignOut: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    // Transition state for clean animation cycles
    var isTransitioning by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()

    // Adaptive layout configuration
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val popupOffset = if (isLandscape) IntOffset(-180, 60) else IntOffset(-120, 80)
    val dropdownWidth = if (isLandscape) 300.dp else 260.dp

    // Visibility sync
    LaunchedEffect(expanded) {
        if (expanded) isTransitioning = true
    }

    if (expanded || isTransitioning) {
        Popup(
            onDismissRequest = { if (isTransitioning) onDismiss() },
            properties = PopupProperties(focusable = true),
            offset = popupOffset
        ) {
            // Dropdown animation with standard timing
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
                // Finalize state on animation completion
                DisposableEffect(Unit) {
                    onDispose { isTransitioning = false }
                }

                Surface(
                    modifier = Modifier
                        .width(dropdownWidth)
                        .padding(8.dp),
                    // Unified glass effect for both themes
                    color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.90f else 0.90f),
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = if (isDark) 16.dp else 8.dp
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        // Header section based on login state
                        if (isAuthenticated) {
                            LoggedInHeader(userEmail = userEmail, userProfilePicture = userProfilePicture)
                        } else {
                            LoggedOutHeader()
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                        )

                        // Menu actions based on login state
                        ProfileMenuItem(
                            icon = if (isAuthenticated) Icons.AutoMirrored.Filled.Logout else Icons.AutoMirrored.Filled.Login,
                            text = if (isAuthenticated) stringResource(R.string.menu_logout) else stringResource(R.string.menu_login),
                            tint = if (isAuthenticated) MaterialTheme.colorScheme.error else if (isDark) MaterialTheme.accentCyan else MaterialTheme.colorScheme.primary,
                            onClick = {
                                onDismiss()
                                if (isAuthenticated) showLogoutDialog = true else onNavigateToLogin()
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

    // Centered sign out confirmation dialog container
    if (showLogoutDialog) {
        LogoutConfirmationDialog(
            onDismissRequest = { showLogoutDialog = false },
            onConfirmLogout = onSignOut
        )
    }
}

// Guest user welcome header
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

// Authenticated user header
@Composable
private fun LoggedInHeader(userEmail: String?, userProfilePicture: String?) {
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
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.accentCyan,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = stringResource(R.string.menu_your_account),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = Poppins,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                text = userEmail ?: stringResource(R.string.default_artist_name),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontFamily = Poppins
                )
            )
        }
    }
}

// Reusable menu option row
@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    text: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
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

// Centered confirmation dialog layout matching dropdown aesthetics
@Composable
private fun LogoutConfirmationDialog(
    onDismissRequest: () -> Unit,
    onConfirmLogout: () -> Unit
) {
    var animateIn by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()

    LaunchedEffect(Unit) {
        animateIn = true
    }

    Popup(
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
        alignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = animateIn,
            enter = fadeIn(tween(220)) + scaleIn(
                initialScale = 0.85f,
                transformOrigin = TransformOrigin(0.5f, 0.5f),
                animationSpec = spring(dampingRatio = 0.85f, stiffness = 350f)
            ),
            exit = fadeOut(tween(150)) + scaleOut(
                targetScale = 0.85f,
                transformOrigin = TransformOrigin(0.5f, 0.5f),
                animationSpec = tween(150)
            )
        ) {
            Surface(
                modifier = Modifier
                    .width(300.dp)
                    .padding(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.95f else 0.95f),
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
                        text = "Sign out",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = Poppins,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Are you sure you want to log out of your account?",
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
                        TextButton(onClick = onDismissRequest) {
                            Text(
                                text = "Cancel",
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
                                onDismissRequest()
                            }
                        ) {
                            Text(
                                text = "Sign out",
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
