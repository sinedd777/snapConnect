package com.example.myapplication.data.repositories

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.myapplication.data.models.*
import com.example.myapplication.data.services.EmbeddingService
import com.example.myapplication.data.services.OpenAIRAGService
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.tasks.await

/**
 * Repository for handling RAG (Retrieval Augmented Generation) operations
 */
class RAGRepository {
    private val ragService = OpenAIRAGService()
    private val embeddingService = EmbeddingService()
    private val firestore = FirebaseFirestore.getInstance()
    
    /**
     * Generates and stores a caption for a snap using RAG, including image analysis
     */
    suspend fun generateAndStoreSnapCaption(
        snap: Snap,
        imageUri: Uri,
        context: Context
    ): Result<String> {
        return try {
            // Convert image to base64
            val imageBase64 = convertImageToBase64(imageUri, context)
            
            // First analyze the image
            val imageAnalysis = ragService.analyzeImage(imageBase64).getOrNull()
            
            // Generate caption
            val captionResult = ragService.generateImageCaption(imageBase64)
            
            if (captionResult.isSuccess) {
                // Update snap with RAG caption and image analysis
                val caption = captionResult.getOrNull()
                val snapRef = firestore.collection("snaps").document(snap.id)
                
                val updates = mutableMapOf<String, Any>(
                    "ragGeneratedCaption" to (caption ?: ""),
                    "ragCaptionGenerated" to true,
                    "ragCaptionGeneratedAt" to Timestamp.now()
                )
                
                // Add image analysis tags if available
                if (imageAnalysis != null) {
                    val tags = mutableListOf<String>()
                    
                    // Add prefixed tags for better categorization
                    imageAnalysis.objects.forEach { tags.add("object:$it") }
                    imageAnalysis.scenes.forEach { tags.add("scene:$it") }
                    imageAnalysis.emotions.forEach { tags.add("emotion:$it") }
                    imageAnalysis.actions.forEach { tags.add("action:$it") }
                    imageAnalysis.dominantColors.forEach { tags.add("color:$it") }
                    tags.addAll(imageAnalysis.tags)
                    
                    updates["ragTags"] = tags
                    updates["imageAnalysis"] = imageAnalysis
                }
                
                snapRef.update(updates).await()
                
                // Update circle analytics if this is a circle snap
                snap.circleId?.let { circleId ->
                    updateCircleImageAnalytics(circleId, imageAnalysis)
                }
                
                Result.success(caption ?: "")
            } else {
                captionResult
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Updates the circle's image analytics with new image analysis data
     */
    private suspend fun updateCircleImageAnalytics(circleId: String, imageAnalysis: ImageAnalysis?) {
        if (imageAnalysis == null) return
        
        try {
            val circleRef = firestore.collection("circles").document(circleId)
            val circleDoc = circleRef.get().await()
            
            if (!circleDoc.exists()) return
            
            // Get existing analytics or create new ones
            val existingAnalytics = circleDoc.get("imageAnalytics") as? Map<String, Any>
            val currentAnalytics = if (existingAnalytics != null) {
                ImageAnalytics(
                    commonObjects = (existingAnalytics["commonObjects"] as? Map<String, Int>) ?: emptyMap(),
                    commonScenes = (existingAnalytics["commonScenes"] as? Map<String, Int>) ?: emptyMap(),
                    commonEmotions = (existingAnalytics["commonEmotions"] as? Map<String, Int>) ?: emptyMap(),
                    commonActions = (existingAnalytics["commonActions"] as? Map<String, Int>) ?: emptyMap(),
                    dominantColors = (existingAnalytics["dominantColors"] as? Map<String, Int>) ?: emptyMap(),
                    topTags = (existingAnalytics["topTags"] as? List<String>) ?: emptyList(),
                    totalImagesAnalyzed = (existingAnalytics["totalImagesAnalyzed"] as? Int) ?: 0
                )
            } else {
                ImageAnalytics()
            }
            
            // Update frequency maps with proper type inference
            val updatedAnalytics = ImageAnalytics(
                commonObjects = updateFrequencyMap(currentAnalytics.commonObjects, imageAnalysis.objects),
                commonScenes = updateFrequencyMap(currentAnalytics.commonScenes, imageAnalysis.scenes),
                commonEmotions = updateFrequencyMap(currentAnalytics.commonEmotions, imageAnalysis.emotions),
                commonActions = updateFrequencyMap(currentAnalytics.commonActions, imageAnalysis.actions),
                dominantColors = updateFrequencyMap(currentAnalytics.dominantColors, imageAnalysis.dominantColors),
                topTags = imageAnalysis.tags.groupingBy { it }.eachCount()
                    .entries.sortedByDescending { it.value }
                    .take(20)
                    .map { it.key },
                totalImagesAnalyzed = currentAnalytics.totalImagesAnalyzed + 1,
                lastUpdated = Timestamp.now()
            )
            
            // Update circle document
            circleRef.update("imageAnalytics", updatedAnalytics).await()
        } catch (e: Exception) {
            println("Failed to update circle image analytics: ${e.message}")
        }
    }
    
    /**
     * Updates a frequency map with new items
     */
    private fun updateFrequencyMap(current: Map<String, Int>, new: List<String>): Map<String, Int> {
        val mutableMap = current.toMutableMap()
        new.forEach { item -> 
            mutableMap[item] = (mutableMap[item] ?: 0) + 1
        }
        return mutableMap
    }
    
    /**
     * Generates and stores a summary for a circle using RAG
     */
    suspend fun generateAndStoreCircleSummary(circleId: String): Result<CircleSummary> {
        return try {
            // Generate complete summary using the service
            val summaryResult = ragService.generateCompleteSummary(circleId)
            
            if (summaryResult.isSuccess) {
                val circleSummary = summaryResult.getOrNull()!!
                
                // Update circle with summary
                val circleRef = firestore.collection("circles").document(circleId)
                val updates = mapOf(
                    "ragSummary" to circleSummary.summary,
                    "ragHighlights" to circleSummary.highlights,
                    "ragSummaryGeneratedAt" to circleSummary.generatedAt
                )
                
                circleRef.update(updates).await()
                Result.success(circleSummary)
            } else {
                summaryResult
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Converts an image URI to base64 string
     */
    private fun convertImageToBase64(uri: Uri, context: Context): String {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val outputStream = ByteArrayOutputStream()
        
        // Compress image to reduce size while maintaining quality
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        val imageBytes = outputStream.toByteArray()
        
        return Base64.encodeToString(imageBytes, Base64.DEFAULT)
    }
    
    suspend fun findSimilarSnaps(snap: Snap): Result<List<Snap>> {
        return ragService.findRelatedContent(snap)
    }
    
    suspend fun generateSnapEmbedding(snap: Snap): Result<List<Float>> {
        return embeddingService.generateSnapEmbedding(snap)
    }
    
    suspend fun getCircleHighlights(circleId: String): Result<List<String>> {
        return ragService.generateCircleHighlights(circleId)
    }
    
    suspend fun analyzeCirclePatterns(circleId: String): Result<List<String>> {
        val snaps = firestore.collection("snaps")
            .whereEqualTo("circleId", circleId)
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
        
        return ragService.analyzeContentPatterns(snaps)
    }
} 