package com.wavvy.app.core.designsystem.components

// Android system UI
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
// UI utilities
import androidx.compose.ui.platform.LocalView

// Hides the system bars of the window this is placed in, as the main window does, for sheets and dialogs that open their own window
@Composable
fun ImmersiveWindow() {
    val view = LocalView.current

    DisposableEffect(view) {
        // The window exists once the view is attached, which may happen after the first frame
        val listener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(attached: View) = hideSystemBars(attached)
            override fun onViewDetachedFromWindow(detached: View) = Unit
        }
        if (view.isAttachedToWindow) hideSystemBars(view) else view.addOnAttachStateChangeListener(listener)
        onDispose { view.removeOnAttachStateChangeListener(listener) }
    }
}

// The bars come back with a swipe and hide again, as in the rest of the app
private fun hideSystemBars(view: View) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        view.windowInsetsController?.apply {
            hide(WindowInsets.Type.systemBars())
            systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    } else {
        @Suppress("DEPRECATION")
        view.rootView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    }
}
