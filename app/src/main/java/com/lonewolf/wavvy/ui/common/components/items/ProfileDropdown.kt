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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.lonewolf.wavvy.data.SavedAccount
import com.lonewolf.wavvy.ui.theme.Poppins
import com.lonewolf.wavvy.ui.theme.accentCyan
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

// Profile menu dropdown popup
@Composable
fun ProfileDropdown(
    expanded: Boolean,
    isAuthenticated: Boolean,
    userEmail: String?,
    userProfilePicture: String?,
    savedAccounts: List<SavedAccount> = emptyList(),
    showAccountSwitcher: Boolean = false,
    onDismiss: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onSignOut: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onSwitchAccount: () -> Unit = {},
    onAccountSelected: (SavedAccount) -> Unit = {},
    onDismissAccountSwitcher: () -> Unit = {}
) {
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
                    color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.90f else 0.90f),
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = if (isDark) 16.dp else 8.dp
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        AnimatedContent(
                            targetState = userEmail,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(1500)) togetherWith
                                        fadeOut(animationSpec = tween(1300))
                            },
                            label = "header_identity_transition"
                        ) { currentEmail ->
                            if (isAuthenticated) {
                                LoggedInHeader(
                                    userName = currentEmail,
                                    userProfilePicture = userProfilePicture,
                                    onLogoutClick = {
                                        onDismiss()
                                        showLogoutDialog = true
                                    }
                                )
                            } else {
                                LoggedOutHeader()
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                        )

                        // Menu actions based on login state
                        ProfileMenuItem(
                            icon = if (isAuthenticated) Icons.Default.SwapHoriz else Icons.AutoMirrored.Filled.Login,
                            text = if (isAuthenticated) stringResource(R.string.menu_switch_account) else stringResource(R.string.menu_login),
                            tint = if (isDark) MaterialTheme.accentCyan else MaterialTheme.colorScheme.onSurface,
                            onClick = {
                                onDismiss()
                                if (isAuthenticated) onSwitchAccount() else onNavigateToLogin()
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

    // Logout confirmation dialog
    if (showLogoutDialog) {
        LogoutConfirmationDialog(
            onDismissRequest = { showLogoutDialog = false },
            onConfirmLogout = onSignOut
        )
    }

    // Account switcher dialog
    if (showAccountSwitcher) {
        AccountSwitcherDialog(
            accounts = savedAccounts,
            onDismissRequest = onDismissAccountSwitcher,
            onAccountSelected = onAccountSelected,
            onAddAccount = {
                onDismissAccountSwitcher()
                onNavigateToLogin()
            }
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
private fun LoggedInHeader(
    userName: String?,
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
                text = stringResource(R.string.menu_your_account),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = Poppins,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                text = userName ?: stringResource(R.string.menu_default_user),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontFamily = Poppins
                )
            )
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

// Account switcher dialog with scrollable account list
@Composable
private fun AccountSwitcherDialog(
    accounts: List<SavedAccount>,
    onDismissRequest: () -> Unit,
    onAccountSelected: (SavedAccount) -> Unit,
    onAddAccount: () -> Unit
) {
    var animateIn by remember { mutableStateOf(false) }
    var isTransitioning by remember { mutableStateOf(true) }
    val isDark = isSystemInDarkTheme()
    val coroutineScope = rememberCoroutineScope()

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
                        .width(280.dp)
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.95f else 0.95f),
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 24.dp
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Spacer(Modifier.height(4.dp))

                        // Scrollable account list
                        LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                            items(accounts) { account ->
                                AccountRow(
                                    account = account,
                                    onClick = {
                                        animateIn = false
                                        // Wait for exit animation before switching
                                        coroutineScope.launch {
                                            delay(150.milliseconds)
                                            onAccountSelected(account)
                                        }
                                    }
                                )
                            }

                            item {
                                if (accounts.isNotEmpty()) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                                    )
                                }
                                // Add account button
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = LocalIndication.current,
                                            onClick = onAddAccount
                                        )
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(Modifier.width(16.dp))
                                    Text(
                                        text = stringResource(R.string.menu_add_account),
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontFamily = Poppins,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
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
}

// Single account row
@Composable
private fun AccountRow(
    account: SavedAccount,
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!account.pictureUrl.isNullOrBlank()) {
            AsyncImage(
                model = account.pictureUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
            )
        } else {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = account.name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            if (!account.handle.isNullOrBlank()) {
                Text(
                    text = account.handle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
            }
        }
    }
}

// Centered confirmation dialog layout matching dropdown aesthetics
@Composable
private fun LogoutConfirmationDialog(
    onDismissRequest: () -> Unit,
    onConfirmLogout: () -> Unit
) {
    var animateIn by remember { mutableStateOf(false) }
    var isTransitioning by remember { mutableStateOf(true) }
    val isDark = isSystemInDarkTheme()

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
