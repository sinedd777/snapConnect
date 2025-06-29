package com.example.myapplication.data.repositories

import android.content.Context
import android.net.Uri
import com.example.myapplication.data.models.Circle
import com.example.myapplication.data.models.Snap
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class SnapRepository {

    private val storage = Firebase.storage
    private val firestore = Firebase.firestore
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val ragRepository = RAGRepository()

    /**
     * Upload a snap to be shared with specific recipients, with optional RAG caption generation
     */
    suspend fun uploadSnap(
        localUri: Uri, 
        recipients: List<String>? = null,
        caption: String? = null,
        context: Context? = null // Context needed for RAG caption generation
    ): Result<Uri> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("No user"))
            val snapId = UUID.randomUUID().toString()
            val ref = storage.reference.child("snaps/$uid/$snapId.jpg")
            ref.putFile(localUri).await()
            val downloadUrl = ref.downloadUrl.await()

            // Use provided recipients or default to just the sender
            val finalRecipients = if (recipients.isNullOrEmpty()) {
                listOf(uid)
            } else {
                // Always include the sender in recipients
                if (recipients.contains(uid)) recipients else recipients + uid
            }

            // Store metadata
            val data = mutableMapOf(
                "id" to snapId,
                "sender" to uid,
                "mediaUrl" to downloadUrl.toString(),
                "mediaType" to "image/jpeg",
                "createdAt" to com.google.firebase.Timestamp.now(),
                "recipients" to finalRecipients,
                "viewedBy" to listOf<String>(),
                "screenshotBy" to listOf<String>(),
                "isCircleContent" to false
            )
            
            // Add user-provided caption if provided
            caption?.let { data["caption"] = it }
            
            // Create initial snap document
            val snapDoc = firestore.collection("snaps").document(snapId)
            snapDoc.set(data).await()
            
            // Generate RAG caption if context is provided
            if (context != null) {
                val snap = Snap.fromMap(data)
                ragRepository.generateAndStoreSnapCaption(snap, localUri, context)
                    .onFailure { e -> 
                        println("Failed to generate RAG caption: ${e.message}")
                    }
            }

            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Upload a snap to a Circle, with RAG caption generation
     */
    suspend fun uploadSnapToCircle(
        localUri: Uri, 
        circleId: String,
        caption: String? = null,
        context: Context? = null // Context needed for RAG caption generation
    ): Result<Uri> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("No user"))
            
            // Verify the user is a member of the circle
            val circleDoc = firestore.collection("circles").document(circleId).get().await()
            if (!circleDoc.exists()) {
                return Result.failure(IllegalStateException("Circle not found"))
            }
            
            val members = circleDoc.get("members") as? List<String> ?: emptyList()
            if (!members.contains(uid)) {
                return Result.failure(IllegalStateException("You are not a member of this circle"))
            }
            
            val snapId = UUID.randomUUID().toString()
            val ref = storage.reference.child("circles/$circleId/snaps/$snapId.jpg")
            ref.putFile(localUri).await()
            val downloadUrl = ref.downloadUrl.await()

            // Store metadata
            val data = mutableMapOf(
                "id" to snapId,
                "sender" to uid,
                "mediaUrl" to downloadUrl.toString(),
                "mediaType" to "image/jpeg",
                "createdAt" to com.google.firebase.Timestamp.now(),
                "recipients" to members,
                "circleId" to circleId,
                "viewedBy" to listOf<String>(),
                "screenshotBy" to listOf<String>(),
                "isCircleContent" to true
            )
            
            // Add user-provided caption if provided
            caption?.let { data["caption"] = it }
            
            // Create initial snap document
            val snapDoc = firestore.collection("snaps").document(snapId)
            snapDoc.set(data).await()
            
            // Generate RAG caption if context is provided
            if (context != null) {
                val snap = Snap.fromMap(data)
                ragRepository.generateAndStoreSnapCaption(snap, localUri, context)
                    .onFailure { e -> 
                        println("Failed to generate RAG caption: ${e.message}")
                    }
            }

            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get snaps for a specific user
     */
    suspend fun getSnapsForUser(limit: Long = 20): Result<List<Snap>> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("No user"))
            
            val snapshots = firestore.collection("snaps")
                .whereArrayContains("recipients", uid)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()
                
            val snaps = snapshots.documents.mapNotNull { doc -> 
                try {
                    val data = doc.data
                    if (data != null) {
                        // Add the document ID to the data map
                        val snapData = data.toMutableMap()
                        snapData["id"] = doc.id
                        
                        // Ensure all required fields are present
                        if (snapData["sender"] == null || 
                            snapData["mediaUrl"] == null || 
                            snapData["mediaType"] == null || 
                            snapData["createdAt"] == null || 
                            snapData["recipients"] == null) {
                            return@mapNotNull null
                        }
                        
                        Snap.fromMap(snapData)
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    // Log the error but continue processing other documents
                    println("Error processing snap document ${doc.id}: ${e.message}")
                    null
                }
            }
            
            // Fetch sender names for snaps
            val senderIds = snaps.map { it.sender }.distinct()
            val userDocs = mutableMapOf<String, Map<String, Any>?>()
            
            for (senderId in senderIds) {
                val userDoc = firestore.collection("users").document(senderId).get().await()
                userDocs[senderId] = userDoc.data
            }
            
            // Add sender names to snaps
            val snapsWithSenderNames = snaps.map { snap ->
                val senderData = userDocs[snap.sender]
                val senderName = senderData?.get("username") as? String ?: senderData?.get("email") as? String
                snap.copy(senderName = senderName)
            }
            
            Result.success(snapsWithSenderNames)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get snaps for a specific Circle
     */
    suspend fun getSnapsForCircle(circleId: String, limit: Long = 50): Result<List<Snap>> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("No user"))
            println("DEBUG-REPO: Getting snaps for circle $circleId, user $uid")
            
            // Verify the user is a member of the circle
            val circleDoc = firestore.collection("circles").document(circleId).get().await()
            if (!circleDoc.exists()) {
                println("DEBUG-REPO: Circle $circleId not found")
                return Result.failure(IllegalStateException("Circle not found"))
            }
            
            val members = circleDoc.get("members") as? List<String> ?: emptyList()
            val creatorId = circleDoc.get("creatorId") as? String
            
            println("DEBUG-REPO: Circle $circleId - members: $members, creator: $creatorId")
            println("DEBUG-REPO: Current user $uid - is member: ${members.contains(uid)}, is creator: ${creatorId == uid}")
            
            if (!members.contains(uid) && creatorId != uid) {
                println("DEBUG-REPO: User $uid is not a member or creator of circle $circleId")
                return Result.failure(IllegalStateException("You are not a member of this circle"))
            }
            
            println("DEBUG-REPO: Executing query for snaps with circleId=$circleId")
            val snapshots = firestore.collection("snaps")
                .whereEqualTo("circleId", circleId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()
                
            val snaps = snapshots.documents.mapNotNull { doc -> 
                try {
                    val data = doc.data
                    if (data != null) {
                        // Add the document ID to the data map
                        val snapData = data.toMutableMap()
                        snapData["id"] = doc.id
                        
                        Snap.fromMap(snapData)
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    // Log the error but continue processing other documents
                    println("Error processing snap document ${doc.id}: ${e.message}")
                    null
                }
            }
            
            // Fetch sender names for snaps
            val senderIds = snaps.map { it.sender }.distinct()
            val userDocs = mutableMapOf<String, Map<String, Any>?>()
            
            for (senderId in senderIds) {
                val userDoc = firestore.collection("users").document(senderId).get().await()
                userDocs[senderId] = userDoc.data
            }
            
            // Add sender names to snaps
            val snapsWithSenderNames = snaps.map { snap ->
                val senderData = userDocs[snap.sender]
                val senderName = senderData?.get("username") as? String ?: senderData?.get("email") as? String
                snap.copy(senderName = senderName)
            }
            
            Result.success(snapsWithSenderNames)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun markSnapViewed(snapId: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("No user"))
            
            val snapRef = firestore.collection("snaps").document(snapId)
            val snap = snapRef.get().await()
            
            if (snap.exists()) {
                val viewedBy = snap.get("viewedBy") as? List<String> ?: listOf()
                if (!viewedBy.contains(uid)) {
                    snapRef.update("viewedBy", viewedBy + uid).await()
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun markSnapScreenshot(snapId: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("No user"))
            
            val snapRef = firestore.collection("snaps").document(snapId)
            val snap = snapRef.get().await()
            
            if (snap.exists()) {
                val screenshotBy = snap.get("screenshotBy") as? List<String> ?: listOf()
                if (!screenshotBy.contains(uid)) {
                    snapRef.update("screenshotBy", screenshotBy + uid).await()
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getSnapById(snapId: String): Result<Snap> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("No user"))
            
            val snapDoc = firestore.collection("snaps").document(snapId).get().await()
            
            if (!snapDoc.exists()) {
                return Result.failure(IllegalStateException("Snap not found"))
            }
            
            val snapData = snapDoc.data
                ?: return Result.failure(IllegalStateException("Snap data is null"))
                
            // Check if user is a recipient
            val recipients = snapData["recipients"] as? List<String> ?: listOf()
            if (!recipients.contains(uid)) {
                return Result.failure(IllegalStateException("Not authorized to view this snap"))
            }
            
            // Add the document ID to the data map
            val fullSnapData = snapData.toMutableMap()
            fullSnapData["id"] = snapId
            
            val snap = try {
                Snap.fromMap(fullSnapData)
            } catch (e: Exception) {
                return Result.failure(IllegalStateException("Error creating Snap object: ${e.message}"))
            }
            
            // Get sender name
            val senderDoc = firestore.collection("users").document(snap.sender).get().await()
            val senderName = senderDoc.getString("username") ?: senderDoc.getString("email")
            
            Result.success(snap.copy(senderName = senderName))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 