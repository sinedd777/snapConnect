# Technical Context

## Technologies Used

### Frontend
1. **Cross-Platform Development**
   - **Android**: Kotlin + Jetpack Compose
   - **iOS**: Swift + SwiftUI (planned)
2. **Jetpack Compose** - Modern UI toolkit for Android
   - State management
   - Navigation
   - Animations
3. **SwiftUI** - Modern UI toolkit for iOS (planned)
   - Declarative syntax
   - State management
   - Animations
4. **Accompanist** - Compose UI utilities
   - Permissions handling
   - System UI controller
   - Navigation animations
5. **CameraX** - Camera API for Android
   - Preview
   - Image capture
   - Video recording
6. **AVFoundation** - Camera API for iOS (planned)
   - Camera capture
   - Video recording
   - Media processing
7. **AR Components**
   - **Android**: ARCore for face tracking
   - **iOS**: ARKit for face tracking (planned)
   - 3D model rendering
   - Face mesh detection
8. **Map Integration**
   - OpenStreetMap for map data
   - Mapbox for rendering
   - Geofencing for location-based features

### Backend Services
1. **Firebase Authentication**
   - OAuth providers (Google, Apple, phone)
   - User management
   - Session handling
2. **Firebase Firestore**
   - NoSQL document database
   - Real-time updates
   - Offline support
   - Security rules
   - Composite indexes for complex queries
   - Geospatial queries
3. **Firebase Storage**
   - Media storage
   - Security rules
   - Upload/download management
4. **Firebase Cloud Messaging**
   - Push notifications
   - Topic subscriptions
5. **Firebase Cloud Functions**
   - Serverless backend logic
   - Event triggers
   - Scheduled tasks
6. **RAG Integration**
   - API integration with RAG provider
   - Context-aware suggestions
   - Event summarization

## Development Environment

### Required Tools
1. Android Studio (Android development)
2. Xcode (iOS development, planned)
3. Kotlin 1.8.0+
4. Swift 5.5+ (planned)
5. Gradle 8.0+
6. Firebase CLI
7. Git

### Device Requirements
1. Android 8.0+ (API level 26) / iOS 15+ (planned)
2. Camera hardware
3. Location services
4. ARCore/ARKit support for AR features
5. Internet connectivity

### Testing Devices
1. Android emulators
2. iOS simulators (planned)
3. Physical Android and iOS devices

## Dependencies

### Core Dependencies (Android)
```kotlin
// build.gradle.kts
dependencies {
    // Kotlin
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    
    // Jetpack Compose
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    
    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.0.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-functions-ktx")
    
    // CameraX
    implementation("androidx.camera:camera-camera2:1.2.3")
    implementation("androidx.camera:camera-lifecycle:1.2.3")
    implementation("androidx.camera:camera-view:1.2.3")
    implementation("androidx.camera:camera-video:1.2.3")
    
    // Accompanist
    implementation("com.google.accompanist:accompanist-permissions:0.30.1")
    
    // Image Loading
    implementation("io.coil-kt:coil-compose:2.4.0")
    
    // AR
    implementation("com.google.ar:core:1.40.0")
    implementation("io.github.sceneview:arsceneview:0.10.0")
    
    // Maps
    implementation("org.osmdroid:osmdroid-android:6.1.16")
    implementation("com.mapbox:mapbox-android-sdk:9.7.1")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    
    // OAuth
    implementation("com.google.android.gms:play-services-auth:20.6.0")
    implementation("com.facebook.android:facebook-login:16.1.3")
}
```

### Core Dependencies (iOS - Planned)
```swift
// Package.swift
dependencies: [
    .package(url: "https://github.com/firebase/firebase-ios-sdk.git", from: "10.0.0"),
    .package(url: "https://github.com/mapbox/mapbox-maps-ios.git", from: "10.0.0"),
    .package(url: "https://github.com/onevcat/Kingfisher.git", from: "7.0.0")
]
```

## Architecture

### App Structure
1. **UI Layer** - Jetpack Compose / SwiftUI components
   - Screens
   - Components
   - ViewModels
2. **Domain Layer** - Business logic
   - Repositories
   - Use Cases
3. **Data Layer** - Data sources
   - Firebase services
   - Local storage
   - Map providers

### State Management
1. Compose state / SwiftUI state
2. StateFlow/SharedFlow / Combine
3. ViewModel state

## Performance Considerations

### Map Performance
1. Tile caching
2. Viewport culling
3. Marker clustering
4. On-demand loading

### Camera Performance
1. CameraX / AVFoundation optimizations
2. Background thread processing
3. Image/video compression

### AR Performance
1. Model optimization
   - Low-poly models
   - Texture compression
2. Face tracking efficiency
3. Render thread management
4. Device compatibility handling

### Network Optimization
1. Image/video compression before upload
2. Batch operations
3. Offline support
4. Geospatial indexing

## Security Considerations

### Authentication
1. OAuth security
2. Session management
3. Account recovery

### Data Access
1. Firestore security rules
2. Storage security rules
3. User data isolation
4. Location data privacy

### Content Security
1. Media encryption
2. Expiring content
3. Screenshot detection
4. Memory Vault encryption

## Testing Strategy

### Unit Testing
1. Repository tests
2. ViewModel tests
3. Use case tests

### UI Testing
1. Compose UI tests / SwiftUI UI tests
2. Screen navigation tests
3. Map interaction tests

### Integration Testing
1. Firebase emulator tests
2. End-to-end flows
3. Location-based feature tests

## Deployment

### Release Process
1. Build variants
   - Debug
   - Release
2. Signing configuration
3. ProGuard rules

### Distribution
1. Google Play Store
2. Apple App Store (planned)
3. Firebase App Distribution for testing

## Monitoring

### Analytics
1. Firebase Analytics
2. Custom events
3. User properties
4. Conversion tracking

### Crash Reporting
1. Firebase Crashlytics
2. Exception tracking
3. Non-fatal error reporting
4. ANR detection

## Core Features

### Map Integration
1. OpenStreetMap data
2. Mapbox rendering
3. Circle pin visualization
4. Geofencing
5. Location filtering

### OAuth Authentication
1. Google Sign-In
2. Apple Sign-In
3. Phone number verification
4. College email verification (optional)

### College Town Selection
1. Predefined list of college towns
2. Custom location entry
3. Location verification
4. Campus boundary detection

### Circle Creation
1. Map-based pin placement
2. Duration selection (1 hour to 7 days)
3. Public/private visibility options
4. AR filter theme selection
5. Category assignment

### Content Sharing
1. Photo capture and sharing
2. Video recording (≤30 seconds)
3. Text posts (≤280 characters)
4. AR filter application
5. RAG-suggested captions

### Reactions and Interactions
1. Emoji reactions
2. Video reactions (≤5 seconds)
3. Group chat
4. Translation options
5. RAG-powered reply suggestions

### Memory Vault
1. Encrypted storage
2. Content organization
3. Export options
4. Access control

### Circle Summaries
1. AI-generated collages
2. Key moments selection
3. Context enhancement
4. Sharing options

## Technical Constraints

### Performance Requirements
1. Map rendering: 60 FPS
2. Camera launch < 1 second
3. Photo capture < 100ms
4. Video processing < 3 seconds
5. Circle pin rendering < 200ms for 100 pins
6. UI response time < 16ms (60 FPS)

### Storage Limits
1. Max photo size: 5MB
2. Max video length: 30 seconds
3. Max video size: 50MB
4. Text post limit: 280 characters
5. Cache size: 1GB
6. Offline storage: 500MB

### Network Requirements
1. Minimum upload speed: 1Mbps
2. Minimum download speed: 2Mbps
3. Offline functionality for core features
4. Graceful degradation on poor connection
5. Background sync for missed updates

### Location Requirements
1. Location accuracy: 10 meters
2. Geofencing precision: 50 meters
3. Background location updates: every 5 minutes
4. College town boundary precision: 100 meters

### Security Requirements
1. End-to-end media encryption
2. Secure key storage
3. Token-based authentication
4. Screenshot detection
5. Screen recording prevention
6. Location data privacy

## Development Practices

### Current Code Organization
```
app/
├── src/
│   ├── main/
│   │   ├── java/com/example/myapplication/
│   │   │   ├── data/
│   │   │   │   ├── models/
│   │   │   │   │   ├── Circle.kt
│   │   │   │   │   ├── User.kt
│   │   │   │   │   └── Snap.kt
│   │   │   │   └── repositories/
│   │   │   │       ├── CircleRepository.kt
│   │   │   │       ├── SnapRepository.kt
│   │   │   │       └── MapRepository.kt
│   │   │   ├── MainActivity.kt
│   │   │   ├── navigation/
│   │   │   │   └── AppNavGraph.kt
│   │   │   └── ui/
│   │   │       ├── auth/
│   │   │       ├── map/
│   │   │       ├── camera/
│   │   │       ├── circles/
│   │   │       └── profile/
```

### Testing Strategy
1. Unit Tests
   - Repository tests
   - ViewModel tests
   - Use case tests
   - Utility functions

2. Integration Tests
   - Map integration
   - Firebase interactions
   - Location services

3. UI Tests
   - Navigation flows
   - Map interactions
   - Camera functionality

### CI/CD Pipeline
1. GitHub Actions
2. Unit test execution
3. UI test execution
4. Build generation
5. Firebase App Distribution
6. Store deployment 