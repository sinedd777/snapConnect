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
    val contentClusters: List<ContentCluster>
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
    val engagement: EngagementMetrics
)

data class EngagementMetrics(
    val totalViews: Int,
    val totalReactions: Int,
    val averageViewsPerSnap: Double,
    val averageReactionsPerSnap: Double
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