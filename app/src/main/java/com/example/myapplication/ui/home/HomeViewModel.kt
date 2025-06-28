package com.example.myapplication.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.models.Circle
import com.example.myapplication.data.repositories.CircleRepository
import com.example.myapplication.data.repositories.MapRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.location.Location
import android.content.Context
import android.annotation.SuppressLint
import android.util.Log
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HomeViewModel : ViewModel() {
    private val circleRepository = CircleRepository()
    private val mapRepository = MapRepository()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    companion object {
        private const val TAG = "HomeViewModel"
    }
    
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
    
    // Location state
    var isLocationPermissionGranted by mutableStateOf(false)
        // Make this publicly settable
        
    var isLocationEnabled by mutableStateOf(false)
        private set
    
    // Debug flag
    private val isDebugMode = false
    
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
                        
                        // Load circles after getting user profile
                        loadNearbyCircles()
                    }
                }
            } catch (e: Exception) {
                // Ignore error, location will stay null
                if (isDebugMode) {
                    Log.e("HomeViewModel", "Error loading user profile: ${e.message}")
                }
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
                
                Log.d(TAG, "Loading circles for location: ($lat, $lng)")
                Log.d(TAG, "Current filter: $currentFilter")
                
                val result = circleRepository.getNearbyCircles(lat, lng)
                
                if (result.isSuccess) {
                    var allCircles = result.getOrNull() ?: emptyList()
                    Log.d(TAG, "Loaded ${allCircles.size} circles before filtering")
                    
                    // Log each circle's details
                    allCircles.forEach { circle ->
                        Log.d(TAG, """Circle details:
                            |ID: ${circle.id}
                            |Name: ${circle.name}
                            |Location: (${circle.locationLat}, ${circle.locationLng})
                            |Private: ${circle.private}
                            |Category: ${circle.category}
                            |Members: ${circle.members.size}""".trimMargin())
                    }
                    
                    // Apply filter
                    circles = when (currentFilter) {
                        "Public" -> allCircles.filter { !it.private }
                        "Private" -> allCircles.filter { it.private }
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
                    
                    Log.d(TAG, "After filtering: ${circles.size} circles remain")
                } else {
                    val error = result.exceptionOrNull()
                    Log.e(TAG, "Failed to load circles: ${error?.message}", error)
                    errorMessage = error?.message ?: "Failed to load nearby circles"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading circles: ${e.message}", e)
                errorMessage = e.message ?: "Unknown error occurred"
            } finally {
                isLoading = false
            }
        }
    }
    
    // Create test circles for debugging purposes
    
    
    // Update filter and reload circles
    fun setFilter(filter: String) {
        currentFilter = filter
        loadNearbyCircles()
    }
    
    // Request location update from the device
    @SuppressLint("MissingPermission")
    fun requestLocationUpdate(context: Context) {
        viewModelScope.launch {
            try {
                isLoading = true
                
                // Use Google Play Services location API
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                val cancellationToken = CancellationTokenSource()
                
                // Request current location with high accuracy
                val locationResult = withContext(Dispatchers.IO) {
                    try {
                        fusedLocationClient.getCurrentLocation(
                            Priority.PRIORITY_HIGH_ACCURACY,
                            cancellationToken.token
                        ).await()
                    } catch (e: Exception) {
                        if (isDebugMode) {
                            Log.e("HomeViewModel", "Error getting current location: ${e.message}")
                        }
                        null
                    }
                }
                
                // Update location if available
                locationResult?.let { location ->
                    userLat = location.latitude
                    userLng = location.longitude
                    isLocationEnabled = true
                    
                    if (isDebugMode) {
                        Log.d("HomeViewModel", "Got location update: ${location.latitude}, ${location.longitude}")
                    }
                    
                    // Update the user's location in Firestore
                    updateUserLocation(location.latitude, location.longitude)
                } ?: run {
                    // Try to get last known location if current location request failed
                    val lastLocation = withContext(Dispatchers.IO) {
                        try {
                            fusedLocationClient.lastLocation.await()
                        } catch (e: Exception) {
                            if (isDebugMode) {
                                Log.e("HomeViewModel", "Error getting last location: ${e.message}")
                            }
                            null
                        }
                    }
                    
                    lastLocation?.let { location ->
                        userLat = location.latitude
                        userLng = location.longitude
                        isLocationEnabled = true
                        
                        if (isDebugMode) {
                            Log.d("HomeViewModel", "Using last known location: ${location.latitude}, ${location.longitude}")
                        }
                        
                        // Update the user's location in Firestore
                        updateUserLocation(location.latitude, location.longitude)
                    } ?: run {
                        errorMessage = "Could not get location. Please check your location settings."
                        if (isDebugMode) {
                            Log.e("HomeViewModel", "No location available")
                        }
                    }
                }
                
                // Reload circles with new location
                loadNearbyCircles()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unknown error occurred"
                if (isDebugMode) {
                    Log.e("HomeViewModel", "Exception updating location: ${e.message}")
                }
            } finally {
                isLoading = false
            }
        }
    }
    
    // Update user location
    fun updateUserLocation(lat: Double, lng: Double) {
        viewModelScope.launch {
            try {
                userLat = lat
                userLng = lng
                
                // Update the user's location in Firestore
                val result = mapRepository.updateUserLocation(lat, lng)
                if (result.isFailure) {
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to update location"
                    if (isDebugMode) {
                        Log.e("HomeViewModel", "Error updating user location: $errorMessage")
                    }
                } else if (isDebugMode) {
                    Log.d("HomeViewModel", "Updated user location in Firestore")
                }
                
                // Reload circles with new location
                loadNearbyCircles()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unknown error occurred"
                if (isDebugMode) {
                    Log.e("HomeViewModel", "Exception updating user location: ${e.message}")
                }
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