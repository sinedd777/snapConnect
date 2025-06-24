import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { AuthProvider, useAuth } from './src/context/AuthContext';
import { UserProfileProvider } from './src/context/UserProfileContext';
import { SignInScreen } from './src/screens/auth/SignInScreen';
import { SignUpScreen } from './src/screens/auth/SignUpScreen';
import { ProfileScreen } from './src/screens/profile/ProfileScreen';
import { View, Text, ActivityIndicator, TouchableOpacity } from 'react-native';
import { RootStackParamList } from './src/navigation/types';
import { useNavigation } from '@react-navigation/native';

const Stack = createNativeStackNavigator<RootStackParamList>();

function MainScreen() {
  const { signOut } = useAuth();
  const navigation = useNavigation();

  return (
    <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
      <Text>Welcome to SnapConnect!</Text>
      <TouchableOpacity 
        onPress={() => navigation.navigate('Profile')}
        style={{ marginTop: 10 }}
      >
        <Text style={{ color: 'blue' }}>View Profile</Text>
      </TouchableOpacity>
      <TouchableOpacity 
        onPress={signOut}
        style={{ marginTop: 10 }}
      >
        <Text style={{ color: 'blue' }}>Sign Out</Text>
      </TouchableOpacity>
    </View>
  );
}

function Navigation() {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  return (
    <Stack.Navigator>
      {user ? (
        <>
          <Stack.Screen name="Main" component={MainScreen} />
          <Stack.Screen 
            name="Profile" 
            component={ProfileScreen}
            options={{ 
              title: 'My Profile',
              headerBackTitle: 'Back'
            }}
          />
        </>
      ) : (
        <>
          <Stack.Screen 
            name="SignIn" 
            component={SignInScreen} 
            options={{ headerShown: false }}
          />
          <Stack.Screen 
            name="SignUp" 
            component={SignUpScreen}
            options={{ headerShown: false }}
          />
        </>
      )}
    </Stack.Navigator>
  );
}

export default function App() {
  return (
    <NavigationContainer>
      <AuthProvider>
        <UserProfileProvider>
          <Navigation />
        </UserProfileProvider>
      </AuthProvider>
    </NavigationContainer>
  );
}
