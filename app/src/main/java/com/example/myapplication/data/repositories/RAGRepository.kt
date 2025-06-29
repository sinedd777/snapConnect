package com.example.myapplication.data.repositories

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.myapplication.data.models.CircleContext
import com.example.myapplication.data.models.CircleSummary
import com.example.myapplication.data.models.Snap
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
     * Generates and stores a caption for a snap using RAG
     */
    suspend fun generateAndStoreSnapCaption(
        snap: Snap,
        imageUri: Uri,
        context: Context
    ): Result<String> {
        return try {
            // Convert image to base64
            val imageBase64 = convertImageToBase64(imageUri, context)
            
            // Generate caption
            val captionResult = ragService.generateImageCaption(imageBase64)
            
            if (captionResult.isSuccess) {
                // Update snap with RAG caption
                val caption = captionResult.getOrNull()
                val snapRef = firestore.collection("snaps").document(snap.id)
                
                val updates = mapOf(
                    "ragGeneratedCaption" to caption,
                    "ragCaptionGenerated" to true,
                    "ragCaptionGeneratedAt" to Timestamp.now()
                )
                
                snapRef.update(updates).await()
                Result.success(caption ?: "")
            } else {
                captionResult
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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
    
    suspend fun getCircleContext(circleId: String): Result<CircleContext> {
        return ragService.retrieveCircleContext(circleId)
    }
    
    suspend fun findSimilarSnaps(snap: Snap): Result<List<Snap>> {
        return embeddingService.findSimilarSnaps(snap)
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