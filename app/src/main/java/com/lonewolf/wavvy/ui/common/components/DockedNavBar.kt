package com.lonewolf.wavvy.ui.common.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
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

// Docked navigation bar
@Composable
fun DockedNavBar(
    modifier: Modifier = Modifier,
    currentRoute: String = NavRoutes.HOME,
    onHomeClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onLibraryClick: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        // Lateral Bar for Landscape with dynamic inset recognition
        val leftInset = WindowInsets.safeDrawing.asPaddingValues().calculateStartPadding(LocalLayoutDirection.current)
        val iconAreaWidth = 80.dp
        val defaultPillWidth = 92.dp

        // Use a reduction factor to fine-tune icon distance from the camera
        val cameraOffsetReduction = 16.dp
        val adjustedInset = (leftInset - cameraOffsetReduction).coerceAtLeast(0.dp)

        // Calculate container width ensuring thickness on the non-camera side
        val totalBarWidth = if (leftInset > 0.dp) {
            iconAreaWidth + adjustedInset
        } else {
            defaultPillWidth
        }

        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(totalBarWidth)
                    .clip(RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                    .border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(start = adjustedInset)
                        .width(iconAreaWidth),
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NavIcon(
                        icon = Icons.Default.Home,
                        label = stringResource(R.string.nav_home),
                        isSelected = currentRoute == NavRoutes.HOME,
                        isLandscape = true,
                        onClick = onHomeClick
                    )
                    NavIcon(
                        icon = Icons.Default.Search,
                        label = stringResource(R.string.nav_explore),
                        isSelected = currentRoute == NavRoutes.SEARCH,
                        isLandscape = true,
                        onClick = onSearchClick
                    )
                    NavIcon(
                        icon = Icons.AutoMirrored.Filled.List,
                        label = stringResource(R.string.nav_library),
                        isSelected = currentRoute == NavRoutes.LIBRARY,
                        isLandscape = true,
                        onClick = onLibraryClick
                    )
                }
            }
        }
    } else {
        // Docked Bottom Bar for Portrait
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 80f))
                    .border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)
                    ),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavIcon(
                    icon = Icons.Default.Home,
                    label = stringResource(R.string.nav_home),
                    isSelected = currentRoute == NavRoutes.HOME,
                    isLandscape = false,
                    onClick = onHomeClick
                )
                NavIcon(
                    icon = Icons.Default.Search,
                    label = stringResource(R.string.nav_explore),
                    isSelected = currentRoute == NavRoutes.SEARCH,
                    isLandscape = false,
                    onClick = onSearchClick
                )
                NavIcon(
                    icon = Icons.AutoMirrored.Filled.List,
                    label = stringResource(R.string.nav_library),
                    isSelected = currentRoute == NavRoutes.LIBRARY,
                    isLandscape = false,
                    onClick = onLibraryClick
                )
            }
        }
    }
}

// NavIcon remains the same as previously established
@Composable
private fun NavIcon(
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

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .then(
                if (isLandscape) Modifier.fillMaxWidth().height(56.dp)
                else Modifier.fillMaxHeight().width(80.dp)
            )
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
