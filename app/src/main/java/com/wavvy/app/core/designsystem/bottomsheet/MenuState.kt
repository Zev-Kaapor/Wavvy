package com.wavvy.app.core.designsystem.bottomsheet

// Compose runtime components
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

// State controller for menu presentation
@Stable
class MenuState {
    var isVisible by mutableStateOf(false)
        private set

    fun show() {
        isVisible = true
    }

    fun dismiss() {
        isVisible = false
    }
}

// Composition local holder for menu state
val LocalMenuState = staticCompositionLocalOf { MenuState() }

// Provider layout injecting menu state scope
@Composable
fun ProvideMenuState(content: @Composable () -> Unit) {
    val menuState = remember { MenuState() }
    CompositionLocalProvider(LocalMenuState provides menuState) {
        content()
    }
}
