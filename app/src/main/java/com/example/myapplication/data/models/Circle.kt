package com.example.myapplication.data.models

import com.google.firebase.Timestamp
import java.util.UUID

/**
 * Represents a Circle, which is a group space for sharing ephemeral content
 */
data class Circle(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String? = null,
    val creatorId: String,
    val members: List<String> = emptyList(),
    val pendingInvites: List<String> = emptyList(),
    val createdAt: Timestamp = Timestamp.now(),
    val expiresAt: Timestamp? = null,
    val locationEnabled: Boolean = false,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val locationRadius: Double? = null,
    val isPrivate: Boolean = true
) {
    companion object {
        fun fromMap(map: Map<String, Any>): Circle {
            return Circle(
                id = map["id"] as String,
                name = map["name"] as String,
                description = map["description"] as? String,
                creatorId = map["creatorId"] as String,
                members = (map["members"] as? List<String>) ?: emptyList(),
                pendingInvites = (map["pendingInvites"] as? List<String>) ?: emptyList(),
                createdAt = map["createdAt"] as Timestamp,
                expiresAt = map["expiresAt"] as? Timestamp,
                locationEnabled = (map["locationEnabled"] as? Boolean) ?: false,
                locationLat = map["locationLat"] as? Double,
                locationLng = map["locationLng"] as? Double,
                locationRadius = map["locationRadius"] as? Double,
                isPrivate = (map["isPrivate"] as? Boolean) ?: true
            )
        }
    }
} 