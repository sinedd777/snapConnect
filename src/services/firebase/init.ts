import firebase from '@react-native-firebase/app';
import auth from '@react-native-firebase/auth';
import firestore from '@react-native-firebase/firestore';
import storage from '@react-native-firebase/storage';
import functions from '@react-native-firebase/functions';
import { firebaseConfig } from './config';

// Initialize Firebase if it hasn't been initialized yet
if (!firebase.apps.length) {
  firebase.initializeApp({
    apiKey: firebaseConfig.apiKey,
    projectId: firebaseConfig.projectId,
    storageBucket: firebaseConfig.storageBucket,
    messagingSenderId: firebaseConfig.messagingSenderId,
    appId: firebaseConfig.appId,
    databaseURL: firebaseConfig.databaseURL
  });
}

// Get Firebase service instances
const app = firebase.app();
const db = firestore();
const storageInstance = storage();
const functionsInstance = functions();

export { app, auth, db as firestore, storageInstance as storage, functionsInstance as functions }; 