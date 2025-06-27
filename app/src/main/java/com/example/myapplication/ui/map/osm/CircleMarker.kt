package com.example.myapplication.ui.map.osm

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.myapplication.R
import com.example.myapplication.data.models.Circle
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import android.graphics.Color
import android.util.Log
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable

private const val TAG = "CircleMarker"

/**
 * Custom marker for displaying Circles on the map
 */
class CircleMarker(
    osmMapView: MapView,
    val circle: Circle,
    isSelected: Boolean = false
) : Marker(osmMapView) {

    private var currentIsSelected = isSelected

    init {
        Log.d(TAG, """Creating marker for circle:
            |ID: ${circle.id}
            |Name: ${circle.name}
            |Location: (${circle.locationLat}, ${circle.locationLng})
            |Selected: $isSelected""".trimMargin())
        
        position = GeoPoint(circle.locationLat ?: 0.0, circle.locationLng ?: 0.0)
        
        // Create a custom marker icon
        val size = if (isSelected) 48 else 40
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Define colors
        val mainColor = Color.parseColor("#FF6200EE") // Primary color
        val strokeColor = Color.WHITE
        val shadowColor = Color.parseColor("#40000000") // Semi-transparent black
        
        // Create paints
        val fillPaint = Paint().apply {
            color = mainColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        val strokePaint = Paint().apply {
            color = strokeColor
            style = Paint.Style.STROKE
            strokeWidth = size / 10f
            isAntiAlias = true
        }
        
        val shadowPaint = Paint().apply {
            color = shadowColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        // Draw shadow
        canvas.drawCircle(size/2f, size/2f + size/8f, size/2f - size/8f, shadowPaint)
        
        // Draw main circle
        canvas.drawCircle(size/2f, size/2f, size/2f - size/8f, fillPaint)
        canvas.drawCircle(size/2f, size/2f, size/2f - size/8f, strokePaint)
        
        // Add a subtle inner shadow effect
        val innerShadowPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            isAntiAlias = true
            alpha = 40
        }
        canvas.drawCircle(size/2f, size/2f + size/16f, size/2f - size/4f, innerShadowPaint)
        
        // If selected, add a pulsing effect
        if (isSelected) {
            val pulseRadius = size/2f + size/8f
            val pulsePaint = Paint().apply {
                color = mainColor
                style = Paint.Style.STROKE
                strokeWidth = size/16f
                isAntiAlias = true
                alpha = 100
            }
            canvas.drawCircle(size/2f, size/2f, pulseRadius, pulsePaint)
        }
        
        // Convert bitmap to drawable
        icon = BitmapDrawable(osmMapView.context.resources, bitmap)
    }

    fun updateSelectedState(isSelected: Boolean) {
        if (currentIsSelected != isSelected) {
            currentIsSelected = isSelected
            updateMarkerAppearance()
        }
    }

    private fun updateMarkerAppearance() {
        // Implementation of updateMarkerAppearance method
    }
    
    override fun onDetach(mapView: MapView?) {
        super.onDetach(mapView)
        Log.d(TAG, "Marker detached for circle: ${circle.name}")
    }
    
    override fun draw(canvas: Canvas?, mapView: MapView?, shadow: Boolean) {
        if (shadow) return
        super.draw(canvas, mapView, shadow)
    }

    fun getCircleId(): String? = circle.id
} 