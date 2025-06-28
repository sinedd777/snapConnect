# SnapConnect

SnapConnect is an Android application built with Kotlin and Jetpack Compose that lets friends share **ephemeral photos & videos** with AR filters in a quick, privacy-centric way.  
The project is **work-in-progress** – the current milestone focuses on camera functionality with AR filters and the authentication flow.

---

## ✨ What Works Today

| Area | Status | Details |
|------|--------|---------|
| **Authentication (e-mail / password)** | ✅ Implemented | Firebase Auth powered sign-in & sign-up screens built with Compose. |
| **UI Theme** | ✅ Implemented | Material 3 theme, colour & typography system. |
| **Navigation** | ✅ Basic | Screen switching between Sign-In, Sign-Up, and Camera. |
| **Camera with AR Filters** | ✅ Implemented | DeepAR integration with multiple face filters. |
| **Project scaffolding** | ✅ Implemented | Gradle 8, Kotlin 2, Compose BOM, AGP 8.1. |

> Road-map highlights: Friend management, self-destructing snaps & push notifications (see `.cursor/memory-bank/progress.md`).

---

## 🛠️  Build & Run

### 1&nbsp;·&nbsp;Prerequisites

1. **Android Studio Giraffe/Koala** (use the latest stable).  
2. **JDK 17** (bundled with Android Studio).  
3. **Android SDK 35** platform + emulator/device.  
4. A **Firebase** project (to obtain API keys).

### 2&nbsp;·&nbsp;Clone the project

```bash
git clone https://github.com/<your-org>/snapConnect.git
cd snapConnect
```

### 3&nbsp;·&nbsp;Add your Firebase keys

Create a `local.properties` file (this file **must not** be committed) in the project root containing **your** Firebase credentials.  
These keys are read at build time to generate `app/google-services.json` automatically.

```properties
# local.properties (DO NOT CHECK IN!)
FIREBASE_API_KEY="<web-api-key>"
FIREBASE_APP_ID="<firebase-android-app-id>"
FIREBASE_MESSAGING_SENDER_ID="<sender-id>"
FIREBASE_PROJECT_ID="<project-id>"
GOOGLE_WEB_CLIENT_ID="<oauth-client-id>"

sdk_dir=<path-to-android-sdk>
```

> Tip: if you opened the project with Android Studio first, a default `local.properties` will already exist – simply append the Firebase lines.

### 4&nbsp;·&nbsp;Sync & build

**Android Studio**:  
1. Open the project (`File → Open`).  
2. Wait for Gradle Sync.  
3. Select the `app` configuration and press **Run ▶**.

**Command line** (requires `adb` on PATH):

```bash
./gradlew clean assembleDebug           # build APK
./gradlew installDebug                  # deploy to default device
```

### 5&nbsp;·&nbsp;Test the app

Launch the app on your emulator/device and:
1. Use the Sign-Up screen to create an account
2. Sign in with your credentials
3. Test the camera functionality with AR filters (requires camera permission)

---

## 📱 Features

### AR Filters with DeepAR

The app integrates DeepAR SDK to provide augmented reality face filters:

- 17+ AR filters including masks, effects, and animations
- Real-time face tracking and filter application
- Screenshot capability
- Filter carousel UI for easy selection

Camera permissions are required for AR functionality.

---

## 💡  Project Structure

```text
app/
 ├── build.gradle.kts          # Android module config
 ├── src/main/
 │    ├── java/com/example/myapplication/
 │    │    ├── ui/auth/        # Compose auth screens
 │    │    ├── ui/camera/      # Camera screen & DeepAR integration
 │    │    └── ui/camera/filters/ # AR filter management
 │    ├── assets/              # AR filter assets
 │    └── res/                 # M3 theming & assets
 └── libs/                     # DeepAR SDK
```

Configuration, architecture & roadmap live under `.cursor/memory-bank/` – start with `projectbrief.md` for full vision.

---

## 🧪 Development

### Running Tests

```bash
./gradlew test                 # Run unit tests
./gradlew connectedAndroidTest  # Run instrumented tests
```

### Debugging

- Use Android Studio's built-in debugger
- Logcat output includes DeepAR debug information (filter by "DeepARManager" tag)

---

## 📄 License

This project uses the DeepAR SDK which requires a license key for production use. The current implementation includes a development license.

---
