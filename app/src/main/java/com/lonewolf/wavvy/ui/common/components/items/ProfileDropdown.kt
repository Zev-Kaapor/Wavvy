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
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 and icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
// UI styling and windowing
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
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
import com.lonewolf.wavvy.ui.theme.accentCyan

// Profile menu dropdown popup
@Composable
fun ProfileDropdown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    // Transition state for clean animation cycles
    var isTransitioning by remember { mutableStateOf(false) }
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
                    color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.95f else 1.0f),
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = if (isDark) 16.dp else 8.dp
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        // Header section
                        LoggedOutHeader()

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                        )

                        // Menu actions
                        ProfileMenuItem(
                            icon = Icons.AutoMirrored.Filled.Login,
                            text = stringResource(R.string.menu_login),
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
