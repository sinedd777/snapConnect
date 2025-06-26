package com.example.myapplication.ui.auth

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "SignUpScreen"

@Composable
fun SignUpScreen(
    onSignUpClicked: (email: String, username: String, password: String, confirm: String) -> Unit,
    onNavigateToSignIn: () -> Unit,
    onCheckUsername: (username: String, callback: (Boolean, String?) -> Unit) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    var usernameError by remember { mutableStateOf<String?>(null) }
    var isUsernameAvailable by remember { mutableStateOf(false) }
    var isCheckingUsername by remember { mutableStateOf(false) }
    
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    
    // Username validation regex - only allow letters, numbers, and underscores, 3-15 characters
    val usernameRegex = remember { Regex("^[a-zA-Z0-9_]{3,15}$") }
    
    // Debounce username checks
    LaunchedEffect(username) {
        // Reset state when username changes
        if (username.isEmpty()) {
            isUsernameAvailable = false
            usernameError = null
            isCheckingUsername = false
            return@LaunchedEffect
        }
        
        // Validate format first
        if (!username.matches(usernameRegex)) {
            isUsernameAvailable = false
            usernameError = "Username must be 3-15 characters with only letters, numbers, and underscores"
            isCheckingUsername = false
            return@LaunchedEffect
        }
        
        // If format is valid, check availability
        isCheckingUsername = true
        delay(500) // Debounce for 500ms
        
        onCheckUsername(username) { available, error ->
            isUsernameAvailable = available
            usernameError = error
            isCheckingUsername = false
        }
    }
    
    // Helper function for form validation
    fun isFormValid(): Boolean {
        return email.isNotBlank() && 
               username.isNotBlank() && 
               password.isNotBlank() && 
               password == confirmPassword &&
               isUsernameAvailable &&
               !isCheckingUsername
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Create Account",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )
        
        OutlinedTextField(
            value = username,
            onValueChange = { username = it.trim() }, // Trim whitespace from username
            label = { Text("Username") },
            singleLine = true,
            isError = !isUsernameAvailable && username.isNotEmpty(),
            supportingText = {
                if (isCheckingUsername) {
                    Text("Checking availability...")
                } else if (usernameError != null && username.isNotEmpty()) {
                    Text(usernameError!!, color = MaterialTheme.colorScheme.error)
                } else if (isUsernameAvailable && username.isNotEmpty()) {
                    Text("Username available", color = MaterialTheme.colorScheme.primary)
                }
            },
            trailingIcon = {
                if (username.isNotEmpty() && !isCheckingUsername) {
                    if (isUsernameAvailable) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Username available",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Username not available",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )
        
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )
        
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            singleLine = true,
            isError = password != confirmPassword && confirmPassword.isNotEmpty(),
            supportingText = {
                if (password != confirmPassword && confirmPassword.isNotEmpty()) {
                    Text("Passwords don't match", color = MaterialTheme.colorScheme.error)
                }
            },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { 
                    focusManager.clearFocus()
                    if (isFormValid()) {
                        onSignUpClicked(email, username, password, confirmPassword)
                    }
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )
        
        Button(
            onClick = { 
                onSignUpClicked(email, username, password, confirmPassword)
            },
            enabled = isFormValid(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Text("Sign Up")
        }
        
        TextButton(
            onClick = onNavigateToSignIn,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text("Already have an account? Sign In")
        }
    }
} 