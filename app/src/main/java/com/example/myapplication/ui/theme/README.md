# Screenshot Prevention in SnapConnect

This document explains how screenshot prevention is implemented in the SnapConnect app to protect user privacy and content.

## Overview

SnapConnect prevents screenshots in sensitive areas of the app, such as:
- Snap viewing screens
- Camera screens
- Recipient selection screens

## Implementation

### ScreenshotProtection Utility

We use a utility composable called `ScreenshotProtection` that applies Android's `FLAG_SECURE` to prevent screenshots and screen recordings:

```kotlin
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
```

This utility is used in sensitive screens by simply adding:

```kotlin
@Composable
fun SensitiveScreen() {
    // Apply screenshot protection
    ScreenshotProtection()
    
    // Rest of the screen content
    // ...
}
```

## How FLAG_SECURE Works

When `FLAG_SECURE` is applied to a window:

1. Android prevents screenshots from being taken (users will see a message that "Taking screenshots isn't allowed by the app")
2. The app content is hidden from appearing in the recent apps screen/multitasking view
3. Screen recording apps will not be able to capture the content
4. The content won't be visible on non-secure displays (like when casting the screen)

## Limitations

- `FLAG_SECURE` must be applied before the content is displayed
- It doesn't prevent sophisticated attacks like optical capture (taking a photo of the screen with another device)
- Some device manufacturers may implement this flag differently

## Future Improvements

Potential future improvements could include:
- Adding visual watermarks to sensitive content
- Implementing more sophisticated screenshot detection mechanisms
- Adding user notifications when screenshot attempts are detected 