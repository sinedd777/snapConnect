package com.example.myapplication.data.repositories

import com.example.myapplication.data.models.FriendRequest
import com.example.myapplication.data.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class FriendRepository {

    private val firestore = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    // Friend request statuses
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_ACCEPTED = "accepted"
        const val STATUS_REJECTED = "rejected"
    }

    suspend fun sendFriendRequest(targetUserId: String): Result<Unit> = try {
        val currentUserId = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("No user"))
        
        // Check if request already exists
        val existingRequests = firestore.collection("friendships")
            .whereEqualTo("userA", currentUserId)
            .whereEqualTo("userB", targetUserId)
            .get()
            .await()
            
        if (existingRequests.isEmpty) {
            // Create new request
            val friendshipId = "$currentUserId:$targetUserId"
            val data = mapOf(
                "id" to friendshipId,
                "userA" to currentUserId,
                "userB" to targetUserId,
                "status" to STATUS_PENDING,
                "createdAt" to com.google.firebase.Timestamp.now()
            )
            
            firestore.collection("friendships").document(friendshipId).set(data).await()
        }
        
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    suspend fun acceptFriendRequest(requestId: String): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("No user"))
            
            // Get the request
            val request = firestore.collection("friendships").document(requestId).get().await()
            
            if (!request.exists()) {
                return Result.failure(IllegalStateException("Friend request not found"))
            }
            
            // Verify this user is the recipient
            val userB = request.getString("userB")
            if (userB != currentUserId) {
                return Result.failure(IllegalStateException("Not authorized to accept this request"))
            }
            
            // Update status to accepted
            firestore.collection("friendships").document(requestId)
                .update("status", STATUS_ACCEPTED)
                .await()
                
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun rejectFriendRequest(requestId: String): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("No user"))
            
            // Get the request
            val request = firestore.collection("friendships").document(requestId).get().await()
            
            if (!request.exists()) {
                return Result.failure(IllegalStateException("Friend request not found"))
            }
            
            // Verify this user is the recipient
            val userB = request.getString("userB")
            if (userB != currentUserId) {
                return Result.failure(IllegalStateException("Not authorized to reject this request"))
            }
            
            // Update status to rejected
            firestore.collection("friendships").document(requestId)
                .update("status", STATUS_REJECTED)
                .await()
                
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getFriends(): Result<List<User>> = try {
        val currentUserId = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("No user"))
        
        // Get friendships where current user is either userA or userB and status is accepted
        val friendshipsAsUserA = firestore.collection("friendships")
            .whereEqualTo("userA", currentUserId)
            .whereEqualTo("status", STATUS_ACCEPTED)
            .get()
            .await()
            
        val friendshipsAsUserB = firestore.collection("friendships")
            .whereEqualTo("userB", currentUserId)
            .whereEqualTo("status", STATUS_ACCEPTED)
            .get()
            .await()
            
        // Combine results and extract friend IDs
        val friendIds = mutableListOf<String>()
        
        friendshipsAsUserA.documents.forEach { doc ->
            doc.getString("userB")?.let { friendIds.add(it) }
        }
        
        friendshipsAsUserB.documents.forEach { doc ->
            doc.getString("userA")?.let { friendIds.add(it) }
        }
        
        // Get user details for all friends
        val friends = mutableListOf<User>()
        
        for (friendId in friendIds) {
            val userDoc = firestore.collection("users").document(friendId).get().await()
            if (userDoc.exists()) {
                userDoc.data?.let { 
                    friends.add(User.fromMap(it))
                }
            }
        }
        
        Result.success(friends)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    suspend fun getPendingFriendRequests(): Result<List<FriendRequest>> = try {
        val currentUserId = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("No user"))
        
        // Get pending requests where current user is the recipient
        val requests = firestore.collection("friendships")
            .whereEqualTo("userB", currentUserId)
            .whereEqualTo("status", STATUS_PENDING)
            .get()
            .await()
            
        // Get user details for each requester
        val pendingRequests = mutableListOf<FriendRequest>()
        
        for (request in requests.documents) {
            val requestData = request.data
            if (requestData != null) {
                val requesterId = requestData["userA"] as String
                val requesterDoc = firestore.collection("users").document(requesterId).get().await()
                
                if (requesterDoc.exists()) {
                    val requesterData = requesterDoc.data ?: continue
                    val combinedData = mutableMapOf<String, Any>()
                    combinedData.putAll(requestData)
                    combinedData["requesterDetails"] = requesterData
                    pendingRequests.add(FriendRequest.fromMap(combinedData))
                }
            }
        }
        
        Result.success(pendingRequests)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    suspend fun searchUsers(query: String): Result<List<User>> = try {
        val currentUserId = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("No user"))
        
        // Search for users by email or username
        val results = firestore.collection("users")
            .whereGreaterThanOrEqualTo("email", query)
            .whereLessThanOrEqualTo("email", query + "\uf8ff")
            .limit(10)
            .get()
            .await()
            
        val users = results.documents
            .mapNotNull { it.data }
            .filter { it["uid"] != currentUserId } // Exclude current user
            .map { User.fromMap(it) }
            
        Result.success(users)
    } catch (e: Exception) {
        Result.failure(e)
    }
} 