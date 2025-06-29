package com.example.myapplication.data.services

import com.example.myapplication.BuildConfig
import com.example.myapplication.data.models.OpenAIRequest
import com.example.myapplication.data.models.OpenAIResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit interface for OpenAI API calls
 */
interface OpenAIApi {
    @POST("v1/chat/completions")
    suspend fun generateCaption(
        @Body request: OpenAIRequest,
        @Header("Authorization") authorization: String = "Bearer ${BuildConfig.OPENAI_API_KEY}"
    ): Response<OpenAIResponse>
    
    @POST("v1/chat/completions")
    suspend fun generateSummary(
        @Body request: OpenAIRequest,
        @Header("Authorization") authorization: String = "Bearer ${BuildConfig.OPENAI_API_KEY}"
    ): Response<OpenAIResponse>
} 