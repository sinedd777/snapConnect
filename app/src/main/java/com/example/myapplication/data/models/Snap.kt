package com.example.myapplication.data.models

import com.google.firebase.Timestamp

data class Snap(
    val id: String,
    val sender: String,
    val senderName: String? = null,
    val recipients: List<String>,
    val circleId: String? = null,
    val mediaUrl: String,
    val mediaType: String,
    val createdAt: Timestamp,
    val expiresAt: Timestamp? = null,
    val viewedBy: List<String> = emptyList(),
    val screenshotBy: List<String> = emptyList(),
    val caption: String? = null,
    val isCircleContent: Boolean = false,
    // RAG-related fields
    val ragGeneratedCaption: String? = null,
    val ragCaptionGenerated: Boolean = false,
    val ragCaptionGeneratedAt: Timestamp? = null,
    val ragTags: List<String> = emptyList(),
    val imageAnalysis: ImageAnalysis? = null
) {
    val isViewed: Boolean
        get() = viewedBy.isNotEmpty()
        
    val isScreenshotted: Boolean
        get() = screenshotBy.isNotEmpty()
        
    companion object {
        fun fromMap(map: Map<String, Any>): Snap {
            // Ensure required fields exist and are of the correct type
            val id = map["id"] as? String 
                ?: throw IllegalArgumentException("Missing or invalid 'id' field")
                
            val sender = map["sender"] as? String 
                ?: throw IllegalArgumentException("Missing or invalid 'sender' field")
                
            val recipients = map["recipients"] as? List<*>
                ?: throw IllegalArgumentException("Missing or invalid 'recipients' field")
                
            val mediaUrl = map["mediaUrl"] as? String
                ?: throw IllegalArgumentException("Missing or invalid 'mediaUrl' field")
                
            val mediaType = map["mediaType"] as? String
                ?: throw IllegalArgumentException("Missing or invalid 'mediaType' field")
                
            val createdAt = map["createdAt"] as? Timestamp
                ?: throw IllegalArgumentException("Missing or invalid 'createdAt' field")
            
            // Convert recipients to List<String>, filtering out any non-string values
            val recipientsList = recipients.filterIsInstance<String>()
            
            // Parse image analysis if it exists
            val imageAnalysis = (map["imageAnalysis"] as? Map<String, Any>)?.let { analysisMap ->
                ImageAnalysis(
                    objects = (analysisMap["objects"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    scenes = (analysisMap["scenes"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    actions = (analysisMap["actions"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    emotions = (analysisMap["emotions"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    dominantColors = (analysisMap["dominantColors"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    tags = (analysisMap["tags"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    confidence = (analysisMap["confidence"] as? Double) ?: 0.0,
                    generatedAt = analysisMap["generatedAt"] as? Timestamp ?: Timestamp.now()
                )
            }
            
            return Snap(
                id = id,
                sender = sender,
                senderName = map["senderName"] as? String,
                recipients = recipientsList,
                circleId = map["circleId"] as? String,
                mediaUrl = mediaUrl,
                mediaType = mediaType,
                createdAt = createdAt,
                expiresAt = map["expiresAt"] as? Timestamp,
                viewedBy = (map["viewedBy"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                screenshotBy = (map["screenshotBy"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                caption = map["caption"] as? String,
                isCircleContent = (map["isCircleContent"] as? Boolean) ?: false,
                // RAG-related fields
                ragGeneratedCaption = map["ragGeneratedCaption"] as? String,
                ragCaptionGenerated = (map["ragCaptionGenerated"] as? Boolean) ?: false,
                ragCaptionGeneratedAt = map["ragCaptionGeneratedAt"] as? Timestamp,
                ragTags = (map["ragTags"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                imageAnalysis = imageAnalysis
            )
        }
    }
} 