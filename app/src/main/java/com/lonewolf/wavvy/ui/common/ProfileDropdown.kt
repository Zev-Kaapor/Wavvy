package com.lonewolf.wavvy.ui.common

// Jetpack Compose animation mechanics
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
// Foundation and interaction
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 and icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
// UI Styling and windowing
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.theme.Poppins

// Animated profile dropdown
@Composable
fun ProfileDropdown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    var isTransitioning by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()

    LaunchedEffect(expanded) {
        if (expanded) isTransitioning = true
    }

    if (expanded || isTransitioning) {
        Popup(
            onDismissRequest = { isTransitioning = false },
            properties = PopupProperties(focusable = true),
            offset = IntOffset(-120, 80)
        ) {
            // Dropdown animation
            AnimatedVisibility(
                visible = isTransitioning,
                enter = fadeIn(tween(200)) + scaleIn(
                    initialScale = 0.4f,
                    transformOrigin = TransformOrigin(1f, 0f),
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
                ),
                exit = fadeOut(spring(stiffness = 450f)) + scaleOut(
                    targetScale = 0.4f,
                    transformOrigin = TransformOrigin(1f, 0f),
                    animationSpec = spring(dampingRatio = 0.9f, stiffness = 450f)
                )
            ) {
                DisposableEffect(Unit) {
                    onDispose { if (!isTransitioning) onDismiss() }
                }

                Surface(
                    modifier = Modifier.width(260.dp).padding(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.95f else 1.0f),
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = if (isDark) 16.dp else 8.dp
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        // User header
                        LoggedOutHeader()

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                        )

                        // Login action
                        ProfileMenuItem(
                            icon = Icons.AutoMirrored.Filled.Login,
                            text = stringResource(R.string.menu_login),
                            tint = if (isDark) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                            onClick = {
                                isTransitioning = false
                                onNavigateToLogin()
                            }
                        )

                        // Integrations action
                        ProfileMenuItem(
                            icon = Icons.Default.Extension,
                            text = stringResource(R.string.menu_integrations),
                            onClick = { isTransitioning = false }
                        )

                        // Settings action
                        ProfileMenuItem(
                            icon = Icons.Default.Settings,
                            text = stringResource(R.string.menu_settings),
                            onClick = {
                                isTransitioning = false
                                onNavigateToSettings()
                            }
                        )
                    }
                }
            }
        }
    }
}

// Guest header view
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

// Interactive menu item
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
