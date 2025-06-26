package com.example.myapplication.ui.auth

import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

private const val TAG = "AuthScreen"

/** Composable authentication entry-point. */
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit = {}          // called after a successful login
) {
    val context = LocalContext.current
    val auth    = remember { FirebaseAuth.getInstance() }
    val firestore = remember { Firebase.firestore }

    var mode by remember { mutableStateOf(AuthMode.SIGN_IN) }

    // ───────────────────────── UI ────────────────────────── //
    when (mode) {
        /* ------------------- SIGN-IN ------------------- */
        AuthMode.SIGN_IN -> {
            SignInScreen(
                onSignInClicked = { email, password ->
                    if (email.isNotBlank() && password.isNotBlank()) {
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    ensureProfileExists(auth) { onAuthSuccess() }
                                    Toast.makeText(context, "Sign-in successful", Toast.LENGTH_SHORT).show()
                                } else {
                                    Log.e(TAG, "Sign-in failed", task.exception)
                                    Toast.makeText(
                                        context,
                                        task.exception?.localizedMessage ?: "Sign-in failed",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    }
                },
                onNavigateToSignUp   = { mode = AuthMode.SIGN_UP },
                onGoogleSignInClicked = null          // remove button in SignInScreen if unused
            )
        }

        /* ------------------- SIGN-UP ------------------- */
        AuthMode.SIGN_UP -> {
            SignUpScreen(
                onSignUpClicked = { email, username, password, confirm ->
                    if (email.isNotBlank() && username.isNotBlank() && password == confirm) {
                        // First check username availability one more time before creating the account
                        checkUsernameAvailability(username) { isAvailable, _ ->
                            if (isAvailable) {
                                // Create the Firebase Auth user
                                auth.createUserWithEmailAndPassword(email, password)
                                    .addOnSuccessListener { authResult ->
                                        Log.d(TAG, "Firebase Auth user created successfully")
                                        // Then create the user document in Firestore
                                        val user = authResult.user
                                        if (user != null) {
                                            val userData = mapOf(
                                                "uid" to user.uid,
                                                "id" to user.uid,  // Add this to match User model
                                                "email" to email,
                                                "username" to username,
                                                "createdAt" to Timestamp.now()
                                            )
                                            
                                            firestore.collection("users").document(user.uid)
                                                .set(userData)
                                                .addOnSuccessListener {
                                                    Log.d(TAG, "User profile created successfully")
                                                    Toast.makeText(context, "Sign-up successful", Toast.LENGTH_SHORT).show()
                                                    mode = AuthMode.SIGN_IN
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.e(TAG, "Error creating user profile", e)
                                                    // Even if profile creation fails, the auth user is created
                                                    // We'll show a warning but still allow sign-in
                                                    Toast.makeText(
                                                        context,
                                                        "Account created but profile setup failed. Some features may be limited.",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                    mode = AuthMode.SIGN_IN
                                                }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e(TAG, "Error creating user", e)
                                        Toast.makeText(
                                            context,
                                            e.localizedMessage ?: "Sign-up failed",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            } else {
                                Toast.makeText(context, "Username is already taken. Please choose another.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "Please fill all fields correctly", Toast.LENGTH_SHORT).show()
                    }
                },
                onNavigateToSignIn = { mode = AuthMode.SIGN_IN },
                onCheckUsername = { username, callback ->
                    checkUsernameAvailability(username, callback)
                }
            )
        }
    }
}

// Helper function to check username availability - moved outside the composable
private fun checkUsernameAvailability(username: String, callback: (Boolean, String?) -> Unit) {
    try {
        Log.d(TAG, "Checking username availability: $username")
        Firebase.firestore.collection("users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "Username query result: ${documents.size()} documents")
                if (documents.isEmpty) {
                    callback(true, null)
                } else {
                    callback(false, "Username already taken")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking username", e)
                // If there's an error, assume username is not available to be safe
                callback(false, "Error checking username availability")
            }
    } catch (e: Exception) {
        Log.e(TAG, "Exception checking username", e)
        callback(false, "Error checking username availability")
    }
}

/* ───────────────────────── Helpers ───────────────────────── */

/** Ensures a Firestore document exists for the logged-in user before invoking [onComplete]. */
private fun ensureProfileExists(
    auth: FirebaseAuth,
    onComplete: () -> Unit
) {
    val user = auth.currentUser ?: return onComplete()

    val db      = Firebase.firestore
    val docRef  = db.collection("users").document(user.uid)

    docRef.get().addOnSuccessListener { snapshot ->
        if (!snapshot.exists()) {
            val data = mapOf(
                "uid"       to user.uid,
                "id"        to user.uid,  // Add this to match User model
                "email"     to (user.email ?: ""),
                "createdAt" to Timestamp.now()
            )
            docRef.set(data)
                .addOnSuccessListener {
                    Log.d(TAG, "User profile created during sign-in")
                    onComplete()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error creating user profile during sign-in", e)
                    onComplete()
                }
        } else {
            onComplete()
        }
    }.addOnFailureListener { e ->
        Log.e(TAG, "Error checking if user profile exists", e)
        onComplete()
    }
}

/** Local screen mode. */
private enum class AuthMode { SIGN_IN, SIGN_UP }