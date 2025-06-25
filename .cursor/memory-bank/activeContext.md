# Active Context

## Current Development Phase
Core functionality implemented with authentication, camera, friend management, and snap sharing. Firebase security rules configured, Firestore indexes deployed, and basic AR filter UI implemented.

## Recent Changes
1. Implemented friend management system:
   - Friend repository with search, requests, and listing
   - Friend request UI with accept/reject functionality
   - User search capability
2. Created snap sharing workflow:
   - Recipient selection screen
   - Snap upload with multiple recipients
   - Snap viewing with auto-destruction
3. Set up data models:
   - User model
   - Snap model
   - FriendRequest model
4. Configured Firebase security rules:
   - User document access control
   - Snap document permissions
   - Friendship document rules
5. Created Firestore indexes:
   - Composite index for querying snaps by recipients with ordering
6. Implemented basic AR filter UI:
   - Added AR dependencies
   - Created filter selection UI
   - Added toggle between normal camera and AR mode
   - Set up simplified AR filter manager

## Active Focus Areas
1. Firebase integration completion
2. UI polish and consistency
3. Performance optimization
4. Error handling improvements
5. User experience enhancements

## Next Steps
1. Complete AR filters implementation with real 3D models
   - Fix compatibility issues with ARCore/Sceneform library
   - Implement proper face tracking and model placement
   - Add real 3D models for filters
2. Set up push notifications for new snaps
3. Add offline support
4. Implement video recording capability
5. Add profile management features

## Current Challenges
1. Optimizing camera performance
2. Managing media storage efficiently
3. Implementing secure content delivery
4. Handling offline scenarios
5. Ensuring proper auto-destruction of content
6. ARCore/Sceneform library compatibility issues

## Technical Decisions
1. Using Jetpack Compose for UI
2. Firebase Auth for authentication
3. Firestore for user and snap data
4. Firebase Storage for media files
5. CameraX for camera implementation
6. Coil for image loading
7. ARCore and Sceneform for AR filters (simplified implementation for now)
8. WorkManager for background tasks (planned)

## Testing Status
1. Basic unit tests needed for auth flow
2. UI tests to be set up
3. Integration tests pending
4. Camera functionality tests needed
5. AR functionality tests needed

## Documentation Needs
1. Authentication flow documentation
2. Firebase setup guide
3. UI component documentation
4. Camera implementation guide
5. AR filter implementation guide
6. Testing strategy documentation

## Current Branch Structure
```
main
└── feature/friend-management (completed)
    └── feature/snap-viewing (completed)
        └── feature/security-rules (completed)
            └── feature/firestore-indexes (completed)
                └── feature/ar-filters (in progress)
```

## Active Issues
1. Media compression for efficient storage
2. Advanced screenshot prevention
3. Push notification setup
4. Offline support implementation
5. Profile management features
6. AR model loading and face tracking

## Recent Pull Requests
1. Authentication flow implementation (merged)
2. Camera functionality (merged)
3. Friend management system (merged)
4. Snap viewing with auto-destruction (merged)
5. Firebase security rules (merged)
6. Firestore indexes for snap queries (merged)
7. Basic AR filters UI implementation (in progress)

## Immediate TODOs
1. Fix ARCore/Sceneform compatibility issues
2. Complete AR filters implementation with real 3D models
3. Set up push notifications for new snaps
4. Add offline queue for snap uploads
5. Implement video recording capability
6. Create profile editing screen

## Current Dependencies
See `build.gradle.kts` for full list:
1. Firebase Auth, Firestore, Storage
2. Jetpack Compose
3. Navigation Components
4. CameraX
5. Accompanist Permissions
6. Coil for image loading
7. ARCore and Sceneform for AR
8. Kotlin Coroutines 