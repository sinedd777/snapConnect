package com.example.myapplication.ui.map.osm

import android.content.Context
import android.os.Bundle
import android.util.Log
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

private const val TAG = "OSMMapView"

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
    
    Log.d(TAG, "OSMMapView composition started")
    
    // Configure OSMDroid
    LaunchedEffect(Unit) {
        try {
            Log.d(TAG, "Initializing OSMDroid configuration")
            Configuration.getInstance().apply {
                load(context, PreferenceManager.getDefaultSharedPreferences(context))
                userAgentValue = context.packageName
                osmdroidBasePath = context.filesDir
                osmdroidTileCache = context.cacheDir
                Log.d(TAG, """
                    OSMDroid base configuration:
                    - Base path: $osmdroidBasePath
                    - Cache path: $osmdroidTileCache
                    - User agent: $userAgentValue
                """.trimIndent())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure OSMDroid", e)
        }
    }
    
    // Create and remember the MapView
    val mapView = remember {
        try {
            Log.d(TAG, "Creating new MapView instance")
            MapView(context).apply {
                Log.d(TAG, "Configuring MapView basic settings")
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(15.0)
                setHorizontalMapRepetitionEnabled(false)
                setVerticalMapRepetitionEnabled(false)
                isTilesScaledToDpi = true
                
                // Set default center to a known location (will be updated later)
                controller.setCenter(GeoPoint(0.0, 0.0))
                
                Log.d(TAG, """
                    MapView initialized with:
                    - Tile source: ${tileProvider?.tileSource?.name()}
                    - Zoom level: ${zoomLevel}
                    - Center: ${mapCenter?.latitude}, ${mapCenter?.longitude}
                    - Tile provider: ${tileProvider?.javaClass?.simpleName}
                """.trimIndent())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MapView", e)
            throw e
        }
    }
    
    // Handle lifecycle events
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    try {
                        Log.d(TAG, "Lifecycle ON_RESUME: Resuming MapView")
                        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
                        mapView.onResume()
                        Log.d(TAG, "MapView resumed successfully")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error resuming MapView", e)
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    try {
                        Log.d(TAG, "Lifecycle ON_PAUSE: Pausing MapView")
                        mapView.onPause()
                        Log.d(TAG, "MapView paused successfully")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error pausing MapView", e)
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    Log.d(TAG, "Lifecycle ON_DESTROY event received")
                }
                else -> {
                    Log.d(TAG, "Lifecycle event: $event")
                }
            }
        }
        
        Log.d(TAG, "Adding lifecycle observer")
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            try {
                Log.d(TAG, "Disposing MapView")
                lifecycleOwner.lifecycle.removeObserver(observer)
                mapView.onDetach()
                Log.d(TAG, "MapView disposed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error disposing MapView", e)
            }
        }
    }
    
    // Render the MapView
    AndroidView(
        factory = { 
            Log.d(TAG, "AndroidView factory called for MapView")
            mapView 
        },
        modifier = modifier,
        update = { map ->
            try {
                Log.d(TAG, "Executing MapView update block")
                onMapReady(map)
                Log.d(TAG, "MapView ready callback executed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error in MapView ready callback", e)
            }
        }
    )
    
    // Render any additional content
    content(mapView)
    
    Log.d(TAG, "OSMMapView composition completed")
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