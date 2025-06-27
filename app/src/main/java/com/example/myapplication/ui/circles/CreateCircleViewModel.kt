package com.example.myapplication.ui.circles

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repositories.CircleRepository
import kotlinx.coroutines.launch

class CreateCircleViewModel : ViewModel() {
    private val circleRepository = CircleRepository()
    
    var isLoading by mutableStateOf(false)
        private set
        
    var errorMessage by mutableStateOf<String?>(null)
        private set
        
    var createdCircleId by mutableStateOf<String?>(null)
        private set
        
    var hasAttemptedSubmit by mutableStateOf(false)
        private set
    
    fun createCircle(
        name: String,
        description: String? = null,
        durationMillis: Long = CircleRepository.DURATION_24_HOURS,
        private: Boolean = true,
        locationEnabled: Boolean = false,
        locationLat: Double? = null,
        locationLng: Double? = null,
        locationRadius: Double? = null,
        initialMembers: List<String> = emptyList()
    ) {
        // Validate input
        hasAttemptedSubmit = true
        
        if (name.isBlank()) {
            errorMessage = "Circle name is required"
            return
        }
        
        // Create the circle
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            
            try {
                val result = circleRepository.createCircle(
                    name = name,
                    description = description,
                    durationMillis = durationMillis,
                    private = private,
                    locationEnabled = locationEnabled,
                    locationLat = locationLat,
                    locationLng = locationLng,
                    locationRadius = locationRadius,
                    initialMembers = initialMembers
                )
                
                if (result.isSuccess) {
                    createdCircleId = result.getOrNull()?.id
                } else {
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to create circle"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unknown error occurred"
            } finally {
                isLoading = false
            }
        }
    }
    
    fun clearError() {
        errorMessage = null
    }
    
    fun reset() {
        isLoading = false
        errorMessage = null
        createdCircleId = null
        hasAttemptedSubmit = false
    }
} 