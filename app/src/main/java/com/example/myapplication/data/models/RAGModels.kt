package com.example.myapplication.data.models

import com.google.firebase.Timestamp

/**
 * Models for RAG (Retrieval Augmented Generation) functionality
 */

data class OpenAIRequest(
    val model: String = "gpt-4",
    val messages: List<Message>,
    val max_tokens: Int = 150,
    val temperature: Double = 0.7
)

data class Message(
    val role: String,
    val content: Any // Can be either List<Content> for vision API or String for chat API
)

data class Content(
    val type: String,
    val text: String? = null,
    val image_url: ImageUrl? = null
)

data class ImageUrl(
    val url: String
)

data class OpenAIResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: ResponseMessage
)

data class ResponseMessage(
    val role: String,
    val content: String
)

/**
 * Models for image analysis
 */
data class ImageAnalysis(
    val objects: List<String> = emptyList(),
    val scenes: List<String> = emptyList(),
    val actions: List<String> = emptyList(),
    val emotions: List<String> = emptyList(),
    val dominantColors: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val confidence: Double = 0.0,
    val generatedAt: Timestamp = Timestamp.now()
)

/**
 * Models for RAG-specific functionality
 */

data class CircleContext(
    val circleId: String,
    val name: String,
    val description: String?,
    val createdAt: Timestamp,
    val memberCount: Int,
    val isPrivate: Boolean,
    val activityPatterns: ActivityPatterns,
    val contentClusters: List<ContentCluster>,
    val imageAnalytics: ImageAnalytics? = null
)

data class ImageAnalytics(
    val commonObjects: Map<String, Int> = emptyMap(), // Object -> frequency
    val commonScenes: Map<String, Int> = emptyMap(), // Scene -> frequency
    val commonEmotions: Map<String, Int> = emptyMap(), // Emotion -> frequency
    val commonActions: Map<String, Int> = emptyMap(), // Action -> frequency
    val dominantColors: Map<String, Int> = emptyMap(), // Color -> frequency
    val topTags: List<String> = emptyList(),
    val totalImagesAnalyzed: Int = 0,
    val lastUpdated: Timestamp = Timestamp.now()
)

data class ActivityPatterns(
    val totalSnaps: Int = 0,
    val averageSnapsPerDay: Double = 0.0,
    val mostActiveTimeOfDay: String = "",
    val mostActiveUsers: List<UserActivity> = emptyList(),
    val firstPoster: UserActivity? = null,
    val lastPoster: UserActivity? = null,
    val mostViewedSnapCount: Int = 0
)

data class UserActivity(
    val userId: String,
    val username: String,
    val totalSnaps: Int,
    val firstSnapAt: Timestamp? = null,
    val lastSnapAt: Timestamp? = null
)

data class ContentCluster(
    val timeframe: String,
    val snapCount: Int,
    val dominantMediaType: String,
    val commonThemes: List<String>,
    val topContributors: List<String>,
    val engagement: EngagementMetrics,
    val imageAnalysis: ImageAnalytics? = null
)

data class EngagementMetrics(
    val views: Int = 0,
    val uniqueViewers: Int = 0,
    val averageViewsPerSnap: Double = 0.0,
    val viewDuration: Long = 0, // in milliseconds
    val reactions: Map<String, Int> = emptyMap() // reaction -> count
)

/**
 * Represents a summary of a Circle's content
 */
data class CircleSummary(
    val circleId: String,
    val summary: String,
    val highlights: List<String>,
    val themes: List<String>,
    val topContributors: List<String>,
    val activityInsights: String,
    val generatedAt: Timestamp = Timestamp.now()
) 