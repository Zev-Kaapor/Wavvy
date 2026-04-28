package com.lonewolf.wavvy.ui.home.components

// Compose layouts and foundations
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.common.components.items.ProfileDropdown
import com.lonewolf.wavvy.ui.theme.Poppins

// Home screen header
@Composable
fun HomeHeader(
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()

    // Visual state configuration
    val logoColor = if (isDark) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    val logoShadow = if (isDark) Shadow(color = logoColor.copy(alpha = 0.4f), blurRadius = 15f) else null

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 24.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = stringResource(R.string.cd_profile_button),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Profile menu
            ProfileDropdown(
                expanded = expanded,
                onDismiss = { expanded = false },
                onNavigateToLogin = { },
                onNavigateToSettings = onNavigateToSettings
            )
        }
    }
}
