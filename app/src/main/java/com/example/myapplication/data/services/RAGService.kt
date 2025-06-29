package com.example.myapplication.data.services

import com.example.myapplication.data.models.CircleContext
import com.example.myapplication.data.models.CircleSummary
import com.example.myapplication.data.models.Snap

/**
 * Service interface for RAG (Retrieval Augmented Generation) functionality
 */
interface RAGService {
    /**
     * Generates a caption for an image using RAG
     * @param imageBase64 The base64 encoded image data
     * @return Result containing the generated caption or an error
     */
    suspend fun generateImageCaption(imageBase64: String): Result<String>
    
    /**
     * Summarizes the content of a circle based on its snaps
     * @param snaps List of snaps in the circle
     * @return Result containing the generated summary or an error
     */
    suspend fun summarizeCircleContent(snaps: List<Snap>): Result<String>
    
    /**
     * Generates highlights for a circle's content
     * @param circleId The ID of the circle
     * @return Result containing a list of highlights or an error
     */
    suspend fun generateCircleHighlights(circleId: String): Result<List<String>>
    
    /**
     * Generates a complete circle summary including highlights
     * @param circleId The ID of the circle
     * @return Result containing the CircleSummary or an error
     */
    suspend fun generateCompleteSummary(circleId: String): Result<CircleSummary>
    
    // New RAG-specific methods
    suspend fun retrieveCircleContext(circleId: String): Result<CircleContext>
    
    suspend fun analyzeContentPatterns(snaps: List<Snap>): Result<List<String>>
    
    suspend fun findRelatedContent(snap: Snap): Result<List<Snap>>
    
    suspend fun generateThematicSummary(context: CircleContext): Result<CircleSummary>
} 