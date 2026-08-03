package com.wavvy.app.features.auth.ui.screens

// Android activity components
import androidx.activity.compose.BackHandler
// Compose animation components
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// Compose icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
// Material 3 components
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
// Compose state and lifecycle hooks
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Image loading
import coil.compose.AsyncImage
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.WordmarkLayoutSpec
import com.wavvy.app.core.designsystem.components.ThemedScrollbar
import com.wavvy.app.core.designsystem.theme.ThemeMode
import com.wavvy.app.core.designsystem.theme.googleSignInBackground
import com.wavvy.app.features.auth.data.GuestProfile
import com.wavvy.app.features.auth.data.SavedAccount
import com.wavvy.app.features.auth.ui.components.EmbeddedAuthWebView
import com.wavvy.app.features.auth.ui.state.AuthUiState
import com.wavvy.app.features.auth.ui.viewmodel.AuthViewModel

// Onboarding flow variants
private enum class OnboardingFlow { GUEST, LOGIN }

// Onboarding navigation steps
private enum class OnboardingStep { NONE, GUEST_DISCLAIMER, BATTERY, CUSTOMIZATION }

// Screen state routes
private enum class AuthSubScreen { LOGIN, GUEST_DISCLAIMER, BATTERY, CUSTOMIZATION, WEB_VIEW, ERROR }

// Account list layout constraints
private val ACCOUNTS_LIST_ROW_HEIGHT = 64.dp
private val ACCOUNTS_LIST_ROW_SPACING = 12.dp
private val ACCOUNTS_LIST_MAX_HEIGHT = ACCOUNTS_LIST_ROW_HEIGHT * 3 + ACCOUNTS_LIST_ROW_SPACING * 2

// Auth screen
@Composable
fun AuthScreen(
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel,
    currentTheme: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    onAuthSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var onboardingStep by rememberSaveable { mutableStateOf(OnboardingStep.NONE) }
    var onboardingFlow by rememberSaveable { mutableStateOf(OnboardingFlow.GUEST) }
    var isNewLoginFlow by rememberSaveable { mutableStateOf(false) }
    var previousUiState by remember { mutableStateOf<AuthUiState>(AuthUiState.Idle) }

    LaunchedEffect(uiState) {
        val wasWebView = previousUiState is AuthUiState.WebViewFlow
        previousUiState = uiState
        if (wasWebView && uiState is AuthUiState.Success && isNewLoginFlow) {
            focusManager.clearFocus()
            keyboardController?.hide()

            isNewLoginFlow = false
            onboardingFlow = OnboardingFlow.LOGIN
            onboardingStep = if (isIgnoringBatteryOptimizations(context)) {
                OnboardingStep.CUSTOMIZATION
            } else {
                OnboardingStep.BATTERY
            }
        } else if (wasWebView && uiState !is AuthUiState.WebViewFlow) {
            isNewLoginFlow = false
        }
    }

    val currentSubScreen = when {
        uiState is AuthUiState.WebViewFlow -> AuthSubScreen.WEB_VIEW
        uiState is AuthUiState.Error -> AuthSubScreen.ERROR
        onboardingStep == OnboardingStep.GUEST_DISCLAIMER -> AuthSubScreen.GUEST_DISCLAIMER
        onboardingStep == OnboardingStep.BATTERY -> AuthSubScreen.BATTERY
        onboardingStep == OnboardingStep.CUSTOMIZATION -> AuthSubScreen.CUSTOMIZATION
        else -> AuthSubScreen.LOGIN
    }

    fun navigateBack() {
        when (currentSubScreen) {
            AuthSubScreen.CUSTOMIZATION -> {
                onboardingStep = when {
                    !isIgnoringBatteryOptimizations(context) -> OnboardingStep.BATTERY
                    onboardingFlow == OnboardingFlow.GUEST -> OnboardingStep.GUEST_DISCLAIMER
                    else -> OnboardingStep.NONE
                }
            }
            AuthSubScreen.BATTERY -> {
                onboardingStep = if (onboardingFlow == OnboardingFlow.GUEST) {
                    OnboardingStep.GUEST_DISCLAIMER
                } else {
                    OnboardingStep.NONE
                }
            }
            AuthSubScreen.GUEST_DISCLAIMER -> onboardingStep = OnboardingStep.NONE
            AuthSubScreen.WEB_VIEW, AuthSubScreen.ERROR -> {
                isNewLoginFlow = false
                viewModel.loadSavedAccounts()
            }
            AuthSubScreen.LOGIN -> Unit
        }
    }

    BackHandler(enabled = currentSubScreen != AuthSubScreen.LOGIN) {
        navigateBack()
    }

    val containerBackgroundColor = if (currentSubScreen == AuthSubScreen.WEB_VIEW) {
        googleSignInBackground
    } else {
        MaterialTheme.colorScheme.background
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(containerBackgroundColor)
            .displayCutoutPadding()
    ) {
        AnimatedContent(
            targetState = currentSubScreen,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            },
            label = "AuthScreenTransitions"
        ) { targetScreen ->
            when (targetScreen) {
                AuthSubScreen.LOGIN -> {
                    var stableAccounts by remember { mutableStateOf(emptyList<SavedAccount>()) }
                    var stableGuestProfiles by remember { mutableStateOf(emptyList<GuestProfile>()) }
                    LaunchedEffect(uiState) {
                        (uiState as? AuthUiState.Success)?.let { success ->
                            stableAccounts = success.savedAccounts
                            stableGuestProfiles = success.guestProfiles
                        }
                    }
                    AccountSelectionContent(
                        accounts = stableAccounts,
                        guestProfiles = stableGuestProfiles,
                        onAddAccountClick = {
                            isNewLoginFlow = true
                            viewModel.startGoogleAuthFlow()
                        },
                        onAccountSelect = { account ->
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            viewModel.selectAccount(account) { success ->
                                if (success) onAuthSuccess()
                            }
                        },
                        onAccountRemove = { account -> viewModel.removeAccount(account) },
                        onGuestProfileSelect = { profile ->
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            viewModel.selectGuestProfile(profile) { onAuthSuccess() }
                        },
                        onGuestProfileRemove = { profile -> viewModel.removeGuestProfile(profile) },
                        onGuestClick = {
                            onboardingFlow = OnboardingFlow.GUEST
                            onboardingStep = OnboardingStep.GUEST_DISCLAIMER
                        }
                    )
                }
                AuthSubScreen.GUEST_DISCLAIMER -> {
                    GuestDisclaimerContent(
                        onConfirm = {
                            onboardingStep = if (isIgnoringBatteryOptimizations(context)) {
                                OnboardingStep.CUSTOMIZATION
                            } else {
                                OnboardingStep.BATTERY
                            }
                        },
                        onBack = { navigateBack() }
                    )
                }
                AuthSubScreen.BATTERY -> {
                    BatteryPermissionContent(
                        onContinue = { onboardingStep = OnboardingStep.CUSTOMIZATION },
                        onBack = { navigateBack() }
                    )
                }
                AuthSubScreen.CUSTOMIZATION -> {
                    ThemeCustomizationContent(
                        showNameField = onboardingFlow == OnboardingFlow.GUEST,
                        currentTheme = currentTheme,
                        onThemeSelected = onThemeSelected,
                        onConfirm = { name ->
                            if (onboardingFlow == OnboardingFlow.GUEST) {
                                viewModel.createGuestProfile(name.trim()) {
                                    onboardingStep = OnboardingStep.NONE
                                    onAuthSuccess()
                                }
                            } else {
                                onboardingStep = OnboardingStep.NONE
                                onAuthSuccess()
                            }
                        },
                        onBack = { navigateBack() }
                    )
                }
                AuthSubScreen.WEB_VIEW -> {
                    val state = uiState as? AuthUiState.WebViewFlow
                    if (state != null) {
                        EmbeddedAuthWebView(
                            authUrl = state.authUrl,
                            redirectUri = state.redirectUri,
                            onTokenCaptured = { cookies -> viewModel.handleCookiesCaptured(cookies) },
                            onErrorReceived = {
                                isNewLoginFlow = false
                                viewModel.loadSavedAccounts()
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                AuthSubScreen.ERROR -> {
                    val state = uiState as? AuthUiState.Error
                    ErrorContent(
                        message = state?.message ?: "",
                        onRetry = { viewModel.loadSavedAccounts() }
                    )
                }
            }
        }
    }
}

// Account selection content
@Composable
private fun AccountSelectionContent(
    accounts: List<SavedAccount>,
    guestProfiles: List<GuestProfile>,
    onAddAccountClick: () -> Unit,
    onAccountSelect: (SavedAccount) -> Unit,
    onAccountRemove: (SavedAccount) -> Unit,
    onGuestProfileSelect: (GuestProfile) -> Unit,
    onGuestProfileRemove: (GuestProfile) -> Unit,
    onGuestClick: () -> Unit
) {
    val bottomAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        bottomAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900)
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val titleSpacerHeight = remember(maxHeight) {
            WordmarkLayoutSpec.dockedOffset(maxHeight) - WordmarkLayoutSpec.CONTENT_PADDING
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(titleSpacerHeight))

            Text(
                text = stringResource(R.string.app_name),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            AnimatedVisibility(
                visible = accounts.isNotEmpty() || guestProfiles.isNotEmpty(),
                enter = fadeIn(animationSpec = tween(durationMillis = 400)),
                exit = fadeOut(animationSpec = tween(durationMillis = 200))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = stringResource(R.string.auth_select_account_hint),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    val accountsListState = rememberLazyListState()
                    val appearedAccountKeys = remember { mutableStateOf(emptySet<String>()) }
                    val isScrollable = accountsListState.canScrollForward || accountsListState.canScrollBackward
                    val scrollbarPadding by animateDpAsState(
                        targetValue = if (isScrollable) 4.dp else 0.dp,
                        animationSpec = tween(durationMillis = 300),
                        label = "ScrollbarPaddingAnimation"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = ACCOUNTS_LIST_MAX_HEIGHT)
                    ) {
                        LazyColumn(
                            state = accountsListState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(accounts, key = { it.handle?.takeIf { h -> h.isNotBlank() } ?: it.name }) { account ->
                                val accountKey = account.handle?.takeIf { h -> h.isNotBlank() } ?: account.name
                                val hasAlreadyAppeared = accountKey in appearedAccountKeys.value
                                val visibleState = remember(accountKey) {
                                    MutableTransitionState(hasAlreadyAppeared).apply { targetState = true }
                                }
                                LaunchedEffect(accountKey) {
                                    appearedAccountKeys.value += accountKey
                                }

                                AnimatedVisibility(
                                    visibleState = visibleState,
                                    enter = fadeIn(animationSpec = tween(durationMillis = 400)),
                                    modifier = Modifier.animateItem()
                                ) {
                                    SavedAccountRow(
                                        account = account,
                                        onClick = { onAccountSelect(account) },
                                        onRemove = { onAccountRemove(account) }
                                    )
                                }
                            }

                            items(guestProfiles, key = { "guest_${it.id}" }) { profile ->
                                val guestKey = "guest_${profile.id}"
                                val hasAlreadyAppeared = guestKey in appearedAccountKeys.value
                                val visibleState = remember(guestKey) {
                                    MutableTransitionState(hasAlreadyAppeared).apply { targetState = true }
                                }
                                LaunchedEffect(guestKey) {
                                    appearedAccountKeys.value += guestKey
                                }

                                AnimatedVisibility(
                                    visibleState = visibleState,
                                    enter = fadeIn(animationSpec = tween(durationMillis = 400)),
                                    modifier = Modifier.animateItem()
                                ) {
                                    GuestProfileRow(
                                        profile = profile,
                                        onClick = { onGuestProfileSelect(profile) },
                                        onRemove = { onGuestProfileRemove(profile) }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(scrollbarPadding))

                        AnimatedVisibility(
                            visible = isScrollable,
                            enter = fadeIn(animationSpec = tween(durationMillis = 300)),
                            exit = fadeOut(animationSpec = tween(durationMillis = 300))
                        ) {
                            ThemedScrollbar(
                                state = accountsListState,
                                modifier = Modifier.fillMaxHeight()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(bottomAlpha.value)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.auth_language_placeholder),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onAddAccountClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = if (accounts.isEmpty()) stringResource(R.string.menu_login) else stringResource(R.string.menu_add_account),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onGuestClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(text = stringResource(R.string.menu_continue_guest))
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// Saved account row
@Composable
private fun SavedAccountRow(
    account: SavedAccount,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = account.pictureUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = account.name,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            account.handle?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
        }

        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

// Saved guest profile row
@Composable
private fun GuestProfileRow(
    profile: GuestProfile,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = profile.name,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(R.string.menu_continue_guest),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }

        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

// Error content
@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.dialog_logout_title),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Text(text = stringResource(R.string.dialog_cancel))
        }
    }
}
