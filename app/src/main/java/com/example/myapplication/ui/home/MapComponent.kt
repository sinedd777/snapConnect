package com.example.myapplication.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.models.Circle
import kotlin.math.pow

/**
 * A placeholder map component that shows circle pins
 * This would be replaced with an actual map implementation in a real app
 */
@Composable
fun MapComponent(
    circles: List<Circle>,
    userLat: Double?,
    userLng: Double?,
    onCircleClick: (Circle) -> Unit,
    modifier: Modifier = Modifier
) {
    // Map state
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    
    // Get colors from the theme
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    val secondaryContainerColor = MaterialTheme.colorScheme.secondaryContainer
    val onPrimaryContainerColor = MaterialTheme.colorScheme.onPrimaryContainer
    val onSecondaryContainerColor = MaterialTheme.colorScheme.onSecondaryContainer
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale *= zoom
                    offset += pan
                }
            }
            .pointerInput(circles) {
                detectTapGestures { tapPosition ->
                    // Check if tap is on any circle pin
                    circles.forEachIndexed { index, circle ->
                        val pinX = size.width / 2 + (index * 50 - circles.size * 25) * scale + offset.x
                        val pinY = size.height / 2 + (index % 3 * 50 - 50) * scale + offset.y
                        
                        val baseSize = 20f
                        val memberBonus = (circle.members.size * 0.5f).coerceAtMost(30f)
                        val pinSize = (baseSize + memberBonus) * scale
                        
                        // Check if tap is within this pin
                        val distance = kotlin.math.sqrt(
                            (tapPosition.x - pinX).pow(2) + 
                            (tapPosition.y - pinY).pow(2)
                        )
                        
                        if (distance <= pinSize) {
                            // Pin was tapped, call the callback
                            onCircleClick(circle)
                            return@detectTapGestures
                        }
                    }
                }
            }
    ) {
        // Draw the map background
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw a grid to represent the map
            val gridSize = 50f
            val gridColor = Color.Gray.copy(alpha = 0.2f)
            
            for (x in 0..(size.width / gridSize).toInt()) {
                drawLine(
                    color = gridColor,
                    start = Offset(x * gridSize * scale + offset.x, 0f),
                    end = Offset(x * gridSize * scale + offset.x, size.height),
                    strokeWidth = 1f
                )
            }
            
            for (y in 0..(size.height / gridSize).toInt()) {
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y * gridSize * scale + offset.y),
                    end = Offset(size.width, y * gridSize * scale + offset.y),
                    strokeWidth = 1f
                )
            }
            
            // Draw user location
            if (userLat != null && userLng != null) {
                val centerX = size.width / 2
                val centerY = size.height / 2
                
                // Draw user location with a pulsing effect
                val pulseRadius = 30f * scale
                drawCircle(
                    color = primaryColor.copy(alpha = 0.2f),
                    radius = pulseRadius,
                    center = Offset(centerX + offset.x, centerY + offset.y)
                )
                
                drawCircle(
                    color = primaryColor.copy(alpha = 0.4f),
                    radius = pulseRadius * 0.7f,
                    center = Offset(centerX + offset.x, centerY + offset.y)
                )
                
                drawCircle(
                    color = primaryColor,
                    radius = 8f * scale,
                    center = Offset(centerX + offset.x, centerY + offset.y)
                )
            }
            
            // Draw circles as pins on the map
            circles.forEachIndexed { index, circle ->
                val pinX = size.width / 2 + (index * 50 - circles.size * 25) * scale + offset.x
                val pinY = size.height / 2 + (index % 3 * 50 - 50) * scale + offset.y
                
                // Calculate pin size based on member count
                val baseSize = 20f
                val memberBonus = (circle.members.size * 0.5f).coerceAtMost(30f)
                val pinSize = (baseSize + memberBonus) * scale
                
                // Draw pin shadow
                drawCircle(
                    color = Color.Black.copy(alpha = 0.2f),
                    radius = pinSize + 2f * scale,
                    center = Offset(pinX, pinY + 2f * scale)
                )
                
                // Draw the main pin circle
                val pinColor = if (circle.private) {
                    secondaryContainerColor
                } else {
                    primaryContainerColor
                }
                
                // Draw outer glow
                drawCircle(
                    color = pinColor.copy(alpha = 0.3f),
                    radius = pinSize + 4f * scale,
                    center = Offset(pinX, pinY)
                )
                
                // Draw main circle
                drawCircle(
                    color = pinColor,
                    radius = pinSize,
                    center = Offset(pinX, pinY)
                )
                
                // Draw border
                drawCircle(
                    color = if (circle.private) 
                        onSecondaryContainerColor.copy(alpha = 0.5f)
                    else 
                        onPrimaryContainerColor.copy(alpha = 0.5f),
                    radius = pinSize,
                    center = Offset(pinX, pinY),
                    style = Stroke(width = 2f * scale)
                )
                
                // Draw inner highlight
                drawCircle(
                    color = Color.White.copy(alpha = 0.3f),
                    radius = pinSize * 0.7f,
                    center = Offset(pinX - pinSize * 0.2f, pinY - pinSize * 0.2f)
                )
            }
        }
        
        // User location indicator text
        if (userLat != null && userLng != null) {
            Text(
                text = "Your Location\n$userLat, $userLng",
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .background(
                        surfaceColor.copy(alpha = 0.8f),
                        MaterialTheme.shapes.small
                    )
                    .padding(8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = onSurfaceColor
            )
        }
        
        // Scale indicator
        Text(
            text = "Zoom: ${String.format("%.1f", scale)}x",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .background(
                    surfaceColor.copy(alpha = 0.8f),
                    MaterialTheme.shapes.small
                )
                .padding(8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = onSurfaceColor
        )
        
        // Instructions
        if (circles.isEmpty()) {
            Text(
                text = "Pinch to zoom\nDrag to pan",
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(
                        surfaceColor.copy(alpha = 0.8f),
                        MaterialTheme.shapes.medium
                    )
                    .padding(16.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = onSurfaceColor
            )
        }
    }
}

@Composable
fun CirclePin(
    circle: Circle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pinSize = remember(circle) {
        val baseSize = 40
        val memberBonus = (circle.members.size * 2).coerceAtMost(20)
        (baseSize + memberBonus).dp
    }
    
    Box(
        modifier = modifier
            .size(pinSize)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(
                if (circle.private) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.primaryContainer
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = circle.name.first().toString(),
            style = MaterialTheme.typography.titleMedium,
            color = if (circle.private) MaterialTheme.colorScheme.onSecondaryContainer
                  else MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
} 