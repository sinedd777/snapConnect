import firebase from '@react-native-firebase/app';
import {
  FIREBASE_API_KEY,
  FIREBASE_PROJECT_ID,
  FIREBASE_STORAGE_BUCKET,
  FIREBASE_MESSAGING_SENDER_ID,
  FIREBASE_APP_ID,
} from '@env';

/**
 * Firebase configuration object
 * These values are loaded from environment variables for security
 */
export const firebaseConfig = {
  // Your web app's Firebase configuration
  // For Firebase JS SDK v7.20.0 and later, measurementId is optional
  apiKey: FIREBASE_API_KEY,
  projectId: FIREBASE_PROJECT_ID,
  storageBucket: FIREBASE_STORAGE_BUCKET,
  messagingSenderId: FIREBASE_MESSAGING_SENDER_ID,
  appId: FIREBASE_APP_ID,
  databaseURL: `https://${FIREBASE_PROJECT_ID}.firebaseio.com`
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