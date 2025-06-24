# Technical Context

## Development Environment
- OS: macOS (darwin 23.6.0)
- Shell: /bin/zsh
- Android SDK Path: /Users/sinedd/Library/Android/sdk

## Project Setup
1. React Native with Expo
2. Firebase Integration
3. TypeScript for type safety
4. Development using Expo Dev Client

## Key Dependencies
- expo-dev-client: Required for native code integration
- @react-native-firebase/app: Core Firebase functionality
- expo-web-browser: Web browser integration
- expo-system-ui: System UI integration
- react-native-safe-area-context: Safe area handling

## Firebase Configuration
- Firebase config is stored in `src/services/firebase/config.ts`
- Android configuration in `android/app/google-services.json`
- Package name: com.sinedd.snapconnect

## Build Configuration
- Metro bundler configured for Firebase SDK compatibility
- Android build setup with Gradle
- iOS build setup with CocoaPods

## Known Technical Constraints
1. Firebase Auth requires proper initialization
2. Metro bundler needs special configuration for Firebase v9.7.x+
3. Native code changes require `expo prebuild --clean`

## Development Commands
- Start Metro bundler: `npm start` or `yarn start`
- Run on Android: `npx expo run:android`
- Run on iOS: `npx expo run:ios`

## Core Dependencies

### Frontend
```json
{
  "dependencies": {
    "expo": "^49.0.0",
    "expo-camera": "^13.0.0",
    "expo-notifications": "^0.20.0",
    "react-native": "0.72.0",
    "react-navigation": "^6.0.0",
    "@react-native-firebase/app": "^18.0.0",
    "@react-native-firebase/auth": "^18.0.0",
    "@react-native-firebase/firestore": "^18.0.0",
    "@react-native-firebase/storage": "^18.0.0",
    "@react-native-firebase/messaging": "^18.0.0"
  }
}
```

### Backend (Firebase)
- Authentication
- Cloud Firestore
- Cloud Storage
- Cloud Functions
- Cloud Messaging

## API Structure

### Authentication
```typescript
// Phone auth flow
signInWithPhoneNumber(phone: string): Promise<void>
verifyCode(code: string): Promise<UserCredential>
updateProfile(data: UserProfile): Promise<void>

// Session management
refreshToken(): Promise<string>
signOut(): Promise<void>
```

### Media Operations
```typescript
// Camera operations
capturePhoto(): Promise<Asset>
captureVideo(): Promise<Asset>
applyFilter(asset: Asset, filter: Filter): Promise<Asset>

// Upload operations
uploadMedia(asset: Asset): Promise<string>
trackProgress(uploadTask: Task): Observable<Progress>
```

### Data Operations
```typescript
// Friend management
sendFriendRequest(userId: string): Promise<void>
acceptFriendRequest(requestId: string): Promise<void>
getFriends(): Promise<Friend[]>

// Snap operations
sendSnap(data: SnapData): Promise<string>
getInbox(): Observable<Snap[]>
markViewed(snapId: string): Promise<void>
```

## Technical Constraints

### Client-side
1. Device Compatibility
   - iOS 13+
   - Android API 21+
   - Memory constraints for media processing

2. Network
   - Handle offline scenarios
   - Optimize for mobile networks
   - Background upload support

3. Storage
   - Local cache management
   - Media compression
   - Temp file cleanup

### Server-side
1. Firebase Limits
   - Document size (1MB max)
   - Collection queries
   - Concurrent connections
   - Storage quotas

2. Functions
   - Execution timeout (540s)
   - Memory limits (256MB-2GB)
   - Concurrent executions
   - Cold start latency

3. Security
   - Rate limiting
   - Data validation
   - Access control
   - Token management

## Performance Targets

### Client Metrics
- App launch < 2s
- Camera load < 1s
- Photo capture < 100ms
- Upload start < 500ms
- UI response < 16ms

### Server Metrics
- Function execution < 10s
- Query response < 500ms
- Push delivery < 1s
- Storage ops < 5s

## Development Workflow

### Version Control
- Feature branches
- PR reviews
- Semantic versioning
- Changelog maintenance

### Testing
- Unit tests (Jest)
- Integration tests
- E2E tests (Detox)
- Manual QA

### Deployment
- Staging environment
- Production environment
- Rollback procedures
- Monitoring setup

## Error Handling

### Client-side
- Network errors
- Media capture errors
- Upload failures
- Auth errors

### Server-side
- Function timeouts
- Storage errors
- Security rule violations
- Rate limit exceeded

## Monitoring

### Analytics
- User engagement
- Feature usage
- Error rates
- Performance metrics

### Logging
- Debug logs
- Error tracking
- Crash reporting
- Usage analytics

## Security Measures

### Data Protection
- End-to-end encryption
- Secure storage
- Access control
- Input validation

### Authentication
- Phone verification
- Token management
- Session control
- Device tracking

### Content Safety
- Media scanning
- User reporting
- Content moderation
- Abuse prevention

## Authentication Implementation
### Firebase Configuration
- Firebase project: `snapconnect-app`
- Android package name: `com.sinedd.snapconnect`
- Debug SHA-1: `5E:8F:16:06:2E:A3:CD:2C:4A:0D:54:78:76:BA:A6:F3:8C:AB:F6:25`

### Dependencies
```json
{
  "@react-native-firebase/app": "^22.2.1",
  "@react-native-firebase/auth": "^22.2.1",
  "@react-native-google-signin/google-signin": "^11.0.0"
}
```

### Key Files
1. `src/services/firebase/config.ts` - Firebase configuration
2. `src/services/firebase/auth.ts` - Authentication service
3. `src/context/AuthContext.tsx` - Authentication context provider
4. `android/app/google-services.json` - Firebase Android configuration

## Build Configuration
### Android
- Using Gradle version 8.1.0
- Minimum SDK: 24
- Target SDK: 35
- Build Tools: 35.0.0
- NDK Version: 27.1.12297006

### Environment Setup
- React Native CLI
- Android SDK
- Firebase CLI
- Google Cloud Console configuration

## Development Notes
1. The `google-services.json` file must be preserved during builds
2. Android builds require proper Google Sign-In configuration
3. Firebase initialization must happen before any auth operations
4. Google Sign-In requires proper SHA-1 fingerprint in Firebase Console

## Known Working Configurations
1. Email/Password Authentication:
   - Uses Firebase Auth directly
   - No additional configuration needed

2. Google Sign-In:
   - Requires `google-services.json`
   - Requires SHA-1 fingerprint in Firebase Console
   - Uses `@react-native-google-signin/google-signin`

## Build Process
1. Preserve `google-services.json`
2. Run `./gradlew clean`
3. Run `./gradlew assembleDebug`
4. Use `npx expo run:android` for development 