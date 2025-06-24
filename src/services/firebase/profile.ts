import firestore from '@react-native-firebase/firestore';
import storage from '@react-native-firebase/storage';
import { UserProfile, UserProfileUpdate, ProfilePhotoUpload } from '../../types/user';

const USERS_COLLECTION = 'users';
const PROFILE_PHOTOS_PATH = 'profile_photos';

export const profileService = {
  /**
   * Create a new user profile in Firestore
   */
  async createProfile(uid: string, data: Partial<UserProfile>): Promise<void> {
    const profile: UserProfile = {
      uid,
      displayName: null,
      email: null,
      phoneNumber: null,
      photoURL: null,
      bio: null,
      createdAt: new Date(),
      updatedAt: new Date(),
      ...data,
    };

    await firestore().collection(USERS_COLLECTION).doc(uid).set(profile);
  },

  /**
   * Get a user profile from Firestore
   */
  async getProfile(uid: string): Promise<UserProfile | null> {
    const doc = await firestore().collection(USERS_COLLECTION).doc(uid).get();
    if (!doc.exists) return null;
    const data = doc.data();
    return data ? { ...data, createdAt: data.createdAt.toDate(), updatedAt: data.updatedAt.toDate() } as UserProfile : null;
  },

  /**
   * Update a user profile in Firestore
   */
  async updateProfile(uid: string, data: UserProfileUpdate): Promise<void> {
    const update = {
      ...data,
      updatedAt: new Date(),
    };

    await firestore().collection(USERS_COLLECTION).doc(uid).update(update);
  },

  /**
   * Upload a profile photo to Firebase Storage
   */
  async uploadProfilePhoto(uid: string, photo: ProfilePhotoUpload): Promise<string> {
    const fileExtension = photo.name.split('.').pop();
    const storageRef = storage().ref(`${PROFILE_PHOTOS_PATH}/${uid}.${fileExtension}`);
    
    // Upload the file
    await storageRef.putFile(photo.uri);
    
    // Get the download URL
    const downloadURL = await storageRef.getDownloadURL();
    
    // Update the user profile with the new photo URL
    await this.updateProfile(uid, { photoURL: downloadURL });
    
    return downloadURL;
  },

  /**
   * Delete a profile photo from Firebase Storage
   */
  async deleteProfilePhoto(uid: string): Promise<void> {
    const photoRef = storage().ref(`${PROFILE_PHOTOS_PATH}/${uid}`);
    try {
      await photoRef.delete();
      await this.updateProfile(uid, { photoURL: undefined });
    } catch (error: unknown) {
      // If the file doesn't exist, just update the profile
      if (error instanceof Error && error.message.includes('storage/object-not-found')) {
        await this.updateProfile(uid, { photoURL: undefined });
      } else {
        throw error;
      }
    }
  },

  /**
   * Listen to real-time profile updates
   */
  subscribeToProfile(uid: string, callback: (profile: UserProfile) => void): () => void {
    return firestore()
      .collection(USERS_COLLECTION)
      .doc(uid)
      .onSnapshot((doc) => {
        if (!doc.exists) return;
        const data = doc.data();
        if (data) {
          callback({
            ...data,
            createdAt: data.createdAt.toDate(),
            updatedAt: data.updatedAt.toDate()
          } as UserProfile);
        }
      });
  },
}; 