package com.example.myapplication.ui.map.osm

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.example.myapplication.data.models.Circle
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * Custom marker for displaying Circle pins on the map
 */
class CircleMarker(
    private val osmMapView: MapView,
    val circle: Circle,
    private val isSelected: Boolean = false
) : Marker(osmMapView) {
    
    private var pinSize: Float = 0f
    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.WHITE
    }
    
    init {
        position = GeoPoint(circle.locationLat ?: 0.0, circle.locationLng ?: 0.0)
        title = circle.name
        snippet = circle.description
        
        // Calculate pin size based on member count
        val baseSize = 20f
        val memberBonus = (circle.members.size * 0.5f).coerceAtMost(30f)
        pinSize = baseSize + memberBonus
        
        // Set the pin color based on privacy
        paint.color = if (circle.isPrivate) {
            Color.parseColor("#E91E63") // Pink for private
        } else {
            Color.parseColor("#4CAF50") // Green for public
        }
        
        // Highlight if selected
        if (isSelected) {
            strokePaint.color = Color.YELLOW
            strokePaint.strokeWidth = 5f
        }
    }
    
    /**
     * Draw the custom marker
     */
    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        
        // Get the screen coordinates for this marker
        val point = mapView.projection.toPixels(position, null)
        
        // Draw the circle pin
        canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), pinSize, paint)
        
        // Draw the stroke
        canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), pinSize, strokePaint)
        
        // Draw the first letter of the circle name
        if (circle.name.isNotEmpty()) {
            val textPaint = Paint().apply {
                isAntiAlias = true
                color = Color.WHITE
                textSize = pinSize * 0.8f
                textAlign = Paint.Align.CENTER
            }
            
            val letter = circle.name.first().toString()
            val textHeight = textPaint.descent() - textPaint.ascent()
            val textOffset = (textHeight / 2) - textPaint.descent()
            
            canvas.drawText(
                letter,
                point.x.toFloat(),
                point.y.toFloat() + textOffset,
                textPaint
            )
        }
    }
    
    /**
     * Handle marker click
     */
    override fun onMarkerClickDefault(marker: Marker, mapView: MapView): Boolean {
        // Show info window with circle details
        showInfoWindow()
        return true
    }
    
    /**
     * Set the marker as selected
     */
    fun setSelected(selected: Boolean) {
        if (selected) {
            strokePaint.color = Color.YELLOW
            strokePaint.strokeWidth = 5f
        } else {
            strokePaint.color = Color.WHITE
            strokePaint.strokeWidth = 3f
        }
        
        // Force redraw
        osmMapView.invalidate()
    }
} 