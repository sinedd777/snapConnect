package com.example.myapplication.ui.map.osm

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.models.Circle
import com.example.myapplication.ui.map.CircleMapViewModel
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

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
    val context = LocalContext.current
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var selectedCircle by remember { mutableStateOf<Circle?>(null) }
    
    // Map status text
    var statusText by remember { mutableStateOf("") }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Render the OSM map
        OSMMapView(
            modifier = Modifier.fillMaxSize(),
            onMapReady = { map ->
                mapView = map
                
                // Connect to view model if provided
                viewModel?.setMapReference(map)
                
                // Set initial map center
                if (userLat != null && userLng != null) {
                    map.setMapCenter(userLat, userLng)
                } else {
                    // Default to Berkeley, CA if user location is not available
                    map.setMapCenter(37.8715, -122.2730)
                }
                
                // Enable location overlay
                val myLocationOverlay = map.enableMyLocation(context)
                
                // Update status text
                val zoom = map.zoomLevelDouble
                statusText = "Zoom: ${String.format("%.1f", zoom)}x"
                
                // Add circle markers
                addCircleMarkers(map, circles, selectedCircle, onCircleClick) { circle ->
                    selectedCircle = circle
                }
                
                // Add zoom listener
                map.setOnScaleListener { _, _, _ ->
                    statusText = "Zoom: ${String.format("%.1f", map.zoomLevelDouble)}x"
                    viewModel?.let { vm ->
                        // Update the viewModel with the new zoom level
                        vm.setMapReference(map)
                    }
                    true
                }
            }
        )
    }
    
    // Update markers when circles change
    LaunchedEffect(circles, selectedCircle) {
        mapView?.let { map ->
            addCircleMarkers(map, circles, selectedCircle, onCircleClick) { circle ->
                selectedCircle = circle
            }
        }
    }
    
    // Update map center when user location changes
    LaunchedEffect(userLat, userLng) {
        if (userLat != null && userLng != null) {
            mapView?.setMapCenter(userLat, userLng)
        }
    }
    
    // Clean up
    DisposableEffect(Unit) {
        onDispose {
            mapView?.onDetach()
        }
    }
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
    // Clear existing markers
    val overlaysToKeep = mapView.overlays.filterNot { it is CircleMarker }
    mapView.overlays.clear()
    mapView.overlays.addAll(overlaysToKeep)
    
    // Add markers for each circle
    circles.forEach { circle ->
        if (circle.locationLat != null && circle.locationLng != null) {
            val marker = CircleMarker(
                osmMapView = mapView,
                circle = circle,
                isSelected = circle == selectedCircle
            )
            
            marker.setOnMarkerClickListener { _, _ ->
                onCircleSelected(circle)
                onCircleClick(circle)
                true
            }
            
            mapView.overlays.add(marker)
        }
    }
    
    // Refresh the map
    mapView.invalidate()
} 