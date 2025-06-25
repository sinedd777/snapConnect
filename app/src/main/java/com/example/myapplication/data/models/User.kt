package com.example.myapplication.data.models

import com.google.firebase.Timestamp

data class User(
    val uid: String,
    val email: String,
    val username: String? = null,
    val avatarUrl: String? = null,
    val createdAt: Timestamp? = null
) {
    companion object {
        fun fromMap(map: Map<String, Any>): User {
            return User(
                uid = map["uid"] as String,
                email = map["email"] as String,
                username = map["username"] as? String,
                avatarUrl = map["avatarUrl"] as? String,
                createdAt = map["createdAt"] as? Timestamp
            )
        }
    }
} 