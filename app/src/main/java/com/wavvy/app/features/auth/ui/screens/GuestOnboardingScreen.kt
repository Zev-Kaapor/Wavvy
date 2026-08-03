package com.wavvy.app.features.auth.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
// Material 3 components
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.SettingsSuggest
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
// Compose state and lifecycle hooks
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.data.local.SettingsStorage
import com.wavvy.app.core.designsystem.components.LearnMoreBottomSheet
import com.wavvy.app.core.designsystem.theme.Poppins
import com.wavvy.app.core.designsystem.theme.ThemeMode

// Hard cap for the guest display name
private const val MAX_GUEST_NAME_LENGTH = 15

// Battery permission content
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryPermissionContent(
    onContinue: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isGranted by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }
    var showDetailsSheet by remember { mutableStateOf(false) }

    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        isGranted = isIgnoringBatteryOptimizations(context)
    }

    // Lifecycle observer
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_START) {
                isGranted = isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.onboarding_battery_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Text(
            text = stringResource(R.string.onboarding_battery_summary),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        TextButton(
            onClick = { showDetailsSheet = true },
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.onboarding_learn_more),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Button(
            onClick = {
                if (!isGranted) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    settingsLauncher.launch(intent)
                }
            },
            enabled = !isGranted,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isGranted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                contentColor = if (isGranted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = if (isGranted) {
                    stringResource(R.string.onboarding_battery_granted)
                } else {
                    stringResource(R.string.onboarding_battery_button)
                },
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onContinue,
            enabled = isGranted,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = stringResource(R.string.onboarding_continue),
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(text = stringResource(R.string.onboarding_back))
        }
    }

    if (showDetailsSheet) {
        LearnMoreBottomSheet(
            title = stringResource(R.string.onboarding_battery_title),
            description = buildBatteryInstructionsText(),
            onDismiss = { showDetailsSheet = false }
        )
    }
}

// Battery instructions builder
@Composable
private fun buildBatteryInstructionsText(): String {
    val manufacturer = remember { android.os.Build.MANUFACTURER.lowercase() }

    val intro = stringResource(R.string.onboarding_battery_intro)
    val step1 = stringResource(R.string.onboarding_battery_step_1)
    val step2 = stringResource(R.string.onboarding_battery_step_2)
    val step3 = stringResource(R.string.onboarding_battery_step_3)
    val step4 = stringResource(R.string.onboarding_battery_step_4)
    val brandLabel = stringResource(R.string.onboarding_battery_brand_label)
    val footer = stringResource(R.string.onboarding_battery_footer)

    val (optionText, optionHint) = when {
        manufacturer.contains("samsung") ->
            stringResource(R.string.onboarding_battery_option_samsung) to
                    stringResource(R.string.onboarding_battery_option_samsung_hint)
        manufacturer.contains("motorola") ->
            stringResource(R.string.onboarding_battery_option_motorola) to
                    stringResource(R.string.onboarding_battery_option_motorola_hint)
        manufacturer.contains("oppo") || manufacturer.contains("realme") ->
            stringResource(R.string.onboarding_battery_option_oppo) to
                    stringResource(R.string.onboarding_battery_option_oppo_hint)
        else ->
            stringResource(R.string.onboarding_battery_option_stock) to
                    stringResource(R.string.onboarding_battery_option_stock_hint)
    }

    return buildString {
        appendLine(intro)
        appendLine()
        appendLine("1. $step1")
        appendLine("2. $step2")
        appendLine("3. $step3")
        appendLine("4. $step4")
        appendLine()
        appendLine("$brandLabel $optionText ($optionHint)")
        appendLine()
        append(footer)
    }
}

// Guest disclaimer content
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestDisclaimerContent(
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDetailsSheet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.onboarding_guest_disclaimer_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Text(
            text = stringResource(R.string.onboarding_guest_disclaimer_summary),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        TextButton(
            onClick = { showDetailsSheet = true },
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.onboarding_learn_more),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Button(
            onClick = onConfirm,
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
                text = stringResource(R.string.onboarding_guest_confirm),
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(text = stringResource(R.string.onboarding_back))
        }
    }

    if (showDetailsSheet) {
        LearnMoreBottomSheet(
            title = stringResource(R.string.onboarding_guest_disclaimer_title),
            description = stringResource(R.string.onboarding_guest_disclaimer_body),
            onDismiss = { showDetailsSheet = false }
        )
    }
}

// Theme customization content
@Composable
fun ThemeCustomizationContent(
    showNameField: Boolean,
    currentTheme: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    onConfirm: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settingsStorage = remember { SettingsStorage(context) }
    var guestName by remember { mutableStateOf(if (showNameField) settingsStorage.getGuestName() else "") }
    val isAtNameLimit = guestName.length >= MAX_GUEST_NAME_LENGTH
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(if (showNameField) R.string.guest_name_title else R.string.login_appearance_title),
            fontSize = 22.sp,
            fontFamily = Poppins,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = stringResource(if (showNameField) R.string.guest_name_subtitle else R.string.login_appearance_subtitle),
            fontSize = 14.sp,
            fontFamily = Poppins,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (showNameField) {
            OutlinedTextField(
                value = guestName,
                onValueChange = { newValue ->
                    if (newValue.length <= MAX_GUEST_NAME_LENGTH) guestName = newValue
                },
                label = { Text(text = stringResource(R.string.guest_name_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }),
                shape = RoundedCornerShape(16.dp),
                supportingText = {
                    Text(
                        text = "${guestName.length}/$MAX_GUEST_NAME_LENGTH",
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                        color = if (isAtNameLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )
        }

        Text(
            text = stringResource(R.string.guest_theme_title),
            fontSize = 14.sp,
            fontFamily = Poppins,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        ThemeMode.entries.forEach { mode ->
            val isSelected = currentTheme == mode
            val label = when (mode) {
                ThemeMode.SYSTEM -> stringResource(R.string.guest_theme_system)
                ThemeMode.LIGHT -> stringResource(R.string.guest_theme_light)
                ThemeMode.DARK -> stringResource(R.string.guest_theme_dark)
            }
            val icon = when (mode) {
                ThemeMode.SYSTEM -> Icons.Rounded.SettingsSuggest
                ThemeMode.LIGHT -> Icons.Rounded.WbSunny
                ThemeMode.DARK -> Icons.Rounded.Bedtime
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                    .clickable {
                        settingsStorage.saveThemeMode(mode)
                        onThemeSelected(mode)
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = label,
                    fontFamily = Poppins,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                focusManager.clearFocus()
                keyboardController?.hide()
                if (showNameField) {
                    settingsStorage.saveGuestName(guestName.trim())
                }
                onConfirm(guestName.trim())
            },
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
                text = stringResource(R.string.onboarding_continue),
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(text = stringResource(R.string.onboarding_back))
        }

        if (showNameField) {
            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    settingsStorage.setGuestActive(true)
                    onConfirm("")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = stringResource(R.string.confirm_skip),
                    fontFamily = Poppins,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// Battery optimization check
internal fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    return powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
}
