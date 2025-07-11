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
     * Update Circle details with a map of fields (creator only)
     */
    suspend fun updateCircle(circleId: String, updates: Map<String, Any>): Result<Circle> {
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
            
            Log.d(TAG, "Fetching all circles for debugging...")
            
            // Simple query to get all circles
            val snapshot = firestore.collection(CIRCLES_COLLECTION)
                .get()
                .await()
            
            // Log raw results
            snapshot.documents.forEach { doc ->
                Log.d(TAG, """Raw document:
                    |ID: ${doc.id}
                    |Data: ${doc.data}""".trimMargin())
            }
            
            // Convert to Circle objects
            val circles = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data?.plus(mapOf("id" to doc.id)) ?: return@mapNotNull null
                    val circle = Circle.fromMap(data)
                    
                    // Log circle details for debugging
                    Log.d(TAG, """Processing circle:
                        |ID: ${circle.id}
                        |Name: ${circle.name}
                        |Location enabled: ${circle.locationEnabled}
                        |Location: (${circle.locationLat}, ${circle.locationLng})
                        |Private: ${circle.private}
                        |Creator: ${circle.creatorId}
                        |Members: ${circle.members}""".trimMargin())
                    
                    circle
                } catch (e: Exception) {
                    Log.e(TAG, "Error converting document to Circle: ${e.message}", e)
                    null
                }
            }

            Log.d(TAG, "Found ${circles.size} circles in total")
            Result.success(circles)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting circles: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Join a public Circle
     */
    suspend fun joinPublicCircle(circleId: String): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("User not authenticated"))
            
            // Check if the circle exists and is public
            val circleResult = getCircleById(circleId)
            if (circleResult.isFailure) {
                return Result.failure(circleResult.exceptionOrNull() ?: IllegalStateException("Failed to get circle"))
            }
            
            val circle = circleResult.getOrThrow()
            
            // Check if circle is public
            if (circle.private) {
                return Result.failure(IllegalStateException("Cannot join a private circle without an invitation"))
            }
            
            // Check if user is already a member
            if (circle.members.contains(currentUserId)) {
                return Result.failure(IllegalStateException("You are already a member of this circle"))
            }
            
            // Add user to members
            firestore.collection(CIRCLES_COLLECTION)
                .document(circleId)
                .update("members", FieldValue.arrayUnion(currentUserId))
                .await()
                
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
} 