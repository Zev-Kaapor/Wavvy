package com.lonewolf.wavvy.ui.common.navigation

// Compose layouts and foundations
import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.platform.LocalConfiguration
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
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Adaptive dimensions
    val navBarHeight = if (isLandscape) 56.dp else 68.dp
    val navBarWidth = if (isLandscape) Modifier.widthIn(min = 320.dp, max = 400.dp).fillMaxWidth(0.5f)
    else Modifier.fillMaxWidth(0.88f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Navigation container
        Row(
            modifier = Modifier
                .then(navBarWidth)
                .height(navBarHeight)
                .clip(RoundedCornerShape(50.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                .border(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(50.dp)
                )
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavIcon(
                icon = Icons.Default.Home,
                label = stringResource(R.string.nav_home),
                isSelected = currentRoute == NavRoutes.HOME,
                isLandscape = isLandscape,
                onClick = onHomeClick
            )
            NavIcon(
                icon = Icons.Default.Search,
                label = stringResource(R.string.nav_explore),
                isSelected = currentRoute == NavRoutes.SEARCH,
                isLandscape = isLandscape,
                onClick = onSearchClick
            )
            NavIcon(
                icon = Icons.AutoMirrored.Filled.List,
                label = stringResource(R.string.nav_library),
                isSelected = currentRoute == NavRoutes.LIBRARY,
                isLandscape = isLandscape,
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
    isLandscape: Boolean,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val tertiary = MaterialTheme.colorScheme.tertiary
    val onSurface = MaterialTheme.colorScheme.onSurface

    // State-based color logic
    val targetColor = remember(isSelected, isDark) {
        if (isSelected) {
            if (isDark) tertiary else onSurface
        } else {
            onSurface.copy(alpha = 0.45f)
        }
    }

    val contentColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(150),
        label = "nav_item_color"
    )

    val iconSize by animateDpAsState(
        targetValue = if (isSelected) 26.dp else 23.dp,
        animationSpec = tween(150),
        label = "nav_item_size"
    )

    // Item layout
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxHeight()
            .weight(1f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(iconSize)
        )

        AnimatedVisibility(visible = !isLandscape) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = Poppins,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 11.sp,
                    color = contentColor
                ),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
