package com.lonewolf.wavvy.ui.common

// Compose layouts and foundations
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
// UI styling and utilities
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

// Navigation routes
object NavRoutes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val LIBRARY = "library"
}

// Floating navigation bar
@Composable
fun FloatingNavBar(
    modifier: Modifier = Modifier,
    currentRoute: String = NavRoutes.HOME,
    onHomeClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onLibraryClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Navigation container
        Row(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(64.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                .border(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(50.dp)
                )
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home
            NavIcon(
                icon = Icons.Default.Home,
                label = stringResource(R.string.nav_home),
                isSelected = currentRoute == NavRoutes.HOME,
                onClick = onHomeClick
            )
            // Search
            NavIcon(
                icon = Icons.Default.Search,
                label = stringResource(R.string.nav_explore),
                isSelected = currentRoute == NavRoutes.SEARCH,
                onClick = onSearchClick
            )
            // Library
            NavIcon(
                icon = Icons.AutoMirrored.Filled.List,
                label = stringResource(R.string.nav_library),
                isSelected = currentRoute == NavRoutes.LIBRARY,
                onClick = onLibraryClick
            )
        }
    }
}

// Individual nav item
@Composable
private fun RowScope.NavIcon(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    // Dynamic color logic
    val targetColor = if (isSelected) {
        if (isDark) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    }

    // Color animation
    val contentColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 150),
        label = "nav_item_color"
    )

    // Size animation
    val iconSize by animateDpAsState(
        targetValue = if (isSelected) 24.dp else 20.dp,
        animationSpec = tween(durationMillis = 150),
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
        // Icon
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(iconSize)
        )

        // Label
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = Poppins,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = 10.sp,
                color = contentColor,
                lineHeight = 12.sp
            )
        )
    }
}
