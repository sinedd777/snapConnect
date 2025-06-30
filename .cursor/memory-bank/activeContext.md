# Active Context

## Current Development Phase
Pivoting from individual snap sharing to map-based, college-focused Circle platform. Core functionality implemented with authentication, camera, friend management, and snap sharing. Firebase security rules configured, Firestore indexes deployed, and basic AR filter UI implemented. Transitioning to map-based interface with college town selection and RAG integration. UI enhanced with Material 3 design system. Map functionality with location services implemented, including fullscreen mode and circle display. Screenshot prevention implemented for sensitive content.

## Product Pivot
We are pivoting the app to focus on:
1. Map-based Circle discovery and interaction
2. College town-specific experiences
3. Location-based sharing
4. Enhanced RAG features for event discovery and content enhancement
5. Collaborative, ephemeral sharing spaces tied to physical locations

## User Flows Being Implemented
1. Set Up Account with college town selection
2. Discover Circles on Map (1 km radius)
3. Create a Circle with location pinning
4. Share Snaps in a Circle (photos, videos, text)
5. React to Snaps with emoji or video reactions
6. Chat in a Circle with RAG-powered suggestions
7. Save content to Memory Vault
8. View AI-generated Circle summaries post-expiration
9. Receive event recommendations

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
   - Circle model
   - RAG-specific models (OpenAI integration)
4. Configured Firebase security rules:
   - User document access control
   - Snap document permissions
   - Friendship document rules
   - Circle document permissions
   - Location update permissions
5. Created Firestore indexes:
   - Composite index for querying snaps by recipients with ordering
   - Composite index for querying snaps by circleId with ordering
   - Composite index for querying circles by isPrivate and createdAt
6. Implemented basic AR filter UI:
   - Added AR dependencies
   - Created filter selection UI
   - Added toggle between normal camera and AR mode
   - Set up simplified AR filter manager
   - Added video recording support
7. Enhanced UI with Material 3:
   - Implemented proper Material 3 theming
   - Added custom color schemes for light/dark modes
   - Created custom shapes for consistent UI elements
   - Enhanced component styling with Material 3 guidelines
   - Improved visual hierarchy and spacing
8. Added profile screen:
   - User details display
   - Account information section
   - Settings section
   - Logout functionality
   - Material 3 styling
9. Implemented map-based interface:
   - Created MapComponent for visualizing Circles
   - Implemented CircleMapScreen and CircleMapViewModel
   - Added filter controls for Circle discovery
   - Integrated location permissions handling
   - Created MapRepository for location-based operations
   - Added fullscreen map mode with animations
   - Implemented circle display with bottom sheet information
   - Added real-time location updates
10. Added college town selection:
    - Implemented CollegeTownSelectionScreen
    - Created CollegeTownViewModel with location detection
    - Added college town data to User model
    - Integrated with onboarding flow
11. Enhanced Circle creation:
    - Added location selection to CreateCircleScreen
    - Implemented radius controls for location-based Circles
    - Added public/private options for Circle visibility
    - Integrated with map-based discovery
    - Made location enabled by default
    - Added permission handling during circle creation
12. Implemented screenshot prevention:
    - Added FLAG_SECURE to sensitive screens
    - Created ScreenshotProtection composable
    - Protected snap viewing screens
    - Protected camera screens
    - Protected recipient selection screens
13. Added RAG integration:
    - Created RAG-specific data models
    - Implemented OpenAI service integration
    - Added circle context analysis
    - Implemented temporal pattern analysis
    - Added engagement metrics tracking

## Active Focus Areas
1. RAG integration for content enhancement
2. OAuth authentication implementation
3. Video recording implementation (≤30 secs)
4. Text post creation (≤280 chars)
5. Collaborative story features
6. Group chat functionality
7. Memory Vault implementation
8. Circle expiration and summary generation
9. Event recommendation system
10. OpenStreetMap integration for improved map experience

## Next Steps
1. Implement OAuth authentication:
   - Google integration
   - Apple integration
   - Phone number verification
2. Enhance map interface with OpenStreetMap:
   - Replace placeholder map with actual map tiles
   - Implement proper geolocation
   - Add map caching for offline use
3. Add video recording capability:
   - 30-second limit
   - Compression
   - Thumbnail generation
4. Add text post creation:
   - 280-character limit
   - Background options
   - Font customization
5. Implement group chat:
   - Real-time messaging
   - RAG-powered suggestions
   - Translation options
6. Set up RAG integration:
   - API provider selection
   - Data source connections
   - Caption suggestions
   - Event context enhancement
7. Create Circle summary generation:
   - AI-generated collages
   - Key moments selection
   - Context enhancement
8. Implement Memory Vault:
   - Encrypted storage
   - Content organization
   - Export options

## Current Challenges
1. Integrating map functionality
2. Handling location permissions and accuracy
3. Implementing OAuth providers
4. Managing Circle discovery with geofencing
5. Optimizing camera performance
6. Implementing video recording
7. Ensuring proper auto-destruction of content
8. ARCore/Sceneform library compatibility issues
9. RAG integration complexity
10. Real-time collaborative features implementation
11. Screenshot prevention edge cases
12. OpenAI API integration reliability

## Technical Decisions
1. Using Jetpack Compose for UI
2. Firebase Auth for authentication
3. Firestore for user and Circle data
4. Firebase Storage for media files
5. CameraX for camera implementation
6. Coil for image loading
7. ARCore and Sceneform for AR filters
8. OpenStreetMap for map implementation
9. Geofencing for location-based Circles
10. RAG API integration with OpenAI
11. Material 3 for consistent design system
12. OAuth providers for authentication
13. Google Play Services for location updates
14. Material 3 bottom sheets for circle information
15. FLAG_SECURE for screenshot prevention

## Core Workflows
1. Map Rendering and Circle Display:
   - Fetch user's location via device GPS
   - Query OpenStreetMap API for college town map (1 km radius)
   - Retrieve active/upcoming Circles from database
   - Calculate Circle pin sizes based on participant count
   - Render map with pins, update every 5 seconds
   - Display circle details in bottom sheet when selected
   - Toggle between normal and fullscreen map modes
2. Circle Creation and Management:
   - Store Circle details in database
   - Validate location within 1 km radius using geofencing
   - Generate unique invite code for private Circles
   - Update Circle size on map as users join
   - Schedule auto-deletion post-expiration
   - Require location permissions for circle creation
3. Snap Processing:
   - Validate format/size (≤30 secs video, ≤280 chars text)
   - Apply selected AR filter
   - Store snap temporarily, tagged to Circle
   - Push snap to story feed for Circle members
   - Delete snap when Circle expires
4. RAG-Powered Suggestions:
   - Query public APIs for trending events/topics
   - Generate captions, hashtags, or filter suggestions
   - Cache results for 1 hour to reduce API calls
   - Deliver suggestions in <2 seconds
5. Screenshot Prevention:
   - Apply FLAG_SECURE to sensitive screens
   - Hide content in recent apps
   - Block screen recording
   - Prevent secure display casting

## Testing Status
1. Basic unit tests needed for auth flow
2. UI tests to be set up
3. Integration tests pending
4. Map functionality tests needed
5. Location services tests needed
6. Camera functionality tests needed
7. AR functionality tests needed
8. Circle functionality tests needed
9. RAG integration tests needed
10. Screenshot prevention tests needed

## Documentation Needs
1. Map integration documentation
2. OAuth setup guide
3. College town selection implementation
4. Circle creation with location guide
5. RAG integration documentation
6. UI component documentation
7. Testing strategy documentation
8. Material 3 theme customization guide
9. Screenshot prevention implementation guide
10. OpenAI integration guide

## Current Branch Structure
```