import React, { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  Image,
  StyleSheet,
  ActivityIndicator,
  Alert,
  Platform,
} from 'react-native';
import * as ImagePicker from 'expo-image-picker';
import { useUserProfile } from '../../context/UserProfileContext';
import { ProfilePhotoUpload } from '../../types/user';

export function ProfileScreen() {
  const { profile, loading, error, updateProfile, uploadProfilePhoto, deleteProfilePhoto } = useUserProfile();
  const [isEditing, setIsEditing] = useState(false);
  const [displayName, setDisplayName] = useState(profile?.displayName || '');
  const [bio, setBio] = useState(profile?.bio || '');
  const [isUploading, setIsUploading] = useState(false);

  const handleSave = async () => {
    try {
      await updateProfile({
        displayName: displayName || undefined,
        bio: bio || undefined,
      });
      setIsEditing(false);
    } catch (err) {
      Alert.alert('Error', 'Failed to update profile');
    }
  };

  const handlePhotoUpload = async () => {
    try {
      // Request permissions
      if (Platform.OS !== 'web') {
        const { status } = await ImagePicker.requestMediaLibraryPermissionsAsync();
        if (status !== 'granted') {
          Alert.alert('Permission needed', 'Please grant camera roll permissions to upload a photo.');
          return;
        }
      }

      // Pick the image
      const result = await ImagePicker.launchImageLibraryAsync({
        mediaTypes: ImagePicker.MediaTypeOptions.Images,
        allowsEditing: true,
        aspect: [1, 1],
        quality: 0.8,
      });

      if (!result.canceled && result.assets[0]) {
        const asset = result.assets[0];
        setIsUploading(true);

        const photo: ProfilePhotoUpload = {
          uri: asset.uri,
          type: 'image/jpeg',
          name: 'profile-photo.jpg',
        };

        await uploadProfilePhoto(photo);
      }
    } catch (err) {
      Alert.alert('Error', 'Failed to upload photo');
    } finally {
      setIsUploading(false);
    }
  };

  const handleDeletePhoto = async () => {
    try {
      await deleteProfilePhoto();
    } catch (err) {
      Alert.alert('Error', 'Failed to delete photo');
    }
  };

  if (loading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  if (error) {
    return (
      <View style={styles.errorContainer}>
        <Text style={styles.errorText}>Error: {error.message}</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <View style={styles.photoContainer}>
        <TouchableOpacity onPress={handlePhotoUpload} disabled={isUploading}>
          {isUploading ? (
            <ActivityIndicator size="large" style={styles.photoPlaceholder} />
          ) : profile?.photoURL ? (
            <Image source={{ uri: profile.photoURL }} style={styles.photo} />
          ) : (
            <View style={styles.photoPlaceholder}>
              <Text style={styles.photoPlaceholderText}>
                {profile?.displayName?.[0]?.toUpperCase() || '?'}
              </Text>
            </View>
          )}
        </TouchableOpacity>
        {profile?.photoURL && (
          <TouchableOpacity onPress={handleDeletePhoto} style={styles.deletePhotoButton}>
            <Text style={styles.deletePhotoText}>Delete Photo</Text>
          </TouchableOpacity>
        )}
      </View>

      <View style={styles.form}>
        <View style={styles.field}>
          <Text style={styles.label}>Name</Text>
          {isEditing ? (
            <TextInput
              style={styles.input}
              value={displayName}
              onChangeText={setDisplayName}
              placeholder="Enter your name"
            />
          ) : (
            <Text style={styles.value}>{profile?.displayName || 'Not set'}</Text>
          )}
        </View>

        <View style={styles.field}>
          <Text style={styles.label}>Email</Text>
          <Text style={styles.value}>{profile?.email || 'Not set'}</Text>
        </View>

        <View style={styles.field}>
          <Text style={styles.label}>Bio</Text>
          {isEditing ? (
            <TextInput
              style={[styles.input, styles.bioInput]}
              value={bio}
              onChangeText={setBio}
              placeholder="Tell us about yourself"
              multiline
              numberOfLines={4}
            />
          ) : (
            <Text style={styles.value}>{profile?.bio || 'Not set'}</Text>
          )}
        </View>

        <View style={styles.buttonContainer}>
          {isEditing ? (
            <>
              <TouchableOpacity onPress={handleSave} style={styles.button}>
                <Text style={styles.buttonText}>Save</Text>
              </TouchableOpacity>
              <TouchableOpacity
                onPress={() => {
                  setIsEditing(false);
                  setDisplayName(profile?.displayName || '');
                  setBio(profile?.bio || '');
                }}
                style={[styles.button, styles.cancelButton]}
              >
                <Text style={[styles.buttonText, styles.cancelButtonText]}>Cancel</Text>
              </TouchableOpacity>
            </>
          ) : (
            <TouchableOpacity onPress={() => setIsEditing(true)} style={styles.button}>
              <Text style={styles.buttonText}>Edit Profile</Text>
            </TouchableOpacity>
          )}
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 16,
    backgroundColor: '#fff',
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  errorContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 16,
  },
  errorText: {
    color: 'red',
    textAlign: 'center',
  },
  photoContainer: {
    alignItems: 'center',
    marginBottom: 24,
  },
  photo: {
    width: 120,
    height: 120,
    borderRadius: 60,
  },
  photoPlaceholder: {
    width: 120,
    height: 120,
    borderRadius: 60,
    backgroundColor: '#e1e1e1',
    justifyContent: 'center',
    alignItems: 'center',
  },
  photoPlaceholderText: {
    fontSize: 40,
    color: '#666',
  },
  deletePhotoButton: {
    marginTop: 8,
  },
  deletePhotoText: {
    color: 'red',
  },
  form: {
    flex: 1,
  },
  field: {
    marginBottom: 24,
  },
  label: {
    fontSize: 14,
    color: '#666',
    marginBottom: 8,
  },
  value: {
    fontSize: 16,
    color: '#000',
  },
  input: {
    borderWidth: 1,
    borderColor: '#ccc',
    borderRadius: 8,
    padding: 12,
    fontSize: 16,
  },
  bioInput: {
    height: 100,
    textAlignVertical: 'top',
  },
  buttonContainer: {
    flexDirection: 'row',
    justifyContent: 'center',
    gap: 12,
  },
  button: {
    backgroundColor: '#007AFF',
    paddingVertical: 12,
    paddingHorizontal: 24,
    borderRadius: 8,
    minWidth: 120,
    alignItems: 'center',
  },
  buttonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
  cancelButton: {
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: '#007AFF',
  },
  cancelButtonText: {
    color: '#007AFF',
  },
}); 