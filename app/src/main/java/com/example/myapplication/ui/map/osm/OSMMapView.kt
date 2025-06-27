package com.example.myapplication.ui.map.osm

import android.content.Context
import android.os.Bundle
import androidx.preference.PreferenceManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import android.view.ScaleGestureDetector

/**
 * A Jetpack Compose wrapper for the OpenStreetMap MapView
 */
@Composable
fun OSMMapView(
    modifier: Modifier = Modifier,
    onMapReady: (MapView) -> Unit = {},
    content: @Composable (MapView) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Configure OSMDroid
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            PreferenceManager.getDefaultSharedPreferences(context)
        )
    }
    
    // Create and remember the MapView
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
        }
    }
    
    // Handle lifecycle events
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }
    
    // Render the MapView
    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { map ->
            onMapReady(map)
        }
    )
    
    // Render any additional content
    content(mapView)
}

/**
 * Add a marker to the map
 */
fun MapView.addMarker(
    geoPoint: GeoPoint,
    title: String? = null,
    snippet: String? = null,
    icon: org.osmdroid.views.overlay.Marker.OnMarkerClickListener? = null
): Marker {
    val marker = Marker(this)
    marker.position = geoPoint
    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
    
    if (title != null) {
        marker.title = title
    }
    
    if (snippet != null) {
        marker.snippet = snippet
    }
    
    if (icon != null) {
        marker.setOnMarkerClickListener(icon)
    }
    
    overlays.add(marker)
    invalidate()
    
    return marker
}

/**
 * Enable showing the user's location on the map
 */
fun MapView.enableMyLocation(context: Context): MyLocationNewOverlay {
    val myLocationProvider = GpsMyLocationProvider(context)
    val myLocationOverlay = MyLocationNewOverlay(myLocationProvider, this)
    myLocationOverlay.enableMyLocation()
    myLocationOverlay.enableFollowLocation()
    myLocationOverlay.isDrawAccuracyEnabled = true
    overlays.add(myLocationOverlay)
    invalidate()
    
    return myLocationOverlay
}

/**
 * Set the map center position
 */
fun MapView.setMapCenter(latitude: Double, longitude: Double, zoom: Double = 15.0) {
    controller.setCenter(GeoPoint(latitude, longitude))
    controller.setZoom(zoom)
}

/**
 * Set a listener for scale (zoom) gestures
 */
fun MapView.setOnScaleListener(listener: (ScaleGestureDetector, Float, Float) -> Boolean) {
    // Create a scale gesture detector
    val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            return listener(detector, detector.scaleFactor, this@setOnScaleListener.zoomLevelDouble.toFloat())
        }
    })
    
    // Set a touch listener that will handle scale gestures
    setOnTouchListener { v, event ->
        // Let the scale detector process the event first
        scaleDetector.onTouchEvent(event)
        
        // Return false to allow the event to be processed by the map
        false
    }
} 