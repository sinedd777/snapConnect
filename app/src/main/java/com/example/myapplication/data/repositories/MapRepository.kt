package com.example.myapplication.data.repositories

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Repository for map-related operations
 */
class MapRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    /**
     * Get list of college towns
     */
    suspend fun getCollegeTowns(): Result<List<String>> {
        return try {
            // In a real app, this would fetch from a database or API
            // For now, return a hardcoded list
            val collegeTowns = listOf(
                "Berkeley, CA", 
                "Cambridge, MA", 
                "Ann Arbor, MI", 
                "Austin, TX", 
                "Madison, WI",
                "Chapel Hill, NC",
                "Boulder, CO",
                "Ithaca, NY",
                "Palo Alto, CA",
                "Evanston, IL",
                "State College, PA",
                "Athens, GA",
                "Columbus, OH",
                "Bloomington, IN",
                "Charlottesville, VA",
                "Urbana-Champaign, IL",
                "Eugene, OR",
                "Gainesville, FL",
                "Tempe, AZ",
                "College Station, TX"
            )
            Result.success(collegeTowns)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Calculate distance between two points in kilometers using Haversine formula
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371 // Earth's radius in kilometers
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    /**
     * Verify if a location is within a college town
     */
    suspend fun verifyCollegeTownLocation(lat: Double, lng: Double): Result<String?> {
        return try {
            // Get all college towns with their coordinates
            val collegeTownCoordinates = mapOf(
                "Berkeley, CA" to Pair(37.8715, -122.2730),
                "Cambridge, MA" to Pair(42.3736, -71.1189),
                "Ann Arbor, MI" to Pair(42.2808, -83.7430),
                "Austin, TX" to Pair(30.2849, -97.7341),
                "Madison, WI" to Pair(43.0766, -89.4125),
                "Chapel Hill, NC" to Pair(35.9132, -79.0558),
                "Boulder, CO" to Pair(40.0150, -105.2705),
                "Ithaca, NY" to Pair(42.4440, -76.5019),
                "Palo Alto, CA" to Pair(37.4419, -122.1430),
                "Evanston, IL" to Pair(42.0451, -87.6877),
                "State College, PA" to Pair(40.7934, -77.8600),
                "Athens, GA" to Pair(33.9519, -83.3576),
                "Columbus, OH" to Pair(40.0014, -83.0167),
                "Bloomington, IN" to Pair(39.1653, -86.5264),
                "Charlottesville, VA" to Pair(38.0293, -78.4767),
                "Urbana-Champaign, IL" to Pair(40.1106, -88.2073),
                "Eugene, OR" to Pair(44.0521, -123.0868),
                "Gainesville, FL" to Pair(29.6516, -82.3248),
                "Tempe, AZ" to Pair(33.4255, -111.9400),
                "College Station, TX" to Pair(30.6280, -96.3344)
            )

            // Find the nearest college town
            var nearestTown: String? = null
            var shortestDistance = Double.MAX_VALUE

            collegeTownCoordinates.forEach { (town, coords) ->
                val distance = calculateDistance(lat, lng, coords.first, coords.second)
                if (distance < shortestDistance) {
                    shortestDistance = distance
                    nearestTown = town
                }
            }

            // Only return the town if it's within 50km (roughly 31 miles)
            if (shortestDistance <= 50.0) {
                Result.success(nearestTown)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get map data for a specific college town
     */
    suspend fun getCollegeTownMapData(collegeTown: String): Result<Map<String, Any>> {
        return try {
            // In a real app, this would fetch map data from a service
            // For now, return dummy data
            val mapData = mapOf(
                "centerLat" to when (collegeTown) {
                    "Berkeley, CA" -> 37.8715
                    "Cambridge, MA" -> 42.3736
                    "Ann Arbor, MI" -> 42.2808
                    "Austin, TX" -> 30.2849
                    "Madison, WI" -> 43.0766
                    else -> 37.8715 // Default to Berkeley
                },
                "centerLng" to when (collegeTown) {
                    "Berkeley, CA" -> -122.2730
                    "Cambridge, MA" -> -71.1189
                    "Ann Arbor, MI" -> -83.7430
                    "Austin, TX" -> -97.7341
                    "Madison, WI" -> -89.4125
                    else -> -122.2730 // Default to Berkeley
                },
                "zoomLevel" to 14.0,
                "hasGeofence" to true,
                "radiusKm" to 1.0
            )
            Result.success(mapData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update user's location in Firestore
     */
    suspend fun updateUserLocation(lat: Double, lng: Double): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")
            
            firestore.collection("users")
                .document(userId)
                .update(mapOf(
                    "lastLat" to lat,
                    "lastLng" to lng,
                    "lastLocationUpdate" to com.google.firebase.Timestamp.now()
                ))
                .await()
                
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 