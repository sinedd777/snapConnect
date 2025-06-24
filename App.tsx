import React, { useEffect, useState } from 'react';
import { SafeAreaView, StyleSheet } from 'react-native';
import { PhoneAuthScreen } from './src/screens/auth/PhoneAuthScreen';
import { onAuthStateChanged } from './src/services/firebase/auth';
import { FirebaseAuthTypes } from '@react-native-firebase/auth';

export default function App() {
  const [user, setUser] = useState<FirebaseAuthTypes.User | null>(null);
  const [initializing, setInitializing] = useState(true);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged((user) => {
      setUser(user);
      if (initializing) {
        setInitializing(false);
      }
    });

    // Cleanup subscription
    return unsubscribe;
  }, [initializing]);

  if (initializing) {
    return null; // Or a loading screen
  }

  return (
    <SafeAreaView style={styles.container}>
      {!user ? <PhoneAuthScreen /> : null /* Add your main app screens here */}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
});
