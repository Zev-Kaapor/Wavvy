package com.lonewolf.wavvy.ui.common.components

// Compose layouts and foundations
import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.theme.luminance

// Navigation routes
object NavRoutes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val LIBRARY = "library"
}

// Docked navigation bar
@SuppressLint("ConfigurationScreenWidthHeight")
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
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val borderAlpha = if (isDark) 0.15f else 0.23f
    val borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = borderAlpha)

    val surfaceColor = MaterialTheme.colorScheme.surface.copy(alpha = 1.0f)

    if (isLandscape) {
        // Lateral Bar for Landscape with dynamic inset recognition
        val leftInset = WindowInsets.safeDrawing.asPaddingValues().calculateStartPadding(LocalLayoutDirection.current)
        val iconAreaWidth = 80.dp
        val defaultPillWidth = 92.dp

        val cameraOffsetReduction = 16.dp
        val adjustedInset = (leftInset - cameraOffsetReduction).coerceAtLeast(0.dp)

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
                    .background(surfaceColor)
                    .drawBehind {
                        val strokeWidth = 0.5.dp.toPx()
                        val cornerRadius = 28.dp.toPx()
                        val path = Path().apply {
                            moveTo(0f, 0f)
                            lineTo(size.width - cornerRadius, 0f)
                            quadraticTo(
                                size.width, 0f,
                                size.width, cornerRadius
                            )
                            lineTo(size.width, size.height - cornerRadius)
                            quadraticTo(
                                size.width, size.height,
                                size.width - cornerRadius, size.height
                            )
                            lineTo(0f, size.height)
                        }
                        drawPath(
                            path = path,
                            color = borderColor,
                            style = Stroke(width = strokeWidth)
                        )
                    },
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
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        icon = Icons.Default.Home,
                        label = stringResource(R.string.nav_home),
                        isSelected = currentRoute == NavRoutes.HOME,
                        onClick = onHomeClick
                    )
                    NavIcon(
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        icon = Icons.Default.Search,
                        label = stringResource(R.string.nav_explore),
                        isSelected = currentRoute == NavRoutes.SEARCH,
                        onClick = onSearchClick
                    )
                    NavIcon(
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        icon = Icons.AutoMirrored.Filled.List,
                        label = stringResource(R.string.nav_library),
                        isSelected = currentRoute == NavRoutes.LIBRARY,
                        onClick = onLibraryClick
                    )
                }
            }
        }
    } else {
        // Floating premium bar for Portrait
        val overflowWidth = configuration.screenWidthDp.dp + 8.dp
        val pillShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
        val systemBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val baseNavBarHeight = 85.dp

        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(baseNavBarHeight),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                modifier = Modifier
                    .requiredWidth(overflowWidth)
                    .height(baseNavBarHeight)
                    .offset(y = 4.dp)
                    .clip(pillShape)
                    .background(surfaceColor)
                    .drawBehind {
                        val strokeWidth = 0.5.dp.toPx()
                        val cornerRadius = 16.dp.toPx()
                        val path = Path().apply {
                            moveTo(0f, size.height)
                            lineTo(0f, cornerRadius)
                            quadraticTo(
                                0f, 0f,
                                cornerRadius, 0f
                            )
                            lineTo(size.width - cornerRadius, 0f)
                            quadraticTo(
                                size.width, 0f,
                                size.width, cornerRadius
                            )
                            lineTo(size.width, size.height)
                        }
                        drawPath(
                            path = path,
                            color = borderColor,
                            style = Stroke(width = strokeWidth)
                        )
                    }
                    .padding(bottom = systemBottomInset),
                horizontalArrangement = Arrangement.spacedBy(35.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavIcon(
                    modifier = Modifier.fillMaxHeight().width(64.dp),
                    icon = Icons.Default.Home,
                    label = stringResource(R.string.nav_home),
                    isSelected = currentRoute == NavRoutes.HOME,
                    onClick = onHomeClick
                )
                NavIcon(
                    modifier = Modifier.fillMaxHeight().width(64.dp),
                    icon = Icons.Default.Search,
                    label = stringResource(R.string.nav_explore),
                    isSelected = currentRoute == NavRoutes.SEARCH,
                    onClick = onSearchClick
                )
                NavIcon(
                    modifier = Modifier.fillMaxHeight().width(64.dp),
                    icon = Icons.AutoMirrored.Filled.List,
                    label = stringResource(R.string.nav_library),
                    isSelected = currentRoute == NavRoutes.LIBRARY,
                    onClick = onLibraryClick
                )
            }
        }
    }
}

// Interactive navigation icon component
@Composable
private fun NavIcon(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
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
        targetValue = if (isSelected) 24.dp else 21.dp,
        animationSpec = tween(150),
        label = "nav_item_size"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
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
    }
}
