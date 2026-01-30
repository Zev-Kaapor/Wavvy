package com.lonewolf.wavvy.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.theme.Poppins

@Composable
fun FloatingNavBar() {
    // Container geral que fixa a navbar na parte inferior
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 0.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Barra flutuante em si
        Row(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(58.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
                .border(
                    0.5.dp,
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    RoundedCornerShape(50.dp)
                )
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícones de navegação
            NavIcon(Icons.Default.Home, stringResource(R.string.nav_home), isSelected = true)
            NavIcon(Icons.Default.Search, stringResource(R.string.nav_explore), isSelected = false)
            NavIcon(Icons.AutoMirrored.Filled.List, stringResource(R.string.nav_library), isSelected = false)
        }
    }
}

@Composable
fun RowScope.NavIcon(
    icon: ImageVector,
    label: String,
    isSelected: Boolean
) {
    // Cores baseadas no estado (ativo / inativo)
    val activeColor = MaterialTheme.colorScheme.tertiary
    val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxHeight()
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .clickable { /* navegação futuramente */ }
            .padding(vertical = 4.dp)
    ) {
        // Ícone principal
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) activeColor else inactiveColor,
            modifier = Modifier.size(24.dp)
        )

        // Texto abaixo do ícone
        Text(
            text = label,
            fontFamily = Poppins,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 11.sp,
            color = if (isSelected) activeColor else inactiveColor,
            lineHeight = 14.sp
        )
    }
}
