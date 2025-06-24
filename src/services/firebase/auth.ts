import auth from '@react-native-firebase/auth';
import { FirebaseAuthTypes } from '@react-native-firebase/auth';

/**
 * Sign in with phone number
 * @param phoneNumber - The phone number to sign in with (format: +1234567890)
 * @returns Promise with the confirmation result
 */
export const signInWithPhone = async (phoneNumber: string): Promise<FirebaseAuthTypes.ConfirmationResult> => {
  try {
    const confirmation = await auth().signInWithPhoneNumber(phoneNumber);
    return confirmation;
  } catch (error) {
    console.error('Error signing in with phone number:', error);
    throw error;
  }
};

/**
 * Verify phone number with code
 * @param confirmation - The confirmation result from signInWithPhone
 * @param code - The verification code received via SMS
 * @returns Promise with the user credential
 */
export const verifyCode = async (
  confirmation: FirebaseAuthTypes.ConfirmationResult,
  code: string
): Promise<FirebaseAuthTypes.UserCredential> => {
  try {
    const credential = await confirmation.confirm(code);
    if (!credential) {
      throw new Error('Failed to verify code: No credential returned');
    }
    return credential;
  } catch (error) {
    console.error('Error verifying code:', error);
    throw error;
  }
};

/**
 * Sign out the current user
 */
export const signOut = async (): Promise<void> => {
  try {
    await auth().signOut();
  } catch (error) {
    console.error('Error signing out:', error);
    throw error;
  }
};

/**
 * Get the current user
 * @returns The current user or null if not signed in
 */
export const getCurrentUser = (): FirebaseAuthTypes.User | null => {
  return auth().currentUser;
};

/**
 * Subscribe to auth state changes
 * @param callback - Function to call when auth state changes
 * @returns Unsubscribe function
 */
export const onAuthStateChanged = (
  callback: (user: FirebaseAuthTypes.User | null) => void
): (() => void) => {
  return auth().onAuthStateChanged(callback);
};

/**
 * Check if a user is currently signed in
 * @returns True if a user is signed in, false otherwise
 */
export const isSignedIn = (): boolean => {
  return auth().currentUser !== null;
}; 