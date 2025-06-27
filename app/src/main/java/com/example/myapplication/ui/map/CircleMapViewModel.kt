package com.example.myapplication.ui.map

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.models.Circle
import com.example.myapplication.data.repositories.CircleRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CircleMapViewModel : ViewModel() {
    private val circleRepository = CircleRepository()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    // Map state
    var circles by mutableStateOf<List<Circle>>(emptyList())
        private set
        
    var userLat by mutableStateOf<Double?>(null)
        private set
        
    var userLng by mutableStateOf<Double?>(null)
        private set
        
    var collegeTown by mutableStateOf<String?>(null)
        private set
        
    var mapZoom by mutableStateOf(1.0f)
        private set
        
    // Filter state
    var currentFilter by mutableStateOf("All")
        private set
        
    // UI state
    var isLoading by mutableStateOf(false)
        private set
        
    var errorMessage by mutableStateOf<String?>(null)
        private set
    
    init {
        loadUserProfile()
    }
    
    // Load user profile data
    private fun loadUserProfile() {
        viewModelScope.launch {
            try {
                auth.currentUser?.let { user ->
                    val userDoc = firestore.collection("users").document(user.uid).get().await()
                    if (userDoc.exists()) {
                        collegeTown = userDoc.getString("collegeTown")
                        
                        // Get last known location if available
                        userLat = userDoc.getDouble("lastLat")
                        userLng = userDoc.getDouble("lastLng")
                    }
                }
            } catch (e: Exception) {
                // Ignore error, location will stay null
            }
        }
    }
    
    // Load nearby circles based on user location and filter
    fun loadNearbyCircles() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            
            try {
                // For now, use hardcoded location if user location is null
                val lat = userLat ?: 37.7749
                val lng = userLng ?: -122.4194
                
                // If we have a college town, prioritize that for fetching circles
                val result = if (collegeTown != null) {
                    circleRepository.getCollegeTownCircles(collegeTown!!)
                } else {
                    circleRepository.getNearbyCircles(lat, lng)
                }
                
                if (result.isSuccess) {
                    val allCircles = result.getOrNull() ?: emptyList()
                    
                    // Apply filter
                    circles = when (currentFilter) {
                        "Public" -> allCircles.filter { !it.isPrivate }
                        "Private" -> allCircles.filter { it.isPrivate }
                        "Active" -> allCircles.filter { 
                            it.expiresAt != null && it.expiresAt.toDate().time > System.currentTimeMillis() 
                        }
                        "Upcoming" -> allCircles.filter {
                            it.startTime != null && it.startTime.toDate().time > System.currentTimeMillis()
                        }
                        "Party" -> allCircles.filter { it.category == "Party" }
                        "Study" -> allCircles.filter { it.category == "Study" }
                        else -> allCircles
                    }
                } else {
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to load nearby circles"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unknown error occurred"
            } finally {
                isLoading = false
            }
        }
    }
    
    // Update filter and reload circles
    fun setFilter(filter: String) {
        currentFilter = filter
        loadNearbyCircles()
    }
    
    // Request location update (would connect to actual location services in a real app)
    fun requestLocationUpdate() {
        // In a real app, this would request a location update from the device
        // For now, just simulate with a slight location change
        userLat = userLat?.plus(0.001) ?: 37.7749
        userLng = userLng?.plus(0.001) ?: -122.4194
        
        // Reload circles with new location
        loadNearbyCircles()
    }
    
    // Update user location
    fun updateUserLocation(lat: Double, lng: Double) {
        userLat = lat
        userLng = lng
        
        // In a real app, we would also update the user's location in Firestore
        viewModelScope.launch {
            try {
                auth.currentUser?.let { user ->
                    firestore.collection("users").document(user.uid)
                        .update(
                            mapOf(
                                "lastLat" to lat,
                                "lastLng" to lng,
                                "lastLocationUpdate" to com.google.firebase.Timestamp.now()
                            )
                        )
                        .await()
                }
            } catch (e: Exception) {
                // Ignore errors updating location
            }
        }
    }
    
    // Zoom controls
    fun zoomIn() {
        mapZoom = (mapZoom * 1.2f).coerceAtMost(5.0f)
    }
    
    fun zoomOut() {
        mapZoom = (mapZoom * 0.8f).coerceAtLeast(0.5f)
    }
    
    // Clear error message
    fun clearError() {
        errorMessage = null
    }
} 