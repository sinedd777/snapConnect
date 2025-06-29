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
                                text = "Generate a fun, engaging caption for this photo that would work well in a college social app. Keep it under 50 words and make it relatable to college students."
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
                Result.success(caption ?: "Great moment!")
            } else {
                Result.failure(Exception("Failed to generate caption: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun retrieveCircleContext(circleId: String): Result<CircleContext> {
        return try {
            // Get circle details
            val circleDoc = firestore.collection("circles").document(circleId).get().await()
            val circle = circleDoc.data?.let { Circle.fromMap(it) } ?: 
                return Result.failure(Exception("Circle not found"))
            
            // Get all snaps for the circle
            val snaps = firestore.collection("snaps")
                .whereEqualTo("circleId", circleId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    val data = doc.data
                    if (data != null) {
                        val snapData = data.toMutableMap()
                        snapData["id"] = doc.id
                        Snap.fromMap(snapData)
                    } else null
                }
            
            // Analyze activity patterns
            val activityPatterns = analyzeActivityPatterns(snaps)
            
            // Generate content clusters
            val contentClusters = generateContentClusters(snaps)
            
            Result.success(CircleContext(
                circleId = circleId,
                name = circle.name,
                description = circle.description,
                createdAt = circle.createdAt,
                memberCount = circle.members.size,
                isPrivate = circle.private,
                activityPatterns = activityPatterns,
                contentClusters = contentClusters
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun analyzeActivityPatterns(snaps: List<Snap>): ActivityPatterns {
        if (snaps.isEmpty()) {
            return ActivityPatterns()
        }

        // Sort snaps by creation time
        val sortedSnaps = snaps.sortedBy { it.createdAt.seconds }
        val firstSnap = sortedSnaps.first()
        val lastSnap = sortedSnaps.last()

        // Calculate total duration in days
        val durationInDays = (lastSnap.createdAt.seconds - firstSnap.createdAt.seconds) / (24.0 * 60 * 60)
        val averageSnapsPerDay = if (durationInDays > 0) snaps.size / durationInDays else snaps.size.toDouble()

        // Analyze snaps by user
        val snapsByUser = snaps.groupBy { it.sender }
        val userActivities = snapsByUser.map { (userId, userSnaps) ->
            val sortedUserSnaps = userSnaps.sortedBy { it.createdAt.seconds }
            UserActivity(
                userId = userId,
                username = sortedUserSnaps.firstOrNull()?.senderName ?: "Unknown User",
                totalSnaps = userSnaps.size,
                firstSnapAt = sortedUserSnaps.firstOrNull()?.createdAt,
                lastSnapAt = sortedUserSnaps.lastOrNull()?.createdAt
            )
        }.sortedByDescending { it.totalSnaps }

        // Find first and last posters
        val firstPoster = userActivities.minByOrNull { it.firstSnapAt?.seconds ?: Long.MAX_VALUE }
        val lastPoster = userActivities.maxByOrNull { it.lastSnapAt?.seconds ?: Long.MIN_VALUE }

        // Find most viewed snap
        val mostViewedCount = snaps.maxOfOrNull { it.viewedBy.size } ?: 0

        // Calculate most active time of day
        val snapsByHour = snaps.groupBy { snap ->
            val date = snap.createdAt.toDate()
            val calendar = Calendar.getInstance()
            calendar.time = date
            calendar.get(Calendar.HOUR_OF_DAY)
        }
        val mostActiveHour = snapsByHour.maxByOrNull { it.value.size }?.key ?: 0
        val timeOfDay = when (mostActiveHour) {
            in 5..11 -> "morning"
            in 12..16 -> "afternoon"
            in 17..20 -> "evening"
            else -> "night"
        }

        return ActivityPatterns(
            totalSnaps = snaps.size,
            averageSnapsPerDay = averageSnapsPerDay,
            mostActiveTimeOfDay = timeOfDay,
            mostActiveUsers = userActivities,
            firstPoster = firstPoster,
            lastPoster = lastPoster,
            mostViewedSnapCount = mostViewedCount
        )
    }
    
    private fun generateContentClusters(snaps: List<Snap>): List<ContentCluster> {
        // Group snaps by week
        val snapsByWeek = snaps.groupBy { snap ->
            val calendar = Calendar.getInstance()
            calendar.time = snap.createdAt.toDate()
            calendar.get(Calendar.WEEK_OF_YEAR).toString() + "-" + calendar.get(Calendar.YEAR)
        }
        
        return snapsByWeek.map { (week, weekSnaps) ->
            val mediaTypes = weekSnaps.groupBy { it.mediaType }
            val dominantType = mediaTypes.maxByOrNull { it.value.size }?.key ?: "unknown"
            
            val contributors = weekSnaps.groupBy { it.sender }
                .mapValues { it.value.size }
                .entries
                .sortedByDescending { it.value }
                .take(3)
                .map { weekSnaps.find { snap -> snap.sender == it.key }?.senderName ?: "Unknown" }
            
            ContentCluster(
                timeframe = "Week of ${weekSnaps.minByOrNull { it.createdAt.seconds }?.createdAt?.toDate()?.let { 
                    SimpleDateFormat("MMM d").format(it)
                }}",
                snapCount = weekSnaps.size,
                dominantMediaType = dominantType,
                commonThemes = listOf(), // This would require text analysis
                topContributors = contributors,
                engagement = calculateEngagement(weekSnaps)
            )
        }.sortedByDescending { it.snapCount }
    }
    
    private fun calculateEngagement(snaps: List<Snap>): EngagementMetrics {
        val totalViews = snaps.sumOf { it.viewedBy.size }
        val totalReactions = snaps.sumOf { it.screenshotBy.size }
        
        return EngagementMetrics(
            totalViews = totalViews,
            totalReactions = totalReactions,
            averageViewsPerSnap = if (snaps.isNotEmpty()) totalViews.toDouble() / snaps.size else 0.0,
            averageReactionsPerSnap = if (snaps.isNotEmpty()) totalReactions.toDouble() / snaps.size else 0.0
        )
    }
    
    override suspend fun analyzeContentPatterns(snaps: List<Snap>): Result<List<String>> {
        return try {
            val patterns = mutableListOf<String>()
            
            // Analyze temporal patterns
            val timePatterns = analyzeTemporalPatterns(snaps)
            patterns.addAll(timePatterns)
            
            // Analyze media type patterns
            val mediaPatterns = analyzeMediaPatterns(snaps)
            patterns.addAll(mediaPatterns)
            
            // Analyze user interaction patterns
            val interactionPatterns = analyzeInteractionPatterns(snaps)
            patterns.addAll(interactionPatterns)
            
            Result.success(patterns)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun findRelatedContent(snap: Snap): Result<List<Snap>> {
        return embeddingService.findSimilarSnaps(snap)
    }
    
    override suspend fun generateThematicSummary(context: CircleContext): Result<CircleSummary> {
        return try {
            // If there are no snaps, generate a basic summary based on circle info
            if (context.activityPatterns.totalSnaps == 0) {
                val basicSummary = CircleSummary(
                    circleId = context.circleId,
                    summary = "This is a ${if (context.isPrivate) "private" else "public"} circle called \"${context.name}\"" + 
                        (context.description?.let { " - $it" } ?: "") + 
                        ". Ready to start sharing memories with ${context.memberCount} member${if (context.memberCount != 1) "s" else ""}!",
                    highlights = listOf(
                        "Circle created and ready for content",
                        "Waiting for the first memory to be shared",
                        "${context.memberCount} member${if (context.memberCount != 1) "s have" else " has"} joined"
                    ),
                    themes = listOf("New Beginnings", "Anticipation"),
                    topContributors = emptyList(),
                    activityInsights = "Circle is ready for its first content. Be the first to share!",
                    generatedAt = Timestamp.now()
                )
                return Result.success(basicSummary)
            }

            // For circles with more than 10 snaps, add fun analytics
            val funAnalytics = if (context.activityPatterns.totalSnaps > 10) {
                val mostActiveUser = context.activityPatterns.mostActiveUsers.firstOrNull()
                val firstPoster = context.activityPatterns.firstPoster
                val lastPoster = context.activityPatterns.lastPoster
                
                buildString {
                    // First line - about snap engagement
                    append("Our star contributor posted ${mostActiveUser?.totalSnaps ?: 0} snaps")
                    append(" and the most viewed snap got ${context.activityPatterns.mostViewedSnapCount} views! ")
                    
                    // Second line - about participation pattern
                    append("From the first brave soul to the latest memory maker, ")
                    append("this circle has seen ${context.activityPatterns.totalSnaps} moments shared")
                    if (context.activityPatterns.averageSnapsPerDay > 0) {
                        append(" (that's about ${String.format("%.1f", context.activityPatterns.averageSnapsPerDay)} snaps per day)!")
                    } else {
                        append("!")
                    }
                }.toString()
            } else {
                null
            }

            val request = OpenAIRequest(
                model = "gpt-4",
                messages = listOf(
                    Message(
                        role = "system",
                        content = """You are analyzing a college social circle's content.
                            Focus on identifying meaningful patterns, themes, and social dynamics.
                            Consider the temporal relationships and engagement patterns."""
                    ),
                    Message(
                        role = "user",
                        content = buildThematicPrompt(context)
                    )
                )
            )
            
            val response = api.generateSummary(request)
            if (response.isSuccessful) {
                val content = response.body()?.choices?.firstOrNull()?.message?.content
                if (content != null) {
                    // Parse the structured response
                    val sections = content.split("\n\n")
                    val summary = sections.firstOrNull { it.startsWith("Summary:") }?.substringAfter("Summary:") ?: ""
                    val highlights = sections.firstOrNull { it.startsWith("Highlights:") }
                        ?.substringAfter("Highlights:")
                        ?.split("\n")
                        ?.filter { it.startsWith("-") }
                        ?.map { it.substringAfter("- ").trim() }
                        ?: emptyList()
                    val themes = sections.firstOrNull { it.startsWith("Themes:") }
                        ?.substringAfter("Themes:")
                        ?.split("\n")
                        ?.filter { it.startsWith("-") }
                        ?.map { it.substringAfter("- ").trim() }
                        ?: emptyList()
                    val insights = sections.firstOrNull { it.startsWith("Activity Insights:") }
                        ?.substringAfter("Activity Insights:") ?: ""
                    
                    // If we have fun analytics, append them to the summary
                    val finalSummary = if (funAnalytics != null) {
                        "$funAnalytics\n\n${summary.trim()}"
                    } else {
                        summary.trim()
                    }
                    
                    Result.success(CircleSummary(
                        circleId = context.circleId,
                        summary = finalSummary,
                        highlights = highlights,
                        themes = themes,
                        topContributors = context.activityPatterns.mostActiveUsers.map { it.username },
                        activityInsights = insights.trim(),
                        generatedAt = Timestamp.now()
                    ))
                } else {
                    Result.failure(Exception("Failed to parse summary response"))
                }
            } else {
                Result.failure(Exception("Failed to generate summary: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun buildThematicPrompt(context: CircleContext): String {
        return buildString {
            appendLine("Generate a thematic summary for this college social circle:")
            appendLine()
            appendLine("Circle Info:")
            appendLine("- Name: ${context.name}")
            appendLine("- Members: ${context.memberCount}")
            appendLine("- Privacy: ${if (context.isPrivate) "Private" else "Public"}")
            context.description?.let { appendLine("- Description: $it") }
            appendLine()
            
            appendLine("Activity Patterns:")
            appendLine("- Total Snaps: ${context.activityPatterns.totalSnaps}")
            appendLine("- Average Daily Activity: ${String.format("%.1f", context.activityPatterns.averageSnapsPerDay)} snaps")
            appendLine("- Peak Activity: ${context.activityPatterns.mostActiveTimeOfDay}")
            appendLine("- Most Active Users:")
            context.activityPatterns.mostActiveUsers.forEach { user ->
                appendLine("  * ${user.username}: ${user.totalSnaps} snaps")
            }
            appendLine()
            
            appendLine("Content Clusters:")
            context.contentClusters.forEach { cluster ->
                appendLine("- ${cluster.timeframe}:")
                appendLine("  * ${cluster.snapCount} snaps")
                appendLine("  * Main type: ${cluster.dominantMediaType}")
                appendLine("  * Top contributors: ${cluster.topContributors.joinToString(", ")}")
                appendLine("  * Engagement: ${cluster.engagement.totalViews} views, ${cluster.engagement.totalReactions} reactions")
            }
            appendLine()
            
            appendLine("Please provide a structured analysis with the following sections:")
            appendLine("1. Summary: A 2-3 sentence overview of the circle's activity and vibe")
            appendLine("2. Highlights: 3-5 key moments or patterns")
            appendLine("3. Themes: 2-3 recurring themes or trends")
            appendLine("4. Activity Insights: Brief analysis of engagement patterns and user dynamics")
        }
    }
    
    private fun analyzeTemporalPatterns(snaps: List<Snap>): List<String> {
        val patterns = mutableListOf<String>()
        
        // Analyze daily patterns
        val snapsByHour = snaps.groupBy { snap ->
            snap.createdAt.toDate().hours
        }
        val peakHours = snapsByHour.entries
            .sortedByDescending { it.value.size }
            .take(2)
            .map { entry ->
                when (entry.key) {
                    in 5..11 -> "morning"
                    in 12..16 -> "afternoon"
                    in 17..20 -> "evening"
                    else -> "night"
                }
            }
        patterns.add("Most active during ${peakHours.joinToString(" and ")}")
        
        // Analyze weekly patterns
        val snapsByDay = snaps.groupBy { snap ->
            snap.createdAt.toDate().let { date ->
                val calendar = Calendar.getInstance()
                calendar.time = date
                calendar.get(Calendar.DAY_OF_WEEK)
            }
        }
        val peakDays = snapsByDay.entries
            .sortedByDescending { it.value.size }
            .take(2)
            .map { entry ->
                when (entry.key) {
                    Calendar.MONDAY -> "Mondays"
                    Calendar.TUESDAY -> "Tuesdays"
                    Calendar.WEDNESDAY -> "Wednesdays"
                    Calendar.THURSDAY -> "Thursdays"
                    Calendar.FRIDAY -> "Fridays"
                    Calendar.SATURDAY -> "Saturdays"
                    else -> "Sundays"
                }
            }
        patterns.add("Most active on ${peakDays.joinToString(" and ")}")
        
        return patterns
    }
    
    private fun analyzeMediaPatterns(snaps: List<Snap>): List<String> {
        val patterns = mutableListOf<String>()
        
        // Analyze media type distribution
        val mediaTypes = snaps.groupBy { it.mediaType }
        val dominantType = mediaTypes.maxByOrNull { it.value.size }
        dominantType?.let {
            patterns.add("Primarily shares ${it.key} content (${(it.value.size * 100.0 / snaps.size).toInt()}%)")
        }
        
        return patterns
    }
    
    private fun analyzeInteractionPatterns(snaps: List<Snap>): List<String> {
        val patterns = mutableListOf<String>()
        
        // Analyze user engagement
        val totalViews = snaps.sumOf { it.viewedBy.size }
        val totalReactions = snaps.sumOf { it.screenshotBy.size }
        val averageViews = if (snaps.isNotEmpty()) totalViews.toDouble() / snaps.size else 0.0
        val averageReactions = if (snaps.isNotEmpty()) totalReactions.toDouble() / snaps.size else 0.0
        
        patterns.add("Average engagement: ${String.format("%.1f", averageViews)} views and ${String.format("%.1f", averageReactions)} reactions per snap")
        
        return patterns
    }
    
    private fun calculateAverageSnapsPerDay(snaps: List<Snap>): Double {
        if (snaps.isEmpty()) return 0.0
        val firstSnap = snaps.minByOrNull { it.createdAt.seconds }?.createdAt?.toDate() ?: return 0.0
        val lastSnap = snaps.maxByOrNull { it.createdAt.seconds }?.createdAt?.toDate() ?: return 0.0
        val daysDiff = ((lastSnap.time - firstSnap.time) / (1000 * 60 * 60 * 24)).toDouble()
        return if (daysDiff > 0) snaps.size / daysDiff else snaps.size.toDouble()
    }
    
    private fun findMostActiveTimeOfDay(snaps: List<Snap>): String {
        val hourCounts = snaps.groupBy { snap ->
            snap.createdAt.toDate().hours
        }
        return hourCounts.maxByOrNull { it.value.size }?.key?.let { hour ->
            when {
                hour < 12 -> "Morning"
                hour < 17 -> "Afternoon"
                hour < 21 -> "Evening"
                else -> "Night"
            }
        } ?: "Various times"
    }
    
    override suspend fun summarizeCircleContent(snaps: List<Snap>): Result<String> {
        return try {
            // Get circle context
            val circleId = snaps.firstOrNull()?.circleId ?: return Result.failure(Exception("No snaps provided"))
            val contextResult = retrieveCircleContext(circleId)
            
            if (contextResult.isSuccess) {
                val context = contextResult.getOrNull()!!
                val summaryResult = generateThematicSummary(context)
                
                if (summaryResult.isSuccess) {
                    Result.success(summaryResult.getOrNull()?.summary ?: "Great times were had!")
                } else {
                    Result.failure(summaryResult.exceptionOrNull() ?: Exception("Failed to generate summary"))
                }
            } else {
                Result.failure(contextResult.exceptionOrNull() ?: Exception("Failed to retrieve context"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun generateCircleHighlights(circleId: String): Result<List<String>> {
        return try {
            val contextResult = retrieveCircleContext(circleId)
            
            if (contextResult.isSuccess) {
                val context = contextResult.getOrNull()!!
                val summaryResult = generateThematicSummary(context)
                
                if (summaryResult.isSuccess) {
                    Result.success(summaryResult.getOrNull()?.highlights ?: listOf("Fun times!", "Great memories!", "Epic moments!"))
                } else {
                    Result.failure(summaryResult.exceptionOrNull() ?: Exception("Failed to generate highlights"))
                }
            } else {
                Result.failure(contextResult.exceptionOrNull() ?: Exception("Failed to retrieve context"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun generateCompleteSummary(circleId: String): Result<CircleSummary> {
        return try {
            val contextResult = retrieveCircleContext(circleId)
            
            if (contextResult.isSuccess) {
                val context = contextResult.getOrNull()!!
                generateThematicSummary(context)
            } else {
                Result.failure(contextResult.exceptionOrNull() ?: Exception("Failed to retrieve context"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 