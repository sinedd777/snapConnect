import firebase from '@react-native-firebase/app';

/**
 * Firebase configuration object
 * These values are available in your Firebase Console -> Project Settings -> General
 * For security, these should be stored in environment variables in production
 */
export const firebaseConfig = {
  // Your web app's Firebase configuration
  // For Firebase JS SDK v7.20.0 and later, measurementId is optional
  apiKey: "AIzaSyBlgNFf34qoz4c9oadLWMT3qu2hANpUEzQ",
  projectId: "snapconnect-app",
  storageBucket: "snapconnect-app.firebasestorage.app",
  messagingSenderId: "272057868675",
  appId: "1:272057868675:android:e91c836c0c66e8dc37d6b6",
  databaseURL: `https://${process.env.FIREBASE_PROJECT_ID}.firebaseio.com`
} as const;

/**
 * Storage bucket URL for Firebase Storage
 * Used for constructing full URLs to stored files
 */
export const storageBucketUrl = `gs://${firebaseConfig.storageBucket}`;

/**
 * Collection names in Firestore
 * Centralized here to avoid typos and make updates easier
 */
export const collections = {
  users: 'users',
  snaps: 'snaps',
  friends: 'friends',
  notifications: 'notifications',
} as const;

/**
 * Storage paths in Firebase Storage
 * Centralized here to avoid typos and make updates easier
 */
export const storagePaths = {
  snaps: 'snaps',
  profiles: 'profiles',
  media: 'media',
} as const; 