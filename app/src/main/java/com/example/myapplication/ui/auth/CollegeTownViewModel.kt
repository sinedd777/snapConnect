package com.example.myapplication.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repositories.MapRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CollegeTownViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val mapRepository = MapRepository()
    
    // UI state
    var isLoading by mutableStateOf(false)
        private set
        
    var errorMessage by mutableStateOf<String?>(null)
        private set
        
    var collegeTowns by mutableStateOf<List<String>>(emptyList())
        private set
        
    init {
        loadCollegeTowns()
    }
    
    /**
     * Load the list of college towns
     */
    private fun loadCollegeTowns() {
        viewModelScope.launch {
            isLoading = true
            
            try {
                val result = mapRepository.getCollegeTowns()
                if (result.isSuccess) {
                    collegeTowns = result.getOrNull() ?: emptyList()
                } else {
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to load college towns"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unknown error occurred"
            } finally {
                isLoading = false
            }
        }
    }
    
    /**
     * Save the selected college town to the user's profile
     */
    fun saveCollegeTown(collegeTown: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            
            try {
                val userId = auth.currentUser?.uid
                    ?: throw IllegalStateException("User not authenticated")
                    
                // Update the user document with the college town
                firestore.collection("users")
                    .document(userId)
                    .update(mapOf(
                        "collegeTown" to collegeTown,
                        "lastUpdated" to com.google.firebase.Timestamp.now()
                    ))
                    .await()
                    
                onSuccess()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to save college town"
            } finally {
                isLoading = false
            }
        }
    }
    
    /**
     * Get the user's current college town if available
     */
    fun getCurrentCollegeTown(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                
                val userDoc = firestore.collection("users")
                    .document(userId)
                    .get()
                    .await()
                    
                if (userDoc.exists()) {
                    val collegeTown = userDoc.getString("collegeTown")
                    onResult(collegeTown)
                } else {
                    onResult(null)
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to get college town"
                onResult(null)
            }
        }
    }
    
    /**
     * Detect college town from user's location
     */
    fun detectCollegeTown(lat: Double, lng: Double, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            
            try {
                val result = mapRepository.verifyCollegeTownLocation(lat, lng)
                if (result.isSuccess) {
                    onResult(result.getOrNull())
                } else {
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to detect college town"
                    onResult(null)
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unknown error occurred"
                onResult(null)
            } finally {
                isLoading = false
            }
        }
    }
    
    /**
     * Clear any error message
     */
    fun clearError() {
        errorMessage = null
    }
} 