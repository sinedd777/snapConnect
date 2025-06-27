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
import org.osmdroid.views.MapView
import android.util.Log

class CircleMapViewModel : ViewModel() {
    private val circleRepository = CircleRepository()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    // Debug tag
    private val TAG = "CircleMapViewModel"
    
    // Map reference (will be set by the composable)
    private var mapView: MapView? = null
    
    // Map state
    var circles by mutableStateOf<List<Circle>>(emptyList())
        private set
        
    var userLat by mutableStateOf<Double?>(null)
        private set
        
    var userLng by mutableStateOf<Double?>(null)
        private set
        
    var collegeTown by mutableStateOf<String?>(null)
        private set
        
    var mapZoom by mutableStateOf(15.0)
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
    
    // Set map reference
    fun setMapReference(map: MapView) {
        mapView = map
        mapZoom = map.zoomLevelDouble
    }
    
    // Load user profile data
    private fun loadUserProfile() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading user profile...")
                auth.currentUser?.let { user ->
                    Log.d(TAG, "Current user ID: ${user.uid}")
                    val userDoc = firestore.collection("users").document(user.uid).get().await()
                    if (userDoc.exists()) {
                        collegeTown = userDoc.getString("collegeTown")
                        userLat = userDoc.getDouble("lastLat")
                        userLng = userDoc.getDouble("lastLng")
                        Log.d(TAG, "User profile loaded - College Town: $collegeTown, Location: ($userLat, $userLng)")
                    } else {
                        Log.d(TAG, "User document does not exist")
                    }
                } ?: Log.d(TAG, "No authenticated user")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading user profile: ${e.message}")
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
                
                Log.d(TAG, """Loading circles:
                    |Location: ($lat, $lng)
                    |Current filter: $currentFilter
                    |College town: $collegeTown
                    |Current circles: ${circles.size}""".trimMargin())
                
                // If we have a college town, prioritize that for fetching circles
                val result = if (collegeTown != null) {
                    Log.d(TAG, "Fetching circles for college town: $collegeTown")
                    circleRepository.getCollegeTownCircles(collegeTown!!)
                } else {
                    Log.d(TAG, "Fetching nearby circles within 1km radius")
                    circleRepository.getNearbyCircles(lat, lng)
                }
                
                if (result.isSuccess) {
                    val allCircles = result.getOrNull() ?: emptyList()
                    Log.d(TAG, "Loaded ${allCircles.size} circles before filtering")
                    
                    // Log each circle's details
                    allCircles.forEach { circle ->
                        Log.d(TAG, """Circle details:
                            |ID: ${circle.id}
                            |Name: ${circle.name}
                            |Location: (${circle.locationLat}, ${circle.locationLng})
                            |Private: ${circle.isPrivate}
                            |Category: ${circle.category}
                            |Members: ${circle.members.size}""".trimMargin())
                    }
                    
                    // Apply filter
                    val filteredCircles = when (currentFilter) {
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
                    
                    Log.d(TAG, """Filtering results:
                        |Filter: $currentFilter
                        |Before: ${allCircles.size}
                        |After: ${filteredCircles.size}
                        |Removed: ${allCircles.size - filteredCircles.size}""".trimMargin())
                    
                    // Only update circles if they've actually changed
                    if (circles != filteredCircles) {
                        Log.d(TAG, "Updating circles list with ${filteredCircles.size} circles")
                        circles = filteredCircles
                    } else {
                        Log.d(TAG, "Circles list unchanged, skipping update")
                    }
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
    
    // Update filter and reload circles
    fun setFilter(filter: String) {
        if (currentFilter != filter) {
            Log.d(TAG, "Setting filter: $currentFilter -> $filter")
            currentFilter = filter
            loadNearbyCircles()
        } else {
            Log.d(TAG, "Filter unchanged: $filter")
        }
    }
    
    // Request location update (would connect to actual location services in a real app)
    fun requestLocationUpdate() {
        val oldLat = userLat
        val oldLng = userLng
        
        // In a real app, this would request a location update from the device
        // For now, just simulate with a slight location change
        userLat = userLat?.plus(0.001) ?: 37.7749
        userLng = userLng?.plus(0.001) ?: -122.4194
        
        Log.d(TAG, """Location update:
            |Old: ($oldLat, $oldLng)
            |New: ($userLat, $userLng)
            |Delta: (${userLat?.minus(oldLat ?: 0.0)}, ${userLng?.minus(oldLng ?: 0.0)})""".trimMargin())
        
        // Center map on user location
        mapView?.let { map ->
            map.controller.setCenter(org.osmdroid.util.GeoPoint(userLat!!, userLng!!))
        }
        
        // Reload circles with new location
        loadNearbyCircles()
    }
    
    // Update user location
    fun updateUserLocation(lat: Double, lng: Double) {
        if (lat != userLat || lng != userLng) {
            Log.d(TAG, """Updating user location:
                |Old: ($userLat, $userLng)
                |New: ($lat, $lng)
                |Delta: (${lat - (userLat ?: 0.0)}, ${lng - (userLng ?: 0.0)})""".trimMargin())
            
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
                        Log.d(TAG, "Successfully updated location in Firestore")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update location in Firestore: ${e.message}")
                }
            }
        } else {
            Log.d(TAG, "Location unchanged: ($lat, $lng)")
        }
    }
    
    // Zoom controls
    fun zoomIn() {
        mapView?.let { map ->
            map.controller.zoomIn()
            mapZoom = map.zoomLevelDouble
            Log.d(TAG, "Zoomed in to: $mapZoom")
        }
    }
    
    fun zoomOut() {
        mapView?.let { map ->
            map.controller.zoomOut()
            mapZoom = map.zoomLevelDouble
            Log.d(TAG, "Zoomed out to: $mapZoom")
        }
    }
    
    // Clear error message
    fun clearError() {
        errorMessage = null
    }
    
    // Create test circles for debugging
    private fun createTestCircles() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Creating test circles...")
                
                // Use current location or default to Berkeley
                val lat = userLat ?: 37.8715
                val lng = userLng ?: -122.2730
                
                // Create a test circle at the user's location
                val result1 = circleRepository.createCircle(
                    name = "Test Circle 1",
                    description = "A test circle for debugging",
                    durationMillis = CircleRepository.DURATION_24_HOURS,
                    isPrivate = false,
                    locationEnabled = true,
                    locationLat = lat,
                    locationLng = lng,
                    locationRadius = 100.0
                )
                
                if (result1.isSuccess) {
                    Log.d(TAG, "Successfully created test circle 1")
                } else {
                    Log.e(TAG, "Failed to create test circle 1: ${result1.exceptionOrNull()?.message}")
                }
                
                // Create another test circle nearby
                val result2 = circleRepository.createCircle(
                    name = "Test Circle 2",
                    description = "Another test circle for debugging",
                    durationMillis = CircleRepository.DURATION_48_HOURS,
                    isPrivate = false,
                    locationEnabled = true,
                    locationLat = lat + 0.002, // Slightly north
                    locationLng = lng + 0.002, // Slightly east
                    locationRadius = 150.0
                )
                
                if (result2.isSuccess) {
                    Log.d(TAG, "Successfully created test circle 2")
                } else {
                    Log.e(TAG, "Failed to create test circle 2: ${result2.exceptionOrNull()?.message}")
                }
                
                // Reload circles to see the new ones
                loadNearbyCircles()
            } catch (e: Exception) {
                Log.e(TAG, "Error creating test circles: ${e.message}", e)
            }
        }
    }
    
    // Function to create test data if needed
    fun createTestDataIfNeeded() {
        if (circles.isEmpty()) {
            Log.d(TAG, "No circles found, creating test data...")
            createTestCircles()
        }
    }
} 