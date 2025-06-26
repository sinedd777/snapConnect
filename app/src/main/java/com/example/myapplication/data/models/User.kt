package com.example.myapplication.data.models

import com.google.firebase.Timestamp

data class User(
    val id: String,
    val email: String? = null,
    val username: String? = null,
    val profilePictureUrl: String? = null,
    val createdAt: Timestamp? = null
) {
    companion object {
        fun fromMap(map: Map<String, Any>): User {
            return User(
                id = map["id"] as? String ?: map["uid"] as String,
                email = map["email"] as? String,
                username = map["username"] as? String,
                profilePictureUrl = map["profilePictureUrl"] as? String ?: map["avatarUrl"] as? String,
                createdAt = map["createdAt"] as? Timestamp
            )
        }
    }
} 