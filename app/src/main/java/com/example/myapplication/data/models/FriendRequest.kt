package com.example.myapplication.data.models

import com.google.firebase.Timestamp

data class FriendRequest(
    val id: String,
    val userA: String,
    val userB: String,
    val status: String,
    val createdAt: Timestamp,
    val requesterDetails: User? = null
) {
    companion object {
        fun fromMap(map: Map<String, Any>): FriendRequest {
            val requesterDetails = (map["requesterDetails"] as? Map<String, Any>)?.let {
                User.fromMap(it)
            }
            
            return FriendRequest(
                id = map["id"] as String,
                userA = map["userA"] as String,
                userB = map["userB"] as String,
                status = map["status"] as String,
                createdAt = map["createdAt"] as Timestamp,
                requesterDetails = requesterDetails
            )
        }
    }
} 