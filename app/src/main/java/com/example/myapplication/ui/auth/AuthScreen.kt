package com.example.myapplication.ui.auth

import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/** Composable authentication entry-point. */
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit = {}          // called after a successful login
) {
    val context = LocalContext.current
    val auth    = remember { FirebaseAuth.getInstance() }

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
                onSignUpClicked = { email, password, confirm ->
                    if (email.isNotBlank() && password == confirm) {
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(context, "Sign-up successful", Toast.LENGTH_SHORT).show()
                                    mode = AuthMode.SIGN_IN
                                } else {
                                    Toast.makeText(
                                        context,
                                        task.exception?.localizedMessage ?: "Sign-up failed",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    } else {
                        Toast.makeText(context, "Passwords don’t match or fields empty", Toast.LENGTH_SHORT).show()
                    }
                },
                onNavigateToSignIn = { mode = AuthMode.SIGN_IN }
            )
        }
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
                "email"     to (user.email ?: ""),
                "createdAt" to Timestamp.now()
            )
            docRef.set(data).addOnCompleteListener { onComplete() }
        } else {
            onComplete()
        }
    }.addOnFailureListener { onComplete() }
}

/** Local screen mode. */
private enum class AuthMode { SIGN_IN, SIGN_UP }