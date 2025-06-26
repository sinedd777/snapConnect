# System Patterns

## Architecture Overview

### Core Components
1. Android App (Kotlin + Jetpack Compose)
2. Firebase Backend Services
   - Authentication (Email/Password)
   - Firestore Database
   - Cloud Storage
   - Cloud Messaging (planned)
3. Cloud Functions (planned)
4. AR Components
   - ARCore for face tracking (planned)
   - Sceneform for 3D rendering (planned)

## Data Models

### User
```kotlin
data class User(
    val uid: String,
    val email: String,
    val username: String? = null,
    val avatarUrl: String? = null,
    val createdAt: Timestamp? = null
) {
    companion object {
        fun fromMap(map: Map<String, Any>): User {
            return User(
                uid = map["uid"] as String,
                email = map["email"] as String,
                username = map["username"] as? String,
                avatarUrl = map["avatarUrl"] as? String,
                createdAt = map["createdAt"] as? Timestamp
            )
        }
    }
}
```

### Friendship / FriendRequest
```kotlin
data class FriendRequest(
    val id: String,
    val userA: String,
    val userB: String,
    val status: String,
    val createdAt: Timestamp,
    val requesterDetails: User? = null
) {
    companion object {
        fun fromMap(map: Map<String, Any>): FriendRequest {
            val requesterDetails = (map["requesterDetails"] as? Map<String, Any>)?.let {
                User.fromMap(it)
            }
            
            return FriendRequest(
                id = map["id"] as String,
                userA = map["userA"] as String,
                userB = map["userB"] as String,
                status = map["status"] as String,
                createdAt = map["createdAt"] as Timestamp,
                requesterDetails = requesterDetails
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
    val recipients: List<String>,
    val mediaUrl: String,
    val mediaType: String,
    val createdAt: Timestamp,
    val expiresAt: Timestamp? = null,
    val viewedBy: List<String> = emptyList(),
    val screenshotBy: List<String> = emptyList()
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
                recipients = map["recipients"] as List<String>,
                mediaUrl = map["mediaUrl"] as String,
                mediaType = map["mediaType"] as String,
                createdAt = map["createdAt"] as Timestamp,
                expiresAt = map["expiresAt"] as? Timestamp,
                viewedBy = (map["viewedBy"] as? List<String>) ?: emptyList(),
                screenshotBy = (map["screenshotBy"] as? List<String>) ?: emptyList()
            )
        }
    }
}
```

### ARFilter
```kotlin
data class ARFilter(
    val name: String,
    val modelPath: String
)
```

### Notification
```kotlin
data class Notification(
    val id: String,
    val userId: String,
    val type: String,
    val payload: Map<String, Any>,
    val read: Boolean,
    val createdAt: Timestamp
)
```

### ModerationItem
```kotlin
data class ModerationItem(
    val id: String,
    val contentId: String,
    val flaggerId: String,
    val reason: String,
    val status: String,
    val createdAt: Timestamp
)
```

## Implemented Repository Patterns

### SnapRepository
```kotlin
class SnapRepository {
    private val storage = Firebase.storage
    private val firestore = Firebase.firestore
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    suspend fun uploadSnap(localUri: Uri, recipients: List<String>? = null): Result<Uri> = try {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("No user"))
        val snapId = UUID.randomUUID().toString()
        val ref = storage.reference.child("snaps/$uid/$snapId.jpg")
        ref.putFile(localUri).await()
        val downloadUrl = ref.downloadUrl.await()

        // Use provided recipients or default to just the sender
        val finalRecipients = if (recipients.isNullOrEmpty()) {
            listOf(uid)
        } else {
            // Always include the sender in recipients
            if (recipients.contains(uid)) recipients else recipients + uid
        }

        // Store metadata
        val data = mapOf(
            "id" to snapId,
            "sender" to uid,
            "mediaUrl" to downloadUrl.toString(),
            "mediaType" to "image/jpeg",
            "createdAt" to com.google.firebase.Timestamp.now(),
            "recipients" to finalRecipients,
            "viewedBy" to listOf<String>(),
            "screenshotBy" to listOf<String>()
        )
        firestore.collection("snaps").document(snapId).set(data).await()

        Result.success(downloadUrl)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    suspend fun getSnapsForUser(limit: Long = 20): Result<List<Snap>> = try {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("No user"))
        
        // Query for snaps where the current user is a recipient, ordered by creation time
        val snapDocs = firestore.collection("snaps")
            .whereArrayContains("recipients", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()
            
        // Process results
        val snaps = snapDocs.documents.mapNotNull { doc ->
            val data = doc.data ?: return@mapNotNull null
            data["id"] = doc.id
            Snap.fromMap(data)
        }
        
        Result.success(snaps)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    suspend fun markSnapViewed(snapId: String): Result<Unit> = try {
        // Implementation details...
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    suspend fun markSnapScreenshot(snapId: String): Result<Unit> = try {
        // Implementation details...
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    suspend fun getSnapById(snapId: String): Result<Snap> {
        // Implementation details...
    }
}
```

### FriendRepository
```kotlin
class FriendRepository {
    private val firestore = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    // Friend request statuses
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_ACCEPTED = "accepted"
        const val STATUS_REJECTED = "rejected"
    }

    suspend fun sendFriendRequest(targetUserId: String): Result<Unit> = try {
        // Implementation details...
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    suspend fun acceptFriendRequest(requestId: String): Result<Unit> {
        // Implementation details...
    }
    
    suspend fun rejectFriendRequest(requestId: String): Result<Unit> {
        // Implementation details...
    }
    
    suspend fun getFriends(): Result<List<User>> = try {
        // Implementation details...
        Result.success(friends)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    suspend fun getPendingFriendRequests(): Result<List<FriendRequest>> = try {
        // Implementation details...
        Result.success(pendingRequests)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    suspend fun searchUsers(query: String): Result<List<User>> = try {
        // Implementation details...
        Result.success(users)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### ARFilterManager (Simplified)
```kotlin
class ARFilterManager(
    private val context: Context,
    private val arSceneView: ArSceneView,
    private val coroutineScope: CoroutineScope
) : DefaultLifecycleObserver {
    
    // Currently active filter
    private var currentFilter: ARFilter? = null
    
    // Available filters
    private val availableFilters = listOf(
        ARFilter("Sunglasses", "models/sunglasses.glb"),
        ARFilter("Party Hat", "models/party_hat.glb"),
        ARFilter("Bunny Ears", "models/bunny_ears.glb"),
        ARFilter("Face Mask", "models/face_mask.glb")
    )
    
    init {
        // Configure AR scene
        arSceneView.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
        arSceneView.depthEnabled = true
        arSceneView.instantPlacementEnabled = true
    }
    
    fun getAvailableFilters(): List<ARFilter> = availableFilters
    
    fun applyFilter(filter: ARFilter) {
        // Remove any existing filter
        removeCurrentFilter()
        
        // Store the current filter
        currentFilter = filter
        
        // In a real implementation, this would load and apply a 3D model
        println("Applying filter: ${filter.name}")
    }
    
    fun removeCurrentFilter() {
        if (currentFilter != null) {
            println("Removing filter: ${currentFilter?.name}")
            currentFilter = null
        }
    }
    
    suspend fun takeScreenshot(): Uri? = withContext(Dispatchers.IO) {
        try {
            // Create a temporary file for the screenshot
            val file = File(context.cacheDir, "ar_screenshot_${System.currentTimeMillis()}.jpg")
            
            // In a real implementation, this would capture the AR view
            // For now, we'll just create a simple bitmap
            val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
            
            // Save the bitmap to the file
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            
            // Return the URI of the saved file
            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    override fun onDestroy(owner: LifecycleOwner) {
        removeCurrentFilter()
        super.onDestroy(owner)
    }
}
```

## Navigation Structure
```
AppNavGraph
├── AuthScreen
│   ├── SignInScreen
│   └── SignUpScreen
├── HomeScreen
├── CameraScreen
│   └── RecipientSelectorScreen
├── SnapViewerScreen
└── FriendsScreen
```

## UI Components

### Authentication Components
1. `AuthScreen` - Container for authentication flow
2. `SignInScreen` - Email/password login
3. `SignUpScreen` - New user registration

### Camera Components
1. `CameraScreen` - Camera preview and capture
   - CameraX integration
   - Permission handling
   - Photo capture
   - Upload functionality
   - AR mode toggle
   - AR filter selection
2. `RecipientSelectorScreen` - Friend selection for sharing

### AR Components
1. `ARFilterManager` - Manages AR filter functionality (simplified)
   - Filter selection
   - Filter removal
   - Screenshot capture
2. `FilterSelector` - UI for selecting AR filters
   - Filter list display
   - Filter selection
   - Filter preview

### Home Components
1. `HomeScreen` - Main screen after authentication
   - Snap list display
   - Navigation to other screens
   - Refresh functionality

### Snap Viewing Components
1. `SnapViewerScreen` - Viewing received snaps
   - Auto-destruction countdown
   - Image display with Coil
   - Screenshot detection

### Friend Management Components
1. `FriendsScreen` - Friend management interface
   - Friend list display
   - Friend request handling
   - User search functionality

## Security Patterns

### Firebase Security Rules
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{db}/documents {
    // Helper functions for circle membership and queries
    function hasCircleIdFilter() {
      return request.query.filters.size() >= 1 && 
             request.query.filters[0].fieldPath == "circleId";
    }
    
    function getCircleIdFromQuery() {
      return request.query.filters[0].value;
    }
    
    function isCircleMember(circleId) {
      let circlePath = /databases/$(db)/documents/circles/$(circleId);
      return exists(circlePath) && 
             (request.auth.uid in get(circlePath).data.members || 
              get(circlePath).data.creatorId == request.auth.uid);
    }
    
    // User docs
    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth.uid == userId;
    }
    
    // Snap docs
    match /snaps/{snapId} {
      // Allow read if the user is the sender or a recipient
      allow read: if request.auth != null && 
                   (resource.data.sender == request.auth.uid || 
                    request.auth.uid in resource.data.recipients);
      
      // Allow creation if the user is the sender
      allow create: if request.auth != null && 
                     request.resource.data.sender == request.auth.uid;
      
      // Allow updates if the user is the sender or a recipient
      allow update: if request.auth != null && 
                     (resource.data.sender == request.auth.uid || 
                      request.auth.uid in resource.data.recipients);
      
      // Allow reading individual snaps by circleId if the user is a member of the circle
      allow get: if request.auth != null && 
                   resource.data.circleId != null &&
                   exists(/databases/$(db)/documents/circles/$(resource.data.circleId)) &&
                   (request.auth.uid in get(/databases/$(db)/documents/circles/$(resource.data.circleId)).data.members ||
                    get(/databases/$(db)/documents/circles/$(resource.data.circleId)).data.creatorId == request.auth.uid);
                    
      // Allow any authenticated user to list snaps
      allow list: if request.auth != null;
    }
    
    // Friendship docs
    match /friendships/{friendshipId} {
      allow read, write: if request.auth != null && 
                          (request.auth.uid == resource.data.userA || 
                           request.auth.uid == resource.data.userB);
      allow create: if request.auth != null && 
                     request.resource.data.userA == request.auth.uid;
    }
    
    // Circle docs
    match /circles/{circleId} {
      // Allow read if user is a member or has a pending invitation
      allow read: if request.auth != null && (
                   (resource != null && resource.data != null && 
                    (request.auth.uid in resource.data.members || 
                     (resource.data.pendingInvites != null && request.auth.uid in resource.data.pendingInvites)))
                   ||
                   // Allow listing all circles (for queries)
                   request.path.size() == 2
                  );
      
      // Allow creation if the creator is the current user
      allow create: if request.auth != null && 
                     request.resource.data.creatorId == request.auth.uid;
      
      // Allow updates if the user is the creator or is accepting/declining an invitation
      allow update: if request.auth != null && (
                     // Creator can update most fields
                     resource.data.creatorId == request.auth.uid ||
                     
                     // Members can only update specific fields related to their membership
                     (request.auth.uid in resource.data.members && 
                      onlyUpdatingMemberFields()) ||
                     
                     // Invited users can accept/decline invitations
                     (resource.data.pendingInvites != null && 
                      request.auth.uid in resource.data.pendingInvites && 
                      onlyAcceptingOrDecliningInvitation())
                   );
      
      // Allow deletion only by the creator
      allow delete: if request.auth != null && 
                     resource.data.creatorId == request.auth.uid;
      
      // Helper function to check if only member-related fields are being updated
      function onlyUpdatingMemberFields() {
        let allowedFields = ['members'];
        let changedFields = request.resource.data.diff(resource.data).affectedKeys();
        return changedFields.hasOnly(allowedFields);
      }
      
      // Helper function to check if only accepting/declining invitation
      function onlyAcceptingOrDecliningInvitation() {
        let allowedFields = ['members', 'pendingInvites'];
        let changedFields = request.resource.data.diff(resource.data).affectedKeys();
        return changedFields.hasOnly(allowedFields) && 
               (request.resource.data.pendingInvites == null || 
                !request.resource.data.pendingInvites.hasAll([request.auth.uid]));
      }
    }
  }
}
```

### Firestore Indexes
```json
{
  "indexes": [
    {
      "collectionGroup": "snaps",
      "queryScope": "COLLECTION",
      "fields": [
        {
          "fieldPath": "recipients",
          "arrayConfig": "CONTAINS"
        },
        {
          "fieldPath": "createdAt",
          "order": "DESCENDING"
        },
        {
          "fieldPath": "__name__",
          "order": "DESCENDING"
        }
      ]
    },
    {
      "collectionGroup": "snaps",
      "queryScope": "COLLECTION",
      "fields": [
        {
          "fieldPath": "circleId",
          "order": "ASCENDING"
        },
        {
          "fieldPath": "createdAt",
          "order": "DESCENDING"
        }
      ]
    },
    {
      "collectionGroup": "snaps",
      "queryScope": "COLLECTION",
      "fields": [
        {
          "fieldPath": "circleId",
          "order": "ASCENDING"
        },
        {
          "fieldPath": "createdAt",
          "order": "DESCENDING"
        },
        {
          "fieldPath": "__name__",
          "order": "DESCENDING"
        }
      ]
    }
  ],
  "fieldOverrides": []
}
```

## Background Tasks (Planned)

### WorkManager Jobs
1. Offline snap upload queue
2. Media compression
3. Token refresh
4. Contact sync

### Cloud Functions (Planned)
1. Snap expiration
2. Push notification dispatch
3. Media cleanup
4. Moderation actions

## Error Handling
1. Toast messages for user feedback
2. Result pattern for repository operations
3. Exception handling in suspend functions
4. Graceful degradation for network issues
5. Robust null handling in data processing 