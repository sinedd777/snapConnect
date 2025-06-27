# System Patterns

## Architecture Overview

### Core Components
1. Cross-Platform Mobile App (iOS and Android)
   - Android: Kotlin + Jetpack Compose
   - iOS: Swift + SwiftUI (planned)
2. Firebase Backend Services
   - Authentication (OAuth providers)
   - Firestore Database
   - Cloud Storage
   - Cloud Messaging (planned)
3. Cloud Functions (planned)
4. Map Integration
   - OpenStreetMap for map rendering
   - Geofencing for location-based features
5. AR Components
   - ARCore/ARKit for face tracking
   - 3D model rendering for filters

## Data Models

### User
```kotlin
data class User(
    val uid: String,
    val email: String,
    val username: String? = null,
    val avatarUrl: String? = null,
    val createdAt: Timestamp? = null,
    val collegeTown: String? = null,
    val oauthProvider: String? = null
) {
    companion object {
        fun fromMap(map: Map<String, Any>): User {
            return User(
                uid = map["uid"] as String,
                email = map["email"] as String,
                username = map["username"] as? String,
                avatarUrl = map["avatarUrl"] as? String,
                createdAt = map["createdAt"] as? Timestamp,
                collegeTown = map["collegeTown"] as? String,
                oauthProvider = map["oauthProvider"] as? String
            )
        }
    }
}
```

### Circle
```kotlin
data class Circle(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String? = null,
    val creatorId: String,
    val members: List<String> = emptyList(),
    val pendingInvites: List<String> = emptyList(),
    val createdAt: Timestamp = Timestamp.now(),
    val expiresAt: Timestamp? = null,
    val startTime: Timestamp? = null,
    val locationEnabled: Boolean = true,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val locationRadius: Double? = null,
    val isPrivate: Boolean = true,
    val inviteCode: String? = null,
    val category: String? = null,
    val arFilterId: String? = null,
    val collegeTown: String? = null
) {
    companion object {
        fun fromMap(map: Map<String, Any>): Circle {
            return Circle(
                id = map["id"] as String,
                name = map["name"] as String,
                description = map["description"] as? String,
                creatorId = map["creatorId"] as String,
                members = (map["members"] as? List<String>) ?: emptyList(),
                pendingInvites = (map["pendingInvites"] as? List<String>) ?: emptyList(),
                createdAt = map["createdAt"] as Timestamp,
                expiresAt = map["expiresAt"] as? Timestamp,
                startTime = map["startTime"] as? Timestamp,
                locationEnabled = (map["locationEnabled"] as? Boolean) ?: true,
                locationLat = map["locationLat"] as? Double,
                locationLng = map["locationLng"] as? Double,
                locationRadius = map["locationRadius"] as? Double,
                isPrivate = (map["isPrivate"] as? Boolean) ?: true,
                inviteCode = map["inviteCode"] as? String,
                category = map["category"] as? String,
                arFilterId = map["arFilterId"] as? String,
                collegeTown = map["collegeTown"] as? String
            )
        }
    }
}
```

### Snap
```kotlin
data class Snap(
    val id: String,
    val sender: String,
    val senderName: String? = null,
    val circleId: String? = null,
    val recipients: List<String> = emptyList(),
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val textContent: String? = null,
    val createdAt: Timestamp,
    val expiresAt: Timestamp? = null,
    val viewedBy: List<String> = emptyList(),
    val screenshotBy: List<String> = emptyList(),
    val reactions: Map<String, String> = emptyMap(),
    val videoReactions: Map<String, String> = emptyMap(),
    val arFilterId: String? = null
) {
    val isViewed: Boolean
        get() = viewedBy.isNotEmpty()
        
    val isScreenshotted: Boolean
        get() = screenshotBy.isNotEmpty()
        
    companion object {
        fun fromMap(map: Map<String, Any>): Snap {
            return Snap(
                id = map["id"] as String,
                sender = map["sender"] as String,
                senderName = map["senderName"] as? String,
                circleId = map["circleId"] as? String,
                recipients = (map["recipients"] as? List<String>) ?: emptyList(),
                mediaUrl = map["mediaUrl"] as? String,
                mediaType = map["mediaType"] as? String,
                textContent = map["textContent"] as? String,
                createdAt = map["createdAt"] as Timestamp,
                expiresAt = map["expiresAt"] as? Timestamp,
                viewedBy = (map["viewedBy"] as? List<String>) ?: emptyList(),
                screenshotBy = (map["screenshotBy"] as? List<String>) ?: emptyList(),
                reactions = (map["reactions"] as? Map<String, String>) ?: emptyMap(),
                videoReactions = (map["videoReactions"] as? Map<String, String>) ?: emptyMap(),
                arFilterId = map["arFilterId"] as? String
            )
        }
    }
}
```

### ARFilter
```kotlin
data class ARFilter(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val modelPath: String,
    val thumbnailUrl: String? = null,
    val category: String? = null,
    val isDefault: Boolean = false
)
```

### ChatMessage
```kotlin
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val circleId: String,
    val senderId: String,
    val senderName: String? = null,
    val content: String,
    val createdAt: Timestamp = Timestamp.now(),
    val reactions: Map<String, String> = emptyMap(),
    val translatedContent: Map<String, String> = emptyMap()
)
```

### CircleSummary
```kotlin
data class CircleSummary(
    val id: String,
    val circleId: String,
    val title: String,
    val description: String? = null,
    val collageUrls: List<String> = emptyList(),
    val highlightSnapIds: List<String> = emptyList(),
    val aiGeneratedText: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val eventContext: Map<String, Any> = emptyMap()
)
```

## Core Repositories

### CircleRepository
```kotlin
class CircleRepository {
    private val firestore: FirebaseFirestore = Firebase.firestore
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    
    // Collection names
    companion object {
        private const val CIRCLES_COLLECTION = "circles"
        private const val USERS_COLLECTION = "users"
        
        // Circle duration options in hours
        val DURATION_1_HOUR = TimeUnit.HOURS.toMillis(1)
        val DURATION_24_HOURS = TimeUnit.HOURS.toMillis(24)
        val DURATION_48_HOURS = TimeUnit.HOURS.toMillis(48)
        val DURATION_72_HOURS = TimeUnit.HOURS.toMillis(72)
        val DURATION_7_DAYS = TimeUnit.DAYS.toMillis(7)
    }
    
    /**
     * Create a new Circle with location
     */
    suspend fun createCircle(
        name: String,
        description: String? = null,
        durationMillis: Long = DURATION_24_HOURS,
        startTimeMillis: Long? = null,
        isPrivate: Boolean = true,
        locationEnabled: Boolean = true,
        locationLat: Double? = null,
        locationLng: Double? = null,
        locationRadius: Double? = null,
        initialMembers: List<String> = emptyList(),
        category: String? = null,
        arFilterId: String? = null
    ): Result<Circle> {
        // Implementation details
    }
    
    /**
     * Get nearby Circles based on location and radius
     */
    suspend fun getNearbyCircles(
        lat: Double,
        lng: Double,
        radiusKm: Double = 1.0,
        limit: Long = 50
    ): Result<List<Circle>> {
        // Implementation details
    }
    
    /**
     * Get Circles for a specific college town
     */
    suspend fun getCollegeTownCircles(
        collegeTown: String,
        limit: Long = 50
    ): Result<List<Circle>> {
        // Implementation details
    }
    
    /**
     * Join a public Circle
     */
    suspend fun joinPublicCircle(circleId: String): Result<Unit> {
        // Implementation details
    }
    
    /**
     * Join a private Circle with invite code
     */
    suspend fun joinPrivateCircle(circleId: String, inviteCode: String): Result<Unit> {
        // Implementation details
    }
    
    /**
     * Generate a unique invite code for a private Circle
     */
    suspend fun generateInviteCode(circleId: String): Result<String> {
        // Implementation details
    }
}
```

### SnapRepository
```kotlin
class SnapRepository {
    private val storage = Firebase.storage
    private val firestore = Firebase.firestore
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    /**
     * Upload a photo snap to a Circle
     */
    suspend fun uploadPhotoSnap(
        localUri: Uri,
        circleId: String,
        arFilterId: String? = null
    ): Result<Snap> {
        // Implementation details
    }
    
    /**
     * Upload a video snap to a Circle
     */
    suspend fun uploadVideoSnap(
        localUri: Uri,
        circleId: String,
        arFilterId: String? = null
    ): Result<Snap> {
        // Implementation details
        }

    /**
     * Create a text snap in a Circle
     */
    suspend fun createTextSnap(
        text: String,
        circleId: String
    ): Result<Snap> {
        // Implementation details
    }
    
    /**
     * Get snaps for a specific Circle
     */
    suspend fun getCircleSnaps(circleId: String, limit: Long = 50): Result<List<Snap>> {
        // Implementation details
    }
    
    /**
     * Add a reaction to a snap
     */
    suspend fun addReaction(snapId: String, emoji: String): Result<Unit> {
        // Implementation details
    }
    
    /**
     * Add a video reaction to a snap
     */
    suspend fun addVideoReaction(snapId: String, reactionUri: Uri): Result<Unit> {
        // Implementation details
    }
    
    /**
     * Save a snap to Memory Vault
     */
    suspend fun saveToVault(snapId: String): Result<Unit> {
        // Implementation details
    }
}
```

### ChatRepository
```kotlin
class ChatRepository {
    private val firestore = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    /**
     * Send a message in a Circle chat
     */
    suspend fun sendMessage(circleId: String, content: String): Result<ChatMessage> {
        // Implementation details
    }
    
    /**
     * Get messages for a Circle chat
     */
    suspend fun getMessages(circleId: String, limit: Long = 50): Result<List<ChatMessage>> {
        // Implementation details
    }
    
    /**
     * Translate a message to a different language
     */
    suspend fun translateMessage(messageId: String, targetLanguage: String): Result<String> {
        // Implementation details
    }
    
    /**
     * Get RAG-powered reply suggestions
     */
    suspend fun getSuggestedReplies(circleId: String, messageContext: List<String>): Result<List<String>> {
        // Implementation details
    }
}
```

### MapRepository
```kotlin
class MapRepository {
    private val firestore = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()
    
    /**
     * Get college towns list
     */
    suspend fun getCollegeTowns(): Result<List<String>> {
        // Implementation details
    }
    
    /**
     * Verify if location is within a college town
     */
    suspend fun verifyCollegeTownLocation(lat: Double, lng: Double): Result<String?> {
        // Implementation details
    }
    
    /**
     * Get map data for a specific college town
     */
    suspend fun getCollegeTownMapData(collegeTown: String): Result<Map<String, Any>> {
        // Implementation details
    }
}
```

## Navigation Structure
```
AppNavGraph
├── AuthFlow
│   ├── SignInScreen
│   ├── SignUpScreen
│   └── CollegeTownSelectionScreen
├── MainFlow
│   ├── MapScreen
│   ├── CirclesScreen
│   ├── CameraScreen
│   │   └── RecipientSelectorScreen
│   ├── ProfileScreen
│   └── NotificationsScreen
├── CircleFlow
│   ├── CircleDetailScreen
│   ├── CircleStoryScreen
│   └── CircleChatScreen
└── CreationFlow
    ├── CreateCircleScreen
    ├── CircleLocationScreen
    └── CircleInviteScreen
```

## Core Workflows

### Map Rendering and Circle Display
1. Fetch user's location via device GPS
2. Query OpenStreetMap API for college town map (1 km radius)
3. Retrieve active/upcoming Circles from database, filter by location and user preferences
4. Calculate Circle pin sizes based on participant count (e.g., 10px base + 2px per 5 users)
5. Render map with pins, update every 5 seconds for real-time changes

### Circle Creation and Management
1. Store Circle details (title, description, location, duration, privacy) in database
2. Validate location within 1 km radius using geofencing
3. Generate unique invite code for private Circles
4. Update Circle size on map as users join (recalculate every minute)
5. Schedule auto-deletion of Circle data post-expiration

### Snap Processing
1. Receive uploaded snap (photo, video, text) and validate format/size (≤30 secs, ≤280 chars)
2. Apply selected AR filter using ARKit/ARCore
3. Store snap temporarily in database, tagged to Circle
4. Push snap to story feed for all Circle members
5. Delete snap when Circle expires

### RAG-Powered Suggestions
1. Query public APIs (X, Eventbrite, news feeds) for trending events/topics in user's location
2. Use RAG model to generate captions, hashtags, or filter suggestions
3. Cache results for 1 hour to reduce API calls
4. Deliver suggestions to app in <2 seconds

### Reaction Handling
1. Receive emoji or video reaction (≤5 secs) and validate format
2. Store reaction in database, linked to snap
3. Broadcast reaction to all Circle members in real-time via WebSocket
4. Delete reactions with snap on Circle expiration

### Chat Management
1. Store chat messages (text, emojis, GIFs) in encrypted database, linked to Circle
2. Apply RAG-powered translations if requested (using public translation API)
3. Push messages to Circle members via real-time notifications
4. Delete chat history when Circle expires

### Memory Vault Storage
1. Receive user's save request for snap
2. Encrypt snap (AES-256) and store in user's vault (local device or cloud)
3. Provide export option as ZIP file
4. Ensure vault access requires user authentication

### Summary Generation
1. On Circle expiration, query database for snaps and metadata
2. Use RAG to fetch public event context (e.g., X trends, Eventbrite data)
3. Generate summary (collage, key snaps, text) using AI model
4. Deliver summary to Circle members within 5 minutes
5. Store summary temporarily (7 days) unless saved to vault 