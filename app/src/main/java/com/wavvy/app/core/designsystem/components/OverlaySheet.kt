package com.wavvy.app.core.designsystem.components

// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
// Reactive flows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// A sheet that a screen asks for and the main screen draws, so it rises over the bottom bar and the mini player and not inside the screen that asked
object OverlaySheet {
    private val mutableContent = MutableStateFlow<(@Composable () -> Unit)?>(null)
    val content: StateFlow<(@Composable () -> Unit)?> = mutableContent.asStateFlow()

    // Shows a sheet, its content brings the sheet itself and calls dismiss when it closes
    fun show(content: @Composable () -> Unit) {
        mutableContent.value = content
    }

    fun dismiss() {
        mutableContent.value = null
    }
}

// Draws the sheet that is asked for, if there is one
@Composable
fun OverlaySheetHost() {
    val sheet by OverlaySheet.content.collectAsState()
    sheet?.invoke()
}
