package com.example.myapplication.ui.circles

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.models.Circle
import com.example.myapplication.data.repositories.CircleRepository
import kotlinx.coroutines.launch

class CirclesViewModel : ViewModel() {
    private val circleRepository = CircleRepository()
    
    var circles by mutableStateOf<List<Circle>>(emptyList())
        private set
        
    var invitations by mutableStateOf<List<Circle>>(emptyList())
        private set
        
    var isLoading by mutableStateOf(false)
        private set
        
    var errorMessage by mutableStateOf<String?>(null)
        private set
    
    fun loadCircles() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            
            try {
                val result = circleRepository.getUserCircles()
                if (result.isSuccess) {
                    circles = result.getOrNull() ?: emptyList()
                } else {
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to load circles"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unknown error occurred"
            } finally {
                isLoading = false
            }
        }
    }
    
    fun loadInvitations() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            
            try {
                val result = circleRepository.getPendingInvites()
                if (result.isSuccess) {
                    invitations = result.getOrNull() ?: emptyList()
                } else {
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to load invitations"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unknown error occurred"
            } finally {
                isLoading = false
            }
        }
    }
    
    fun acceptInvitation(circleId: String) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            
            try {
                val result = circleRepository.acceptInvitation(circleId)
                if (result.isSuccess) {
                    // Remove from invitations
                    invitations = invitations.filter { it.id != circleId }
                    // Reload circles to include the newly accepted circle
                    loadCircles()
                } else {
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to accept invitation"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unknown error occurred"
            } finally {
                isLoading = false
            }
        }
    }
    
    fun declineInvitation(circleId: String) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            
            try {
                val result = circleRepository.declineInvitation(circleId)
                if (result.isSuccess) {
                    // Remove from invitations
                    invitations = invitations.filter { it.id != circleId }
                } else {
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to decline invitation"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unknown error occurred"
            } finally {
                isLoading = false
            }
        }
    }
    
    fun leaveCircle(circleId: String) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            
            try {
                val result = circleRepository.leaveCircle(circleId)
                if (result.isSuccess) {
                    // Remove from circles
                    circles = circles.filter { it.id != circleId }
                } else {
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to leave circle"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unknown error occurred"
            } finally {
                isLoading = false
            }
        }
    }
    
    fun deleteCircle(circleId: String) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            
            try {
                val result = circleRepository.deleteCircle(circleId)
                if (result.isSuccess) {
                    // Remove from circles
                    circles = circles.filter { it.id != circleId }
                } else {
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to delete circle"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unknown error occurred"
            } finally {
                isLoading = false
            }
        }
    }
} 