import React, { createContext, useContext, useEffect, useState } from 'react';
import { UserProfile, UserProfileUpdate, ProfilePhotoUpload } from '../types/user';
import { profileService } from '../services/firebase/profile';
import { useAuth } from './AuthContext';

interface UserProfileContextType {
  profile: UserProfile | null;
  loading: boolean;
  error: Error | null;
  updateProfile: (data: UserProfileUpdate) => Promise<void>;
  uploadProfilePhoto: (photo: ProfilePhotoUpload) => Promise<string>;
  deleteProfilePhoto: () => Promise<void>;
}

const UserProfileContext = createContext<UserProfileContextType | undefined>(undefined);

export function UserProfileProvider({ children }: { children: React.ReactNode }) {
  const { user } = useAuth();
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    let unsubscribe: (() => void) | undefined;

    const initializeProfile = async () => {
      if (!user) {
        setProfile(null);
        setLoading(false);
        return;
      }

      try {
        // Check if profile exists
        const existingProfile = await profileService.getProfile(user.uid);
        
        if (!existingProfile) {
          // Create new profile if it doesn't exist
          await profileService.createProfile(user.uid, {
            email: user.email,
            displayName: user.displayName,
            phoneNumber: user.phoneNumber,
            photoURL: user.photoURL,
          });
        }

        // Subscribe to profile changes
        unsubscribe = profileService.subscribeToProfile(user.uid, (updatedProfile) => {
          setProfile(updatedProfile);
          setLoading(false);
        });
      } catch (err) {
        setError(err instanceof Error ? err : new Error('Failed to initialize profile'));
        setLoading(false);
      }
    };

    initializeProfile();

    return () => {
      if (unsubscribe) {
        unsubscribe();
      }
    };
  }, [user]);

  const updateProfile = async (data: UserProfileUpdate) => {
    if (!user) throw new Error('No authenticated user');
    setError(null);
    
    try {
      await profileService.updateProfile(user.uid, data);
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to update profile');
      setError(error);
      throw error;
    }
  };

  const uploadProfilePhoto = async (photo: ProfilePhotoUpload) => {
    if (!user) throw new Error('No authenticated user');
    setError(null);
    
    try {
      return await profileService.uploadProfilePhoto(user.uid, photo);
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to upload profile photo');
      setError(error);
      throw error;
    }
  };

  const deleteProfilePhoto = async () => {
    if (!user) throw new Error('No authenticated user');
    setError(null);
    
    try {
      await profileService.deleteProfilePhoto(user.uid);
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to delete profile photo');
      setError(error);
      throw error;
    }
  };

  return (
    <UserProfileContext.Provider
      value={{
        profile,
        loading,
        error,
        updateProfile,
        uploadProfilePhoto,
        deleteProfilePhoto,
      }}
    >
      {children}
    </UserProfileContext.Provider>
  );
}

export function useUserProfile() {
  const context = useContext(UserProfileContext);
  if (context === undefined) {
    throw new Error('useUserProfile must be used within a UserProfileProvider');
  }
  return context;
} 