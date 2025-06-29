package com.example.myapplication.data.services

import com.example.myapplication.BuildConfig
import com.example.myapplication.data.models.Snap
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

interface EmbeddingApi {
    @POST("v1/embeddings")
    suspend fun createEmbedding(
        @Body request: EmbeddingRequest,
        @Header("Authorization") authorization: String = "Bearer ${BuildConfig.OPENAI_API_KEY}"
    ): Response<EmbeddingResponse>
}

data class EmbeddingRequest(
    val model: String = "text-embedding-3-small",
    val input: String
)

data class EmbeddingResponse(
    val data: List<EmbeddingData>
)

data class EmbeddingData(
    val embedding: List<Float>
)

class EmbeddingService {
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
    
    private val api = retrofit.create(EmbeddingApi::class.java)
    private val firestore = FirebaseFirestore.getInstance()
    
    suspend fun generateSnapEmbedding(snap: Snap): Result<List<Float>> {
        return try {
            // Create a rich text representation of the snap
            val snapText = buildString {
                append("Media Type: ${snap.mediaType}\n")
                append("Caption: ${snap.caption ?: "No caption"}\n")
                append("Sender: ${snap.senderName}\n")
                append("Time: ${snap.createdAt.toDate()}")
            }
            
            val request = EmbeddingRequest(input = snapText)
            val response = api.createEmbedding(request)
            
            if (response.isSuccessful) {
                val embedding = response.body()?.data?.firstOrNull()?.embedding
                if (embedding != null) {
                    // Store the embedding in Firestore
                    val snapRef = firestore.collection("snaps").document(snap.id)
                    snapRef.update("embedding", embedding).await()
                    
                    Result.success(embedding)
                } else {
                    Result.failure(Exception("No embedding generated"))
                }
            } else {
                Result.failure(Exception("Failed to generate embedding: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun findSimilarSnaps(snap: Snap, limit: Int = 5): Result<List<Snap>> {
        return try {
            // Get the embedding for the target snap
            val snapDoc = firestore.collection("snaps").document(snap.id).get().await()
            val embedding = snapDoc.get("embedding") as? List<Float> ?: return Result.failure(Exception("No embedding found"))
            
            // Get all snaps in the same circle
            val circleSnaps = firestore.collection("snaps")
                .whereEqualTo("circleId", snap.circleId)
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
            
            // Calculate cosine similarity with each snap
            val similarities = circleSnaps.mapNotNull { otherSnap ->
                val otherEmbedding = firestore.collection("snaps")
                    .document(otherSnap.id)
                    .get()
                    .await()
                    .get("embedding") as? List<Float>
                
                if (otherEmbedding != null) {
                    val similarity = cosineSimilarity(embedding, otherEmbedding)
                    Pair(otherSnap, similarity)
                } else null
            }
            
            // Return the most similar snaps
            val similarSnaps = similarities
                .sortedByDescending { it.second }
                .take(limit)
                .map { it.first }
            
            Result.success(similarSnaps)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun cosineSimilarity(a: List<Float>, b: List<Float>): Float {
        if (a.size != b.size) return 0f
        
        var dotProduct = 0f
        var normA = 0f
        var normB = 0f
        
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        
        return if (normA > 0 && normB > 0) {
            dotProduct / (kotlin.math.sqrt(normA.toDouble()) * kotlin.math.sqrt(normB.toDouble())).toFloat()
        } else {
            0f
        }
    }
} 