package com.example.myapplication.ui.circles

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.models.Circle
import com.example.myapplication.data.models.Snap
import com.example.myapplication.data.models.User
import com.example.myapplication.data.repositories.CircleRepository
import com.example.myapplication.data.repositories.SnapRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CircleDetailViewModel : ViewModel() {
    private val circleRepository = CircleRepository()
    private val snapRepository = SnapRepository()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    var circle by mutableStateOf<Circle?>(null)
        private set
        
    var snaps by mutableStateOf<List<Snap>>(emptyList())
        private set
        
    var members by mutableStateOf<List<User>>(emptyList())
        private set
        
    var isLoading by mutableStateOf(false)
        private set
        
    var errorMessage by mutableStateOf<String?>(null)
        private set
        
    var navigateBack by mutableStateOf(false)
        private set
        
    val isCreator: Boolean
        get() = circle?.creatorId == auth.currentUser?.uid
        
    val isMember: Boolean
        get() = circle?.members?.contains(auth.currentUser?.uid) == true
    
    fun loadCircleDetails(circleId: String) {
        isLoading = true
        viewModelScope.launch {
            try {
                // Load circle details
                val circleResult = circleRepository.getCircleById(circleId)
                
                if (circleResult.isSuccess) {
                    circle = circleResult.getOrNull()
                    
                    // Load member details
                    loadMembers()
                } else {
                    errorMessage = circleResult.exceptionOrNull()?.message ?: "Failed to load circle"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "An unknown error occurred"
            } finally {
                isLoading = false
            }
        }
    }
    
    fun loadCircleSnaps(circleId: String) {
        isLoading = true
        viewModelScope.launch {
            try {
                println("DEBUG: Loading snaps for circle $circleId")
                
                // First, get the circle to verify membership
                val circleResult = circleRepository.getCircleById(circleId)
                if (circleResult.isFailure) {
                    val error = circleResult.exceptionOrNull()
                    errorMessage = error?.message ?: "Failed to load circle"
                    println("DEBUG: Failed to load circle: ${error?.message}")
                    isLoading = false
                    return@launch
                }
                
                val circle = circleResult.getOrThrow()
                val uid = auth.currentUser?.uid ?: return@launch
                
                // Log circle details for debugging
                println("DEBUG: Circle ID: ${circle.id}")
                println("DEBUG: Current user ID: $uid")
                println("DEBUG: Circle members: ${circle.members}")
                println("DEBUG: Circle creator: ${circle.creatorId}")
                println("DEBUG: Is user member? ${circle.members.contains(uid)}")
                println("DEBUG: Is user creator? ${circle.creatorId == uid}")
                
                if (!circle.members.contains(uid) && circle.creatorId != uid) {
                    errorMessage = "You are not a member of this circle"
                    println("DEBUG: User is not a member or creator of this circle")
                    isLoading = false
                    return@launch
                }
                
                // Use a different approach - get snaps directly by ID
                println("DEBUG: Getting snaps directly for circleId=$circleId")
                
                // First get the circle's snaps IDs
                val circleSnapsIds = mutableListOf<String>()
                
                try {
                    // Get all snaps where circleId matches without ordering
                    val snapsQuery = firestore.collection("snaps")
                        .whereEqualTo("circleId", circleId)
                        .get()
                        .await()
                    
                    println("DEBUG: Found ${snapsQuery.documents.size} snaps for circle")
                    
                    val circleSnaps = snapsQuery.documents.mapNotNull { doc ->
                        try {
                            val data = doc.data
                            if (data != null) {
                                val snapData = data.toMutableMap()
                                snapData["id"] = doc.id
                                Snap.fromMap(snapData)
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            println("DEBUG: Error processing snap: ${e.message}")
                            null
                        }
                    }.sortedByDescending { it.createdAt.seconds }
                    
                    println("DEBUG: Found ${circleSnaps.size} snaps for circle $circleId")
                    snaps = circleSnaps
                } catch (e: Exception) {
                    println("DEBUG: Error getting snaps: ${e.message}")
                    e.printStackTrace()
                    errorMessage = "Error loading snaps: ${e.message}"
                }
                
                // Load sender names if we have any snaps
                if (snaps.isNotEmpty()) {
                    loadSenderNames(snaps)
                }
                
                isLoading = false
                
            } catch (e: Exception) {
                errorMessage = "Error loading snaps: ${e.message}"
                println("DEBUG: Exception in loadCircleSnaps: ${e.message}")
                e.printStackTrace()
                isLoading = false
            }
        }
    }
    
    private fun loadSenderNames(snapsList: List<Snap>) {
        viewModelScope.launch {
            try {
                // Fetch sender names for snaps
                val senderIds = snapsList.map { it.sender }.distinct()
                val userDocs = mutableMapOf<String, Map<String, Any>?>()
                
                for (senderId in senderIds) {
                    val userDoc = firestore.collection("users").document(senderId).get().await()
                    userDocs[senderId] = userDoc.data
                }
                
                // Add sender names to snaps
                val snapsWithSenderNames = snapsList.map { snap ->
                    val senderData = userDocs[snap.sender]
                    val senderName = senderData?.get("username") as? String ?: senderData?.get("email") as? String
                    snap.copy(senderName = senderName)
                }
                
                // Update UI with snaps with names
                snaps = snapsWithSenderNames
            } catch (e: Exception) {
                println("Error loading sender names: ${e.message}")
            }
        }
    }
    
    // No need to override onCleared() since we're not using listeners anymore
    
    private suspend fun loadMembers() {
        try {
            val memberIds = circle?.members ?: return
            val usersList = mutableListOf<User>()
            
            for (memberId in memberIds) {
                val userDoc = firestore.collection("users").document(memberId).get().await()
                if (userDoc.exists()) {
                    val userData = userDoc.data
                    if (userData != null) {
                        val user = User(
                            id = memberId,
                            email = userData["email"] as? String,
                            username = userData["username"] as? String,
                            profilePictureUrl = userData["profilePictureUrl"] as? String
                        )
                        usersList.add(user)
                    }
                }
            }
            
            members = usersList
        } catch (e: Exception) {
            errorMessage = "Failed to load members: ${e.message}"
        }
    }
    
    fun leaveCircle() {
        viewModelScope.launch {
            try {
                val circleId = circle?.id ?: return@launch
                val result = circleRepository.leaveCircle(circleId)
                
                if (result.isFailure) {
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to leave circle"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "An unknown error occurred"
            }
        }
    }
    
    fun deleteCircle() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            
            try {
                val result = circleRepository.deleteCircle(circle?.id ?: return@launch)
                if (result.isSuccess) {
                    navigateBack = true
                } else {
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to delete circle"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unknown error occurred"
            } finally {
                isLoading = false
            }
        }
    }
    
    suspend fun inviteUserByEmail(email: String) {
        if (email.isBlank()) {
            errorMessage = "Email cannot be empty"
            return
        }
        
        try {
            // Find user by email
            val userQuery = firestore.collection("users")
                .whereEqualTo("email", email)
                .get()
                .await()
                
            if (userQuery.isEmpty) {
                errorMessage = "No user found with this email"
                return
            }
            
            val userDoc = userQuery.documents.first()
            val userId = userDoc.id
            val circleId = circle?.id ?: return
            
            // Check if user is already a member
            if (circle?.members?.contains(userId) == true) {
                errorMessage = "User is already a member of this circle"
                return
            }
            
            // Check if user is already invited
            if (circle?.pendingInvites?.contains(userId) == true) {
                errorMessage = "User has already been invited to this circle"
                return
            }
            
            // Invite the user
            val result = circleRepository.inviteUserToCircle(circleId, userId)
            
            if (result.isSuccess) {
                // Update local state
                circle = circle?.copy(
                    pendingInvites = (circle?.pendingInvites ?: emptyList()) + userId
                )
            } else {
                errorMessage = result.exceptionOrNull()?.message ?: "Failed to invite user"
            }
        } catch (e: Exception) {
            errorMessage = "Failed to invite user: ${e.message}"
        }
    }
    
    fun addMemberToCircle(userId: String) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            
            try {
                val result = circleRepository.addMemberToCircle(circle?.id ?: return@launch, userId)
                if (result.isSuccess) {
                    // Refresh circle data
                    loadCircleDetails(circle?.id ?: "")
                } else {
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to add member"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unknown error occurred"
            } finally {
                isLoading = false
            }
        }
    }
    
    fun clearError() {
        errorMessage = null
    }
}

class CircleDetailViewModelFactory(private val circleId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CircleDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CircleDetailViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 