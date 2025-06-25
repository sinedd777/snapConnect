package com.example.myapplication.ui.auth

import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseAuth

/**
 * Authentication screen:
 * •  Email / password **sign-in**
 * •  Email / password **sign-up**
 *
 * Google Sign-In has been removed.
 */
@Composable
fun AuthScreen() {
    /* ------------------------------------------------------------------ */
    /* Helpers & state                                                    */
    /* ------------------------------------------------------------------ */
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val auth    = remember { FirebaseAuth.getInstance() }

    var mode by remember { mutableStateOf(AuthMode.SIGN_IN) }

    /* ------------------------------------------------------------------ */
    /* UI                                                                  */
    /* ------------------------------------------------------------------ */
    when (mode) {
        AuthMode.SIGN_IN -> {
            SignInScreen(
                onSignInClicked = { email, password ->
                    if (email.isNotBlank() && password.isNotBlank()) {
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                val msg = if (task.isSuccessful)
                                    "Sign-in successful"
                                else
                                    task.exception?.localizedMessage ?: "Sign-in failed"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                    }
                },
                onNavigateToSignUp = { mode = AuthMode.SIGN_UP },
                /* Delete this parameter in SignInScreen if it’s no longer needed */
                onGoogleSignInClicked = null         // or {} if the parameter is non-nullable
            )
        }

        AuthMode.SIGN_UP -> {
            SignUpScreen(
                onSignUpClicked = { email, password, confirm ->
                    if (email.isNotBlank() && password == confirm) {
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                val msg = if (task.isSuccessful) {
                                    mode = AuthMode.SIGN_IN
                                    "Sign-up successful"
                                } else {
                                    task.exception?.localizedMessage ?: "Sign-up failed"
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(context,
                            "Passwords don’t match or fields empty",
                            Toast.LENGTH_SHORT).show()
                    }
                },
                onNavigateToSignIn = { mode = AuthMode.SIGN_IN }
            )
        }
    }
}

/* ---------------------------------------------------------------------- */
private enum class AuthMode { SIGN_IN, SIGN_UP }