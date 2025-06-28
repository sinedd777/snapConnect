package com.example.myapplication.data.repositories

import com.example.myapplication.data.models.Circle
import com.example.myapplication.data.models.User
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit
import com.google.firebase.firestore.GeoPoint
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import android.util.Log

class CircleRepository {
    private val firestore: FirebaseFirestore = Firebase.firestore
    private val storage = Firebase.storage
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    
    // Debug tag
    private val TAG = "CircleRepository"
    
    // Collection names
    companion object {
        private const val CIRCLES_COLLECTION = "circles"
        private const val USERS_COLLECTION = "users"
        
        // Circle duration options in hours
        val DURATION_1_HOUR = TimeUnit.HOURS.toMillis(1)
        val DURATION_24_HOURS = TimeUnit.HOURS.toMillis(24)
        val DURATION_48_HOURS = TimeUnit.HOURS.toMillis(48)
        val DURATION_72_HOURS = TimeUnit.HOURS.toMillis(72)
        val DURATION_7_DAYS = TimeUnit.DAYS.toMillis(7)
    }
    
    /**
     * Create a new Circle
     */
    suspend fun createCircle(
        name: String,
        description: String? = null,
        durationMillis: Long = DURATION_24_HOURS,
        private: Boolean = true,
        locationEnabled: Boolean = false,
        locationLat: Double? = null,
        locationLng: Double? = null,
        locationRadius: Double? = null,
        initialMembers: List<String> = emptyList()
    ): Result<Circle> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("User not authenticated"))
            
            // Calculate expiration time
            val expiresAt = Calendar.getInstance().apply {
                time = Date()
                add(Calendar.MILLISECOND, durationMillis.toInt())
            }.time
            
            // Create members list with creator always included
            val members = if (initialMembers.contains(currentUserId)) {
                initialMembers
            } else {
                initialMembers + currentUserId
            }
            
            // Generate geohash if location is enabled
            val geohash = if (locationEnabled && locationLat != null && locationLng != null) {
                GeoFireUtils.getGeoHashForLocation(GeoLocation(locationLat, locationLng))
            } else null
            
            // Create Circle object
            val circle = Circle(
                name = name,
                description = description,
                creatorId = currentUserId,
                members = members,
                createdAt = Timestamp.now(),
                expiresAt = Timestamp(expiresAt),
                locationEnabled = locationEnabled,
                locationLat = locationLat,
                locationLng = locationLng,
                locationRadius = locationRadius,
                private = private,
                geohash = geohash
            )
            
            // Save to Firestore
            firestore.collection(CIRCLES_COLLECTION)
                .document(circle.id)
                .set(circle)
                .await()
                
            Result.success(circle)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get a Circle by ID
     */
    suspend fun getCircleById(circleId: String): Result<Circle> {
        return try {
            val circleDoc = firestore.collection(CIRCLES_COLLECTION)
                .document(circleId)
                .get()
                .await()
                
            if (circleDoc.exists()) {
                val data = circleDoc.data
                if (data != null) {
                    data["id"] = circleDoc.id
                    Result.success(Circle.fromMap(data))
                } else {
                    Result.failure(IllegalStateException("Circle data is null"))
                }
            } else {
                Result.failure(IllegalStateException("Circle not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all Circles for the current user
     */
    suspend fun getUserCircles(limit: Long = 20): Result<List<Circle>> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("User not authenticated"))
            
            val circleDocs = firestore.collection(CIRCLES_COLLECTION)
                .whereArrayContains("members", currentUserId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()
                
            val circles = circleDocs.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                data["id"] = doc.id
                Circle.fromMap(data)
            }
            
            Result.success(circles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Add a member to a Circle
     */
    suspend fun addMemberToCircle(circleId: String, userId: String): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("User not authenticated"))
            
            // Check if the circle exists and the current user is a member
            val circleResult = getCircleById(circleId)
            if (circleResult.isFailure) {
                return Result.failure(circleResult.exceptionOrNull() ?: IllegalStateException("Failed to get circle"))
            }
            
            val circle = circleResult.getOrThrow()
            if (!circle.members.contains(currentUserId) && circle.creatorId != currentUserId) {
                return Result.failure(IllegalStateException("You don't have permission to add members to this circle"))
            }
            
            // Add the user to the members array
            firestore.collection(CIRCLES_COLLECTION)
                .document(circleId)
                .update("members", FieldValue.arrayUnion(userId))
                .await()
                
            // Remove from pending invites if present
            firestore.collection(CIRCLES_COLLECTION)
                .document(circleId)
                .update("pendingInvites", FieldValue.arrayRemove(userId))
                .await()
                
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Invite a user to a Circle
     */
    suspend fun inviteUserToCircle(circleId: String, userId: String): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("User not authenticated"))
            
            // Check if the circle exists and the current user is a member
            val circleResult = getCircleById(circleId)
            if (circleResult.isFailure) {
                return Result.failure(circleResult.exceptionOrNull() ?: IllegalStateException("Failed to get circle"))
            }
            
            val circle = circleResult.getOrThrow()
            if (!circle.members.contains(currentUserId) && circle.creatorId != currentUserId) {
                return Result.failure(IllegalStateException("You don't have permission to invite users to this circle"))
            }
            
            // Add the user to the pending invites array
            firestore.collection(CIRCLES_COLLECTION)
                .document(circleId)
                .update("pendingInvites", FieldValue.arrayUnion(userId))
                .await()
                
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Leave a Circle
     */
    suspend fun leaveCircle(circleId: String): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("User not authenticated"))
            
            // Remove the user from the members array
            firestore.collection(CIRCLES_COLLECTION)
                .document(circleId)
                .update("members", FieldValue.arrayRemove(currentUserId))
                .await()
                
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete a Circle (creator only)
     */
    suspend fun deleteCircle(circleId: String): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("User not authenticated"))
    
            // Check if the circle exists and the current user is the creator
            val circleResult = getCircleById(circleId)
            if (circleResult.isFailure) {
                return Result.failure(circleResult.exceptionOrNull() ?: IllegalStateException("Failed to get circle"))
            }
            
            val circle = circleResult.getOrThrow()
            if (circle.creatorId != currentUserId) {
                return Result.failure(IllegalStateException("Only the creator can delete this circle"))
            }
            
            // First, get all snaps for this circle
            val snapsQuery = firestore.collection("snaps")
                .whereEqualTo("circleId", circleId)
                .get()
                .await()
            
            // Delete all snaps in a batch
            val batch = firestore.batch()
            snapsQuery.documents.forEach { snapDoc ->
                batch.delete(snapDoc.reference)
                
                // Also delete the snap media from storage if it exists
                val mediaUrl = snapDoc.getString("mediaUrl")
                if (mediaUrl != null) {
                    try {
                        storage.getReferenceFromUrl(mediaUrl).delete().await()
                    } catch (e: Exception) {
                        // Log but don't fail if media deletion fails
                        println("Failed to delete media for snap ${snapDoc.id}: ${e.message}")
                    }
                }
            }
            
            // Delete the circle document
            batch.delete(firestore.collection(CIRCLES_COLLECTION).document(circleId))
            
            // Commit the batch
            batch.commit().await()
                
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update Circle details (creator only)
     */
    suspend fun updateCircle(
        circleId: String,
        name: String? = null,
        description: String? = null,
        private: Boolean? = null,
        locationEnabled: Boolean? = null,
        locationLat: Double? = null,
        locationLng: Double? = null,
        locationRadius: Double? = null
    ): Result<Circle> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("User not authenticated"))
            
            // Check if the circle exists and the current user is the creator
            val circleResult = getCircleById(circleId)
            if (circleResult.isFailure) {
                return Result.failure(circleResult.exceptionOrNull() ?: IllegalStateException("Failed to get circle"))
            }
            
            val circle = circleResult.getOrThrow()
            if (circle.creatorId != currentUserId) {
                return Result.failure(IllegalStateException("Only the creator can update this circle"))
            }
            
            // Build update map with only non-null fields
            val updates = mutableMapOf<String, Any>()
            name?.let { updates["name"] = it }
            description?.let { updates["description"] = it }
            private?.let { updates["private"] = it }
            locationEnabled?.let { updates["locationEnabled"] = it }
            locationLat?.let { updates["locationLat"] = it }
            locationLng?.let { updates["locationLng"] = it }
            locationRadius?.let { updates["locationRadius"] = it }
            
            if (updates.isEmpty()) {
                return Result.success(circle)
            }
            
            // Update the circle
            firestore.collection(CIRCLES_COLLECTION)
                .document(circleId)
                .update(updates)
                .await()
                
            // Get the updated circle
            getCircleById(circleId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get pending Circle invites for the current user
     */
    suspend fun getPendingInvites(): Result<List<Circle>> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("User not authenticated"))
            
            val circleDocs = firestore.collection(CIRCLES_COLLECTION)
                .whereArrayContains("pendingInvites", currentUserId)
                .get()
                .await()
                
            val circles = circleDocs.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                data["id"] = doc.id
                Circle.fromMap(data)
            }
            
            Result.success(circles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Accept a Circle invitation
     */
    suspend fun acceptInvitation(circleId: String): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("User not authenticated"))
            
            // Check if the invitation exists
            val circleResult = getCircleById(circleId)
            if (circleResult.isFailure) {
                return Result.failure(circleResult.exceptionOrNull() ?: IllegalStateException("Failed to get circle"))
            }
            
            val circle = circleResult.getOrThrow()
            if (!circle.pendingInvites.contains(currentUserId)) {
                return Result.failure(IllegalStateException("No invitation found for this circle"))
            }
            
            // Add user to members and remove from pending invites
            firestore.collection(CIRCLES_COLLECTION)
                .document(circleId)
                .update(
                    mapOf(
                        "members" to FieldValue.arrayUnion(currentUserId),
                        "pendingInvites" to FieldValue.arrayRemove(currentUserId)
                    )
                )
                .await()
                
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Decline a Circle invitation
     */
    suspend fun declineInvitation(circleId: String): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("User not authenticated"))
            
            // Remove from pending invites
            firestore.collection(CIRCLES_COLLECTION)
                .document(circleId)
                .update("pendingInvites", FieldValue.arrayRemove(currentUserId))
                .await()
                
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get nearby Circles based on location and radius
     */
    suspend fun getNearbyCircles(
        lat: Double,
        lng: Double,
        radiusKm: Double = 1.0,
        limit: Long = 50
    ): Result<List<Circle>> {
        return try {
            val currentUserId = auth.currentUser?.uid
            if (currentUserId == null) {
                return Result.failure(IllegalStateException("User not authenticated"))
            }
            
            // Convert radius to meters
            val radiusInM = radiusKm * 1000
            
            // Get the geohash query bounds
            val center = GeoLocation(lat, lng)
            val bounds = GeoFireUtils.getGeoHashQueryBounds(center, radiusInM)
            
            
            // First, let's check how many total circles exist
            val totalCircles = firestore.collection(CIRCLES_COLLECTION).get().await()
            
            
            // Create a list to hold all query tasks
            val tasks = bounds.map { bound ->
                firestore.collection(CIRCLES_COLLECTION)
                    .whereEqualTo("private", false)
                    .orderBy("geohash")
                    .startAt(bound.startHash)
                    .endAt(bound.endHash)
                    .get()
            }
            
            // Wait for all queries to complete
            val snapshots = tasks.map { it.await() }
            
            // Log raw results
            snapshots.forEachIndexed { index, snapshot ->
                snapshot.documents.forEach { doc ->
                    Log.d(TAG, """Raw document:
                        |ID: ${doc.id}
                        |Data: ${doc.data}""".trimMargin())
                }
            }
            
            // Merge results and filter by actual distance
            val matchingDocs = snapshots.flatMap { snapshot ->
                snapshot.documents.filter { doc ->
                    val lat = doc.getDouble("locationLat")
                    val lng = doc.getDouble("locationLng")
                    
                    if (lat == null || lng == null) {
                        return@filter false
                    }
                    
                    // We have to filter out a few false positives due to GeoHash accuracy
                    val docLocation = GeoLocation(lat, lng)
                    val distanceInM = GeoFireUtils.getDistanceBetween(docLocation, center)
                    val isWithinRadius = distanceInM <= radiusInM
                    
                    isWithinRadius
                }
            }
            
            // Convert to Circle objects
            val circles = matchingDocs.mapNotNull { doc ->
                try {
                    val data = doc.data
                    if (data == null) {
                        return@mapNotNull null
                    }
                    data["id"] = doc.id
                    Circle.fromMap(data)
                } catch (e: Exception) {
                    null
                }
            }
            
            
            Result.success(circles)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting nearby circles: ${e.message}", e)
            Result.failure(e)
        }
    }
    
} 