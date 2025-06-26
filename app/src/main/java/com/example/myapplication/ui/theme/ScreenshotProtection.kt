package com.example.myapplication.ui.theme

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * A utility composable that applies FLAG_SECURE to prevent screenshots and screen recordings
 * when the composable is active, and removes the flag when it's disposed.
 * 
 * This should be used in screens that display sensitive content like snaps, camera preview,
 * or any content that should not be captured in screenshots.
 */
@Composable
fun ScreenshotProtection() {
    val context = LocalContext.current
    
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.window?.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        
        onDispose {
            // Remove FLAG_SECURE when the composable is disposed
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
} 