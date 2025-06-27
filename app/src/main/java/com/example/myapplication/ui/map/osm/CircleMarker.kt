package com.example.myapplication.ui.map.osm

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.example.myapplication.R
import com.example.myapplication.data.models.Circle
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * Custom marker for displaying Circles on the map
 */
class CircleMarker(
    osmMapView: MapView,
    val circle: Circle,
    val isSelected: Boolean = false
) : Marker(osmMapView) {

    init {
        position = GeoPoint(
            circle.locationLat ?: 0.0,
            circle.locationLng ?: 0.0
        )
        
        // Set title and snippet
        title = circle.name
        snippet = when {
            circle.members.size == 1 -> "1 participant"
            else -> "${circle.members.size} participants"
        }
        
        // Set marker appearance
        setAnchor(ANCHOR_CENTER, ANCHOR_BOTTOM)
        
        // Customize marker based on Circle properties
        val markerSize = calculateMarkerSize(circle.members.size)
        val markerColor = if (circle.isPrivate) PRIVATE_COLOR else PUBLIC_COLOR
        val markerStrokeColor = if (isSelected) SELECTED_STROKE_COLOR else DEFAULT_STROKE_COLOR
        
        // Create custom marker icon
        icon = createMarkerIcon(
            osmMapView.context,
            markerSize,
            markerColor,
            markerStrokeColor,
            circle.category
        )
    }
    
    /**
     * Calculate marker size based on number of members
     */
    private fun calculateMarkerSize(memberCount: Int): Float {
        // Base size + additional size based on member count
        return BASE_SIZE + (memberCount * MEMBER_SIZE_FACTOR).coerceAtMost(MAX_ADDITIONAL_SIZE)
    }
    
    /**
     * Create a custom marker icon
     */
    private fun createMarkerIcon(
        context: Context,
        size: Float,
        color: Int,
        strokeColor: Int,
        category: String?
    ): Drawable? {
        // For now, we'll use a simple circle drawable
        // In a real app, you might want to create a more complex drawable based on category
        return CircleDrawable(size, color, strokeColor, category)
    }
    
    companion object {
        // Marker appearance constants
        private const val BASE_SIZE = 30f
        private const val MEMBER_SIZE_FACTOR = 2f
        private const val MAX_ADDITIONAL_SIZE = 30f
        
        // Colors
        private val PUBLIC_COLOR = Color.parseColor("#6200EE")
        private val PRIVATE_COLOR = Color.parseColor("#03DAC5")
        private val DEFAULT_STROKE_COLOR = Color.WHITE
        private val SELECTED_STROKE_COLOR = Color.YELLOW
    }
    
    /**
     * Custom drawable for Circle markers
     */
    private class CircleDrawable(
        private val size: Float,
        private val color: Int,
        private val strokeColor: Int,
        private val category: String?
    ) : Drawable() {
        
        private val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = this@CircleDrawable.color
        }
        
        private val strokePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = strokeColor
        }
        
        override fun draw(canvas: Canvas) {
            val centerX = bounds.width() / 2f
            val centerY = bounds.height() / 2f
            val radius = size / 2f
            
            // Draw the circle
            canvas.drawCircle(centerX, centerY, radius, paint)
            canvas.drawCircle(centerX, centerY, radius, strokePaint)
            
            // Draw category indicator if available
            category?.let {
                // This could be extended to draw different icons based on category
                // For now, we'll just use a different color in the center
                val categoryPaint = Paint().apply {
                    isAntiAlias = true
                    style = Paint.Style.FILL
                    color = getCategoryColor(it)
                }
                canvas.drawCircle(centerX, centerY, radius / 3f, categoryPaint)
            }
        }
        
        private fun getCategoryColor(category: String): Int {
            return when (category.lowercase()) {
                "party" -> Color.parseColor("#FF4081")
                "study" -> Color.parseColor("#2196F3")
                "sports" -> Color.parseColor("#4CAF50")
                "food" -> Color.parseColor("#FF9800")
                "music" -> Color.parseColor("#9C27B0")
                else -> Color.parseColor("#607D8B")
            }
        }
        
        override fun setAlpha(alpha: Int) {
            paint.alpha = alpha
            strokePaint.alpha = alpha
        }
        
        override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
            paint.colorFilter = colorFilter
            strokePaint.colorFilter = colorFilter
        }
        
        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int {
            return android.graphics.PixelFormat.TRANSLUCENT
        }
        
        override fun getIntrinsicWidth(): Int {
            return size.toInt() * 2
        }
        
        override fun getIntrinsicHeight(): Int {
            return size.toInt() * 2
        }
    }
} 