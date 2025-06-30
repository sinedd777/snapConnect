package com.example.myapplication.data.services

import com.example.myapplication.data.models.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.myapplication.BuildConfig
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class OpenAIRAGService : RAGService {
    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.openai.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    private val api = retrofit.create(OpenAIApi::class.java)
    private val firestore = FirebaseFirestore.getInstance()
    private val embeddingService = EmbeddingService()
    
    override suspend fun generateImageCaption(imageBase64: String): Result<String> {
        return try {
            val request = OpenAIRequest(
                model = "gpt-4-vision-preview",
                messages = listOf(
                    Message(
                        role = "user",
                        content = listOf(
                            Content(
                                type = "text",
                                text = "Generate a natural, engaging caption for this image. Keep it concise but descriptive."
                            ),
                            Content(
                                type = "image_url",
                                image_url = ImageUrl("data:image/jpeg;base64,$imageBase64")
                            )
                        )
                    )
                )
            )
            
            val response = api.generateCaption(request)
            if (response.isSuccessful) {
                val caption = response.body()?.choices?.firstOrNull()?.message?.content
                if (caption != null) {
                    Result.success(caption.trim())
                } else {
                    Result.failure(Exception("Failed to generate caption"))
                }
            } else {
                Result.failure(Exception("Failed to generate caption: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Analyzes an image using GPT-4 Vision API to extract objects, scenes, emotions, and actions
     */
    override suspend fun analyzeImage(imageBase64: String): Result<ImageAnalysis> {
        return try {
            val request = OpenAIRequest(
                model = "gpt-4-vision-preview",
                messages = listOf(
                    Message(
                        role = "user",
                        content = listOf(
                            Content(
                                type = "text",
                                text = """Analyze this image and provide a structured analysis in the following format:
                                    |Objects: List the main objects in the image (comma-separated)
                                    |Scene: Describe the scene/setting (comma-separated keywords)
                                    |Actions: List any actions being performed (comma-separated)
                                    |Emotions: List any emotions displayed (comma-separated)
                                    |Colors: List the dominant colors (comma-separated)
                                    |Tags: Provide relevant tags for categorization (comma-separated)""".trimMargin()
                            ),
                            Content(
                                type = "image_url",
                                image_url = ImageUrl("data:image/jpeg;base64,$imageBase64")
                            )
                        )
                    )
                )
            )
            
            val response = api.analyzeImage(request)
            if (response.isSuccessful) {
                val content = response.body()?.choices?.firstOrNull()?.message?.content
                if (content != null) {
                    // Parse the structured response using extension function
                    val objects = content.extractLine("Objects:") ?: emptyList()
                    val scenes = content.extractLine("Scene:") ?: emptyList()
                    val actions = content.extractLine("Actions:") ?: emptyList()
                    val emotions = content.extractLine("Emotions:") ?: emptyList()
                    val colors = content.extractLine("Colors:") ?: emptyList()
                    val tags = content.extractLine("Tags:") ?: emptyList()
                    
                    Result.success(ImageAnalysis(
                        objects = objects,
                        scenes = scenes,
                        actions = actions,
                        emotions = emotions,
                        dominantColors = colors,
                        tags = tags,
                        confidence = 0.9, // GPT-4 Vision is generally highly accurate
                        generatedAt = Timestamp.now()
                    ))
                } else {
                    Result.failure(Exception("Failed to parse image analysis response"))
                }
            } else {
                Result.failure(Exception("Failed to analyze image: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun String.extractLine(prefix: String): List<String>? {
        return lines()
            .find { it.trim().startsWith(prefix) }
            ?.substringAfter(prefix)
            ?.trim()
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
    }
    
    override suspend fun summarizeCircleContent(snaps: List<Snap>): Result<String> {
        // Get circle info to make a friendly message
        val circle = snaps.firstOrNull()?.circleId?.let { circleId ->
            firestore.collection("circles").document(circleId).get().await()
        }
        
        val circleName = circle?.get("name") as? String ?: "this circle"
        val circleDescription = circle?.get("description") as? String

        // If no snaps or minimal content, just make a fun one-liner about the circle
        if (snaps.isEmpty() || snaps.all { it.caption == null && it.ragGeneratedCaption == null }) {
            val request = OpenAIRequest(
                model = "gpt-4",
                messages = listOf(
                    Message(
                        role = "user",
                        content = """Write a short, natural tagline for an event/group named "$circleName"${if (circleDescription != null) " (${circleDescription})" else ""}. 
                            |Guidelines:
                            |- Keep it under 10 words
                            |- Make it sound like a natural event/group tagline
                            |- No personification of the circle
                            |- No mentions of content/data
                            |- Add an appropriate emoji at the start
                            |
                            |Examples:
                            |- 🎭 Where drama meets destiny on stage!
                            |- 🎨 Bringing color to campus, one brush at a time
                            |- 🏃 Sprint, score, celebrate - that's how we roll""".trimMargin()
                    )
                )
            )
            
            val response = api.generateSummary(request)
            return if (response.isSuccessful) {
                val summary = response.body()?.choices?.firstOrNull()?.message?.content
                if (summary != null) {
                    Result.success(summary.trim())
                } else {
                    Result.success("⭐ Where moments become memories!")
                }
            } else {
                Result.success("⭐ Where moments become memories!")
            }
        }

        // For circles with actual content
        return try {
            val imageContext = if (snaps.any { it.imageAnalysis != null }) {
                val analyses = snaps.mapNotNull { it.imageAnalysis }
                val commonEmotions = analyses
                    .flatMap { it.emotions }
                    .groupingBy { it }
                    .eachCount()
                    .maxByOrNull { it.value }
                    ?.key
                
                val commonScenes = analyses
                    .flatMap { it.scenes }
                    .groupingBy { it }
                    .eachCount()
                    .maxByOrNull { it.value }
                    ?.key

                if (commonEmotions != null || commonScenes != null) {
                    "Vibe: ${listOfNotNull(commonEmotions, commonScenes).joinToString(" in ")}"
                } else ""
            } else ""

            val request = OpenAIRequest(
                model = "gpt-4",
                messages = listOf(
                    Message(
                        role = "user",
                        content = """Write a short, natural tagline for this group/event:
                            |
                            |Name: $circleName
                            |${if (circleDescription != null) "Description: $circleDescription\n" else ""}
                            |${if (imageContext.isNotEmpty()) "$imageContext\n" else ""}
                            |
                            |Guidelines:
                            |- Keep it under 10 words
                            |- Make it sound like a natural event tagline
                            |- No personification
                            |- Add an appropriate emoji at the start
                            |- Focus on the event/group theme
                            |
                            |Examples:
                            |- 🎭 Where drama meets destiny on stage!
                            |- 🎨 Bringing color to campus, one brush at a time
                            |- 🏃 Sprint, score, celebrate - that's how we roll""".trimMargin()
                    )
                )
            )
            
            val response = api.generateSummary(request)
            if (response.isSuccessful) {
                val summary = response.body()?.choices?.firstOrNull()?.message?.content
                if (summary != null) {
                    Result.success(summary.trim())
                } else {
                    Result.success("⚡ ${circleName}: Where the action happens!")
                }
            } else {
                Result.success("⚡ ${circleName}: Where the action happens!")
            }
        } catch (e: Exception) {
            Result.success("⚡ ${circleName}: Where the action happens!")
        }
    }
    
    override suspend fun generateCircleHighlights(circleId: String): Result<List<String>> {
        return try {
            val snaps = getSnapsForCircle(circleId)
            
            // If no snaps, return empty list
            if (snaps.isEmpty()) {
                return Result.success(emptyList())
            }
            
            val highlights = mutableListOf<String>()
            
            // Most viewed content
            val mostViewed = snaps.maxByOrNull { it.viewedBy.size }
            if (mostViewed != null) {
                val caption = mostViewed.ragGeneratedCaption ?: mostViewed.caption ?: "No caption"
                val senderName = mostViewed.senderName ?: "Unknown"
                highlights.add("Most viewed: $caption by $senderName (${mostViewed.viewedBy.size} views)")
            }
            
            // Most screenshot content
            val mostScreenshot = snaps.maxByOrNull { it.screenshotBy.size }
            if (mostScreenshot != null && mostScreenshot.screenshotBy.isNotEmpty()) {
                val caption = mostScreenshot.ragGeneratedCaption ?: mostScreenshot.caption ?: "No caption"
                val senderName = mostScreenshot.senderName ?: "Unknown"
                highlights.add("Most saved: $caption by $senderName (${mostScreenshot.screenshotBy.size} saves)")
            }
            
            // Most active contributors
            val topContributors = snaps
                .groupBy { it.sender }
                .entries
                .sortedByDescending { (_, snaps) -> snaps.size }
                .take(3)
                .mapNotNull { (_, snaps) -> snaps.firstOrNull()?.senderName }
                .filter { it != "Unknown" }
            
            if (topContributors.isNotEmpty()) {
                highlights.add("Top contributors: ${topContributors.joinToString(", ")}")
            }
            
            Result.success(highlights)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun generateCompleteSummary(circleId: String): Result<CircleSummary> {
        return try {
            val snaps = getSnapsForCircle(circleId)
            
            // Get basic summary
            val summary = summarizeCircleContent(snaps).getOrNull() ?: ""
            
            // Get highlights
            val highlights = generateCircleHighlights(circleId).getOrNull() ?: emptyList()
            
            // Extract common themes from RAG tags
            val themes = snaps
                .flatMap { it.ragTags }
                .groupingBy { it }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .take(5)
                .map { it.key }
            
            // Get top contributors
            val topContributors = snaps
                .groupBy { it.sender }
                .entries
                .sortedByDescending { (_, snaps) -> snaps.size }
                .take(3)
                .mapNotNull { (_, snaps) -> snaps.firstOrNull()?.senderName }
                .filter { it != null }
            
            // Generate activity insights
            val activityInsights = buildString {
                append("Activity Overview:\n")
                append("- Total snaps: ${snaps.size}\n")
                append("- Total views: ${snaps.sumOf { it.viewedBy.size }}\n")
                append("- Total saves: ${snaps.sumOf { it.screenshotBy.size }}\n")
                append("- Unique contributors: ${snaps.map { it.sender }.distinct().size}")
            }
            
            Result.success(CircleSummary(
                circleId = circleId,
                summary = summary,
                highlights = highlights,
                themes = themes,
                topContributors = topContributors,
                activityInsights = activityInsights,
                generatedAt = Timestamp.now()
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun analyzeContentPatterns(snaps: List<Snap>): Result<List<String>> {
        return try {
            val patterns = mutableListOf<String>()
            
            // Analyze temporal patterns
            val timePatterns = snaps.groupBy { snap ->
                val date = snap.createdAt.toDate()
                val calendar = Calendar.getInstance()
                calendar.time = date
                calendar.get(Calendar.HOUR_OF_DAY)
            }
            
            val mostActiveHour = timePatterns.maxByOrNull { it.value.size }
            if (mostActiveHour != null) {
                patterns.add("Most active time: ${mostActiveHour.key}:00 with ${mostActiveHour.value.size} snaps")
            }
            
            // Analyze media patterns
            val mediaTypes = snaps.groupBy { snap: Snap -> snap.mediaType }
            val dominantMedia = mediaTypes.maxByOrNull { it.value.size }
            if (dominantMedia != null) {
                patterns.add("Dominant media type: ${dominantMedia.key} (${dominantMedia.value.size} snaps)")
            }
            
            // Analyze engagement patterns
            val avgViews = snaps.map { snap: Snap -> snap.viewedBy.size }.average()
            patterns.add("Average views per snap: %.1f".format(avgViews))
            
            // Analyze tag patterns
            val commonTags = snaps
                .flatMap { snap: Snap -> snap.ragTags }
                .groupingBy { tag: String -> tag }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .take(3)
                .map { "${it.key} (${it.value} times)" }
            
            if (commonTags.isNotEmpty()) {
                patterns.add("Common themes: ${commonTags.joinToString(", ")}")
            }
            
            Result.success(patterns)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun findRelatedContent(snap: Snap): Result<List<Snap>> {
        return try {
            // Find snaps with similar tags
            val similarSnaps = getSnapsForCircle(snap.circleId ?: return Result.success(emptyList()))
                .filter { otherSnap: Snap -> otherSnap.id != snap.id } // Exclude the current snap
                .map { otherSnap: Snap ->
                    val commonTags = snap.ragTags.intersect(otherSnap.ragTags)
                    Pair(otherSnap, commonTags.size)
                }
                .filter { (_, commonTagCount): Pair<Snap, Int> -> commonTagCount > 0 } // Only include snaps with at least one common tag
                .sortedByDescending { (_, commonTagCount): Pair<Snap, Int> -> commonTagCount }
                .take(5)
                .map { (otherSnap, _): Pair<Snap, Int> -> otherSnap }
            
            Result.success(similarSnaps)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getSnapsForCircle(circleId: String): List<Snap> {
        return try {
            val snapshots = firestore.collection("snaps")
                .whereEqualTo("circleId", circleId)
                .get()
                .await()
            
            val snaps = snapshots.documents.mapNotNull { doc ->
                val data = doc.data
                if (data != null) {
                    val snapData = data.toMutableMap()
                    snapData["id"] = doc.id
                    
                    // Get sender name
                    val senderId = data["sender"] as? String
                    if (senderId != null) {
                        val userDoc = firestore.collection("users").document(senderId).get().await()
                        val username = userDoc.get("username") as? String
                        val email = userDoc.get("email") as? String
                        snapData["senderName"] = username ?: email ?: "Unknown"
                    }
                    
                    Snap.fromMap(snapData)
                } else null
            }
            
            snaps
        } catch (e: Exception) {
            emptyList()
        }
    }
} 