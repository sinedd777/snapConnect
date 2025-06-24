export interface UserProfile {
  uid: string;
  displayName: string | null;
  email: string | null;
  phoneNumber: string | null;
  photoURL: string | null;
  bio: string | null;
  createdAt: Date;
  updatedAt: Date;
}

export interface UserProfileUpdate {
  displayName?: string;
  email?: string;
  phoneNumber?: string;
  photoURL?: string;
  bio?: string;
}

export type ProfilePhotoUpload = {
  uri: string;
  type: string;
  name: string;
} 