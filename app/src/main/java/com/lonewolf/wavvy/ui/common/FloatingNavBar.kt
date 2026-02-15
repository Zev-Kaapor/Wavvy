package com.lonewolf.wavvy.ui.common

// Jetpack Compose layout, interaction, and styling
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
// Navigation bar icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
// Material 3 and composable state
import androidx.compose.material3.*
import androidx.compose.runtime.*
// UI Utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.theme.Poppins

// Floating navigation bar for main app sections
@Composable
fun FloatingNavBar(
    modifier: Modifier = Modifier,
    currentRoute: String = "home",
    onHomeClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onLibraryClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(60.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                .border(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(50.dp)
                )
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home
            NavIcon(
                icon = Icons.Default.Home,
                label = stringResource(R.string.nav_home),
                isSelected = currentRoute == "home",
                onClick = onHomeClick
            )
            // Search
            NavIcon(
                icon = Icons.Default.Search,
                label = stringResource(R.string.nav_explore),
                isSelected = currentRoute == "search",
                onClick = onSearchClick
            )
            // Library
            NavIcon(
                icon = Icons.AutoMirrored.Filled.List,
                label = stringResource(R.string.nav_library),
                isSelected = currentRoute == "library",
                onClick = onLibraryClick
            )
        }
    }
}

// Individual nav item with smooth state transitions
@Composable
private fun RowScope.NavIcon(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Animate color transition
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        animationSpec = tween(durationMillis = 300),
        label = "nav_item_color"
    )

    // Animate icon size for a subtle pop effect
    val iconSize by animateDpAsState(
        targetValue = if (isSelected) 26.dp else 22.dp,
        animationSpec = tween(durationMillis = 300),
        label = "nav_item_size"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxHeight()
            .weight(1f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        // Nav icon with size animation
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(iconSize)
        )

        // Nav label with animated color
        Text(
            text = label,
            fontFamily = Poppins,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 11.sp,
            color = contentColor,
            lineHeight = 14.sp
        )
    }
}
