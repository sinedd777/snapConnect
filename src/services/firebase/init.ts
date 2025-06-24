import firebase from '@react-native-firebase/app';
import auth from '@react-native-firebase/auth';
import firestore from '@react-native-firebase/firestore';
import storage from '@react-native-firebase/storage';
import functions from '@react-native-firebase/functions';
import { firebaseConfig } from './config';

/**
 * Initialize Firebase if it hasn't been initialized yet
 */
export function initializeFirebase() {
  try {
    if (!firebase.apps.length) {
      firebase.initializeApp(firebaseConfig);
    }
    return firebase.app();
  } catch (error) {
    console.error('Error initializing Firebase:', error);
    throw error;
  }
}

// Initialize Firebase
const app = initializeFirebase();

// Initialize services
const db = firestore();
const storageInstance = storage();
const functionsInstance = functions();
const authInstance = auth();

// Export initialized instances
export { 
  app,
  authInstance as auth,
  db as firestore,
  storageInstance as storage,
  functionsInstance as functions
}; 