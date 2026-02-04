package com.lonewolf.wavvy.ui.common

// Jetpack Compose framework and layout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
// Material design icons and components
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
// UI styling and resources
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
// Project internal resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.theme.Poppins

// Main header with logo and profile access
@Composable
fun HomeHeader(
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val brandColor = MaterialTheme.colorScheme.tertiary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // App Logo
        Text(
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        color = brandColor,
                        fontWeight = FontWeight.Black,
                        shadow = Shadow(
                            color = brandColor.copy(alpha = 0.45f),
                            blurRadius = 20f
                        )
                    )
                ) {
                    append(stringResource(R.string.logo_Home))
                }
            },
            fontFamily = Poppins,
            fontSize = 28.sp
        )

        // Profile interaction area
        Box {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .clickable { expanded = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = stringResource(R.string.cd_profile_button),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
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
