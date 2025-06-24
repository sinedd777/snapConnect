# SnapConnect

A mobile application that enables friends to share ephemeral photo and video content with AR effects and real-time notifications.

## Prerequisites

- Node.js 18+
- Expo CLI
- Firebase CLI
- iOS Simulator / Android Emulator
- Git

## Setup Instructions

1. Clone the repository:
```bash
git clone <repository-url>
cd snapConnect
```

2. Install dependencies:
```bash
npm install
```

3. Set up environment variables:
```bash
# Create a .env file with the following variables:
FIREBASE_API_KEY=your_api_key
FIREBASE_AUTH_DOMAIN=your_auth_domain
FIREBASE_PROJECT_ID=your_project_id
FIREBASE_STORAGE_BUCKET=your_storage_bucket
FIREBASE_MESSAGING_SENDER_ID=your_messaging_sender_id
FIREBASE_APP_ID=your_app_id

APP_ENV=development
API_URL=http://localhost:5001/your-project-id/us-central1
```

4. Start the development server:
```bash
npm start
```

5. Run on specific platform:
```bash
# For iOS
npm run ios

# For Android
npm run android
```

## Features

- Private, ephemeral photo/video sharing between friends
- Real-time notifications and engagement
- AR filters and effects for creative expression
- Automatic content expiry for privacy
- Simple friend management system

## Tech Stack

- Frontend: React Native + Expo
- Backend: Firebase Suite
  - Authentication (Phone SMS)
  - Cloud Firestore (Data)
  - Cloud Storage (Media)
  - Cloud Functions (Business Logic)
  - Cloud Messaging (Push Notifications)

## Development Workflow

1. Create a new branch for your feature
2. Make your changes
3. Submit a pull request
4. Code review
5. Merge to main branch

## Testing

```bash
# Run unit tests
npm test

# Run e2e tests
npm run test:e2e
```

## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct and the process for submitting pull requests.

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details 