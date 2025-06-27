package com.example.myapplication.ui.map.osm

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.models.Circle
import com.example.myapplication.ui.map.CircleMapViewModel
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import android.util.Log
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.config.Configuration
import androidx.preference.PreferenceManager

private const val TAG = "OSMMapComponent"

/**
 * OpenStreetMap implementation of the map component
 */
@Composable
fun OSMMapComponent(
    circles: List<Circle>,
    userLat: Double?,
    userLng: Double?,
    onCircleClick: (Circle) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CircleMapViewModel? = null
) {    
    Log.d(TAG, "OSMMapComponent recomposed with ${circles.size} circles, location: ($userLat, $userLng)")
    
    val context = LocalContext.current
    
    // Use remember for state management
    val selectedCircle = remember { mutableStateOf<Circle?>(null) }
    val lastOverlayCount = remember { mutableStateOf(0) }
    val isMapInitialized = remember { mutableStateOf(false) }
    
    // Create a stable reference to the map view
    val mapViewHolder = remember { 
        object {
            var mapView: MapView? = null
        }
    }
    
    // Configure OSMDroid
    LaunchedEffect(Unit) {
        try {
            Log.d(TAG, "Configuring OSMDroid...")
            val config = Configuration.getInstance()
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            config.load(context, prefs)
            
            // Log configuration details
            Log.d(TAG, """
                OSMDroid Configuration:
                User Agent: ${config.userAgentValue}
                Base Path: ${config.osmdroidBasePath}
                Tile Cache: ${config.osmdroidTileCache}
                Has write permission: ${context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED}
                Has internet permission: ${context.checkSelfPermission(android.Manifest.permission.INTERNET) == android.content.pm.PackageManager.PERMISSION_GRANTED}
            """.trimIndent())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure OSMDroid", e)
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Render the OSM map
        OSMMapView(
            modifier = Modifier.fillMaxSize(),
            onMapReady = { map ->
                try {
                    Log.d(TAG, "Map ready callback triggered")
                    mapViewHolder.mapView = map
                    
                    // Set custom map style using OpenStreetMap's standard tile source
                    try {
                        Log.d(TAG, "Setting up tile source...")
                        val customTileSource = XYTileSource(
                            "OpenStreetMap",
                            0, // min zoom
                            20, // max zoom
                            256,
                            ".png",
                            arrayOf(
                                "https://a.tile.openstreetmap.org/",
                                "https://b.tile.openstreetmap.org/",
                                "https://c.tile.openstreetmap.org/"
                            ),
                            "© OpenStreetMap contributors"
                        )
                        map.setTileSource(customTileSource)
                        Log.d(TAG, "Tile source set successfully")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to set tile source", e)
                        // Fallback to default tile source
                        Log.d(TAG, "Falling back to default MAPNIK tile source")
                        map.setTileSource(TileSourceFactory.MAPNIK)
                    }
                    
                    // Configure map settings
                    map.apply {
                        Log.d(TAG, "Configuring map settings...")
                        setMultiTouchControls(true)
                        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                        setHorizontalMapRepetitionEnabled(false)
                        setVerticalMapRepetitionEnabled(false)
                        minZoomLevel = 3.0
                        maxZoomLevel = 20.0
                        isTilesScaledToDpi = true
                        
                        // Set default zoom level for better initial view
                        controller.setZoom(16.0)
                        Log.d(TAG, "Map zoom set to: ${getZoomLevel()}")
                        
                        // Add overlays
                        
                    }
                    
                    // Connect to view model if provided
                    viewModel?.setMapReference(map)
                    
                    // Enable location overlay with custom styling
                    try {
                        Log.d(TAG, "Setting up location overlay...")
                        val myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), map).apply {
                            enableMyLocation()
                            // Create a custom person icon
                            val personBitmap = android.graphics.Bitmap.createBitmap(32, 32, android.graphics.Bitmap.Config.ARGB_8888)
                            val canvas = android.graphics.Canvas(personBitmap)
                            val paint = android.graphics.Paint().apply {
                                color = android.graphics.Color.parseColor("#4CAF50")
                                style = android.graphics.Paint.Style.FILL
                                isAntiAlias = true
                            }
                            canvas.drawCircle(16f, 16f, 14f, paint.apply {
                                alpha = 100
                            })
                            canvas.drawCircle(16f, 16f, 8f, paint.apply {
                                alpha = 255
                            })
                            setPersonIcon(personBitmap)
                            setDirectionIcon(null)
                            isDrawAccuracyEnabled = true
                        }
                        map.overlays.add(myLocationOverlay)
                        Log.d(TAG, "Location overlay added successfully")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to set up location overlay", e)
                    }
                    
                    // Set initial center if location is available
                    if (userLat != null && userLng != null) {
                        Log.d(TAG, "Setting initial center to: $userLat, $userLng")
                        map.controller.animateTo(GeoPoint(userLat, userLng))
                    } else {
                        Log.w(TAG, "No user location available for initial center")
                    }
                    
                    // Initial marker setup
                    updateMapMarkers(map, circles, selectedCircle.value, onCircleClick) { circle ->
                        selectedCircle.value = circle
                    }
                    
                    isMapInitialized.value = true
                    Log.d(TAG, "Map initialization completed successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error during map initialization", e)
                }
            }
        )
        
        // Show loading indicator if map is not initialized
        if (!isMapInitialized.value) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Loading map...",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
    
    // Combined LaunchedEffect for map updates
    LaunchedEffect(circles, selectedCircle.value, userLat, userLng) {
        try {
            mapViewHolder.mapView?.let { map ->
                // Update markers only if circles or selection changed
                if (circles.isNotEmpty()) {
                    Log.d(TAG, "Updating map markers with ${circles.size} circles")
                    updateMapMarkers(map, circles, selectedCircle.value, onCircleClick) { circle ->
                        selectedCircle.value = circle
                    }
                }
                
                // Update center if location changed
                if (userLat != null && userLng != null) {
                    Log.d(TAG, "Updating map center to: $userLat, $userLng")
                    map.controller.animateTo(GeoPoint(userLat, userLng))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating map", e)
        }
    }
    
    // Clean up
    DisposableEffect(Unit) {
        onDispose {
            try {
                Log.d(TAG, "Disposing map view")
                mapViewHolder.mapView?.onDetach()
                mapViewHolder.mapView = null
            } catch (e: Exception) {
                Log.e(TAG, "Error disposing map view", e)
            }
        }
    }
}

/**
 * Update circle markers on the map efficiently
 */
private fun updateMapMarkers(
    mapView: MapView,
    circles: List<Circle>,
    selectedCircle: Circle?,
    onCircleClick: (Circle) -> Unit,
    onCircleSelected: (Circle) -> Unit
) {    
    // Get existing markers
    val existingMarkers = mapView.overlays.filterIsInstance<CircleMarker>()
    val existingCircleIds = existingMarkers.map { it.getCircleId() }.toSet()
    
    // Find circles to add and remove
    val circlesToAdd = circles.filter { it.id !in existingCircleIds }
    val markersToRemove = existingMarkers.filter { marker -> 
        circles.none { it.id == marker.getCircleId() }
    }
    
    // Remove old markers
    if (markersToRemove.isNotEmpty()) {
        mapView.overlays.removeAll(markersToRemove)
    }
    
    // Add new markers
    circlesToAdd.forEach { circle ->
        if (circle.locationLat != null && circle.locationLng != null) {
            val marker = CircleMarker(
                osmMapView = mapView,
                circle = circle,
                isSelected = circle == selectedCircle
            ).apply {
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                isDraggable = false
                isFlat = true
                
                setOnMarkerClickListener { _, _ ->
                    onCircleSelected(circle)
                    onCircleClick(circle)
                    true
                }
            }
            mapView.overlays.add(marker)
        }
    }
    
    // Update existing markers' selected state
    existingMarkers.forEach { marker ->
        val circle = circles.find { it.id == marker.getCircleId() }
        if (circle != null) {
            marker.updateSelectedState(circle == selectedCircle)
        }
    }
    
    // Refresh the map only if changes were made
    if (circlesToAdd.isNotEmpty() || markersToRemove.isNotEmpty()) {
        mapView.invalidate()
    }
}

// Extension function to get circle ID from marker
private fun CircleMarker.getCircleId(): String = this.circle.id

// Extension function to update marker selected state
private fun CircleMarker.updateSelectedState(isSelected: Boolean) {
    // Implementation depends on your CircleMarker class
    // You'll need to add this functionality to CircleMarker
}

/**
 * Add circle markers to the map
 */
private fun addCircleMarkers(
    mapView: MapView,
    circles: List<Circle>,
    selectedCircle: Circle?,
    onCircleClick: (Circle) -> Unit,
    onCircleSelected: (Circle) -> Unit
) {    
    Log.d(TAG, "Adding ${circles.size} markers to map")
    
    // Clear existing markers
    val overlaysToKeep = mapView.overlays.filterNot { it is CircleMarker }
    val markersRemoved = mapView.overlays.size - overlaysToKeep.size
    Log.d(TAG, "Cleared $markersRemoved existing markers")
    
    mapView.overlays.clear()
    mapView.overlays.addAll(overlaysToKeep)
    
    // Add markers for each circle
    var validMarkersAdded = 0
    var invalidLocations = 0
    
    circles.forEach { circle ->
        if (circle.locationLat != null && circle.locationLng != null) {
            val marker = CircleMarker(
                osmMapView = mapView,
                circle = circle,
                isSelected = circle == selectedCircle
            ).apply {
                // Make markers more visible
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                isDraggable = false
                isFlat = true
            }
            
            marker.setOnMarkerClickListener { _, _ ->
                Log.d(TAG, "Marker clicked for circle: ${circle.name}")
                onCircleSelected(circle)
                onCircleClick(circle)
                true
            }
            
            mapView.overlays.add(marker)
            validMarkersAdded++
            Log.d(TAG, "Added marker for circle: ${circle.name} at (${circle.locationLat}, ${circle.locationLng})")
        } else {
            invalidLocations++
            Log.w(TAG, "Circle ${circle.name} has invalid location")
        }
    }
    
    Log.d(TAG, "Added $validMarkersAdded valid markers, $invalidLocations circles had invalid locations")
    
    // Refresh the map
    mapView.invalidate()
}

/**
 * Extension function to set map center
 */
private fun MapView.setMapCenter(lat: Double, lng: Double) {
    controller.setCenter(GeoPoint(lat, lng))
} 