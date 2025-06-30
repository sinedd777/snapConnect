package com.example.myapplication.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.models.Circle
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.horizontalScroll
import com.example.myapplication.ui.map.osm.OSMMapComponent
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.util.Log
import com.composables.core.ScrollArea
import com.composables.core.VerticalScrollbar
import com.composables.core.Thumb
import com.composables.core.rememberScrollAreaState
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.delay
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.ripple.rememberRipple
import androidx.navigation.NavController
import com.example.myapplication.navigation.Destinations

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenCamera: () -> Unit,
    onOpenSnapViewer: (String) -> Unit,
    onOpenFriends: () -> Unit,
    onOpenCircles: () -> Unit,
    onOpenProfile: () -> Unit,
    onCreateCircle: () -> Unit,
    onOpenMap: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
    navController: NavController
) {
    val TAG = "HomeScreen"
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Sort state
    var selectedSort by remember { mutableStateOf("Distance") }
    val sortOptions = listOf("Distance", "Members", "Time")
    
    // Fullscreen map state
    var isMapFullscreen by remember { mutableStateOf(false) }
    
    
    // Animation values
    val mapHeight by animateDpAsState(
        targetValue = if (isMapFullscreen) 1000.dp else 240.dp,
        animationSpec = tween(300),
        label = "mapHeight"
    )
    
    val mapCornerRadius by animateDpAsState(
        targetValue = if (isMapFullscreen) 0.dp else 16.dp,
        animationSpec = tween(300),
        label = "mapCornerRadius"
    )
    
    val mapPadding by animateDpAsState(
        targetValue = if (isMapFullscreen) 0.dp else 16.dp,
        animationSpec = tween(300),
        label = "mapPadding"
    )
    
    val contentAlpha by animateFloatAsState(
        targetValue = if (isMapFullscreen) 0f else 1f,
        animationSpec = tween(300),
        label = "contentAlpha"
    )
    
    // Location permission handling
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (fineLocationGranted || coarseLocationGranted) {
            // Permission granted, update location
            viewModel.isLocationPermissionGranted = true
            viewModel.requestLocationUpdate(context)
        } else {
            // Permission denied, show error
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Location permission denied. Some features may not work correctly.")
            }
            viewModel.isLocationPermissionGranted = false
        }
    }
    
    // Check location permissions on start
    LaunchedEffect(Unit) {
        val fineLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarseLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        if (fineLocationPermission == PackageManager.PERMISSION_GRANTED ||
            coarseLocationPermission == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted
            viewModel.isLocationPermissionGranted = true
            viewModel.requestLocationUpdate(context)
        } else {
            // Request permission
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
    
    // Debug: Track circles state changes
    LaunchedEffect(viewModel.circles) {
        Log.d(TAG, "Circles state updated: ${viewModel.circles.size} circles")
        viewModel.circles.forEach { circle ->
            Log.d(TAG, """Circle details:
                |ID: ${circle.id}
                |Name: ${circle.name}
                |Location: (${circle.locationLat}, ${circle.locationLng})
                |Private: ${circle.private}
                |Category: ${circle.category}
                |Members: ${circle.members.size}
                |Location Enabled: ${circle.locationEnabled}""".trimMargin())
        }
    }

    // Debug: Track location updates
    LaunchedEffect(viewModel.userLat, viewModel.userLng) {
        Log.d(TAG, "Location updated - Lat: ${viewModel.userLat}, Lng: ${viewModel.userLng}")
    }

    // Debug: Track loading state
    LaunchedEffect(viewModel.isLoading) {
        Log.d(TAG, "Loading state changed: ${viewModel.isLoading}")
    }
    
    // Load data with debug logging
    LaunchedEffect(Unit) {
        Log.d(TAG, "Initial data load triggered")
        viewModel.loadNearbyCircles()
    }
    
    // Error handling with debug logging
    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let { error ->
            Log.e(TAG, "Error occurred: $error")
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }
    
    // Filter change handler with debug logging
    LaunchedEffect(selectedSort) {
        Log.d(TAG, "Filter changed to: $selectedSort")
        when (selectedSort) {
            "Distance" -> viewModel.sortCirclesByDistance()
            "Members" -> viewModel.sortCirclesByMembers()
            "Time" -> viewModel.sortCirclesByTimeToExpiry()
        }
    }
    
    // Function to handle circle selection
    val onOpenCircleDetail = { circleId: String ->
        // Navigate to circle detail screen
        onOpenCircles()
        navController.navigate(Destinations.circleDetail(circleId))
    }
    
    
    // Handle bottom sheet dismissal
    
    Scaffold(
        topBar = {
            if (!isMapFullscreen) {
                TopAppBar(
                    title = { Text("SnapCircle") },
                    actions = {
                        // Location refresh button
                        IconButton(
                            onClick = { 
                                if (viewModel.isLocationPermissionGranted) {
                                    viewModel.requestLocationUpdate(context)
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            }
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = "Update Location")
                        }
                        
                        IconButton(onClick = { viewModel.loadNearbyCircles() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                        IconButton(onClick = onOpenProfile) {
                            Icon(Icons.Default.Person, contentDescription = "Profile")
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (!isMapFullscreen) {
                NavigationBar {
                    NavigationBarItem(
                        selected = true,
                        onClick = { /* Already on home */ },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = onOpenCamera,
                        icon = { Icon(Icons.Default.AddCircle, contentDescription = "Create") },
                        label = { Text("Create") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = onOpenCircles,
                        icon = { Icon(Icons.Default.Groups, contentDescription = "Circles") },
                        label = { Text("Circles") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = onOpenFriends,
                        icon = { Icon(Icons.Default.People, contentDescription = "Friends") },
                        label = { Text("Friends") }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isMapFullscreen) PaddingValues(0.dp) else padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Map header
                if (!isMapFullscreen) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Nearby Circles",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                
                // Map preview
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(mapHeight)
                        .padding(horizontal = mapPadding)
                        .clip(RoundedCornerShape(mapCornerRadius))
                        .zIndex(1f),
                    shape = RoundedCornerShape(mapCornerRadius)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (viewModel.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            OSMMapComponent(
                                circles = viewModel.circles.also { 
                                    Log.d(TAG, "Rendering map with ${it.size} circles")
                                    it.forEach { circle ->
                                        Log.d(TAG, "Rendering circle on map - ID: ${circle.id}, Location: (${circle.locationLat}, ${circle.locationLng})")
                                    }
                                },
                                userLat = viewModel.userLat,
                                userLng = viewModel.userLng,
                                onCircleClick = { circle -> 
                                    Log.d(TAG, "Circle clicked on map - ID: ${circle.id}")
                                    onOpenCircleDetail(circle.id)
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        
                        // Location button overlay
                        if (!viewModel.isLoading) {
                            FloatingActionButton(
                                onClick = { 
                                    if (viewModel.isLocationPermissionGranted) {
                                        viewModel.requestLocationUpdate(context)
                                    } else {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(40.dp),
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                Icon(
                                    Icons.Default.MyLocation,
                                    contentDescription = "My Location",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            // Full map button
                            FloatingActionButton(
                                onClick = { isMapFullscreen = !isMapFullscreen },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .size(40.dp),
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                Icon(
                                    if (isMapFullscreen) Icons.Default.CloseFullscreen else Icons.Default.OpenInFull,
                                    contentDescription = if (isMapFullscreen) "Exit Full Screen" else "View Full Map",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        
                        // Back button when in fullscreen mode
                        if (isMapFullscreen) {
                            FloatingActionButton(
                                onClick = { isMapFullscreen = false },
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                                    .size(40.dp),
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                Icon(
                                    Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                
                // Content below map (only visible when not fullscreen)
                if (!isMapFullscreen) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f) // Take remaining space in parent Column
                    ) {
                        // Sort chips with icon
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Sort icon
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Sort",
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(end = 4.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            
                            sortOptions.forEach { sort ->
                                FilterChip(
                                    selected = selectedSort == sort,
                                    onClick = { 
                                        selectedSort = sort
                                        when (sort) {
                                            "Distance" -> viewModel.sortCirclesByDistance()
                                            "Members" -> viewModel.sortCirclesByMembers()
                                            "Time" -> viewModel.sortCirclesByTimeToExpiry()
                                        }
                                    },
                                    label = { Text(sort) },
                                    leadingIcon = if (selectedSort == sort) {
                                        { Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }

                        // Circles list
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            if (viewModel.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            } else if (viewModel.circles.isEmpty()) {
                                Column(
                                    modifier = Modifier.align(Alignment.Center),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "No circles found nearby",
                                        style = MaterialTheme.typography.bodyLarge,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                val lazyListState = rememberLazyListState()
                                val scrollAreaState = rememberScrollAreaState(lazyListState)

                                ScrollArea(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(top = 4.dp),
                                    state = scrollAreaState
                                ) {
                                    LazyColumn(
                                        state = lazyListState,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        items(viewModel.circles) { circle ->
                                            CircleListItem(
                                                circle = circle,
                                                onClick = { onOpenCircleDetail(circle.id) }
                                            )
                                        }
                                    }

                                    VerticalScrollbar(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .fillMaxHeight()
                                            .padding(4.dp)
                                            .width(8.dp)
                                    ) {
                                        Thumb(
                                            modifier = Modifier
                                                .background(
                                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                                    RoundedCornerShape(4.dp)
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
        }
    }
}

@Composable
fun CircleListItem(
    circle: Circle,
    onClick: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel()
    
    // Calculate distance if both user and circle locations are available
    val distance = remember(circle, viewModel.userLat, viewModel.userLng) {
        if (circle.locationEnabled && circle.locationLat != null && circle.locationLng != null) {
            calculateDistance(
                viewModel.userLat,
                viewModel.userLng,
                circle.locationLat,
                circle.locationLng
            )
        } else null
    }
    
    // Animation states
    val animatedOffset by animateDpAsState(
        targetValue = if (isExpanded) 12.dp else 0.dp,
        animationSpec = tween(300),
        label = "textOffset"
    )
    
    // Marquee animation
    val textScrollState = rememberScrollState()
    
    // Reset scroll when collapsed
    LaunchedEffect(isExpanded) {
        if (!isExpanded) {
            textScrollState.scrollTo(0)
        }
    }
    
    // Start marquee animation when expanded
    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            while (true) {
                delay(1000)
                textScrollState.animateScrollTo(
                    value = textScrollState.maxValue,
                    animationSpec = tween(
                        durationMillis = 3000,
                        easing = LinearEasing
                    )
                )
                delay(1000)
                textScrollState.animateScrollTo(
                    value = 0,
                    animationSpec = tween(
                        durationMillis = 500,
                        easing = LinearEasing
                    )
                )
                delay(500)
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { isExpanded = !isExpanded },
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (isExpanded) 2.dp else 1.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Main content row with animated offset
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(x = animatedOffset),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Name and description in single line
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(
                            state = if (isExpanded) textScrollState else rememberScrollState(),
                            enabled = isExpanded
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = circle.name,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (circle.private) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Private",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    if (!circle.description.isNullOrBlank()) {
                        Text(
                            text = " • ",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = circle.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                        if (isExpanded) {
                            Spacer(modifier = Modifier.width(16.dp))
                        }
                    }
                }
            }
            
            // Expandable details
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Time till expiry
                    Text(
                        text = getTimeMessage(circle),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.graphicsLayer(alpha = 0.9f)
                        .padding(horizontal = 8.dp, vertical = 4.dp)  // Added padding here
                    )

                    // Right side content
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Member count
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.graphicsLayer(alpha = 0.8f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = "Members",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${circle.members.size}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Location info if enabled
                        if (circle.locationEnabled && circle.locationLat != null && circle.locationLng != null) {
                            Spacer(modifier = Modifier.width(16.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .graphicsLayer(alpha = 0.8f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = rememberRipple(bounded = true)
                                    ) {
                                        // Open Google Maps navigation
                                        val uri = Uri.parse("google.navigation:q=${circle.locationLat},${circle.locationLng}")
                                        val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                                            setPackage("com.google.android.apps.maps")
                                        }
                                        if (mapIntent.resolveActivity(context.packageManager) != null) {
                                            context.startActivity(mapIntent)
                                        } else {
                                            // Fallback to browser if Google Maps isn't installed
                                            val browserUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${circle.locationLat},${circle.locationLng}")
                                            val browserIntent = Intent(Intent.ACTION_VIEW, browserUri)
                                            context.startActivity(browserIntent)
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Distance",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = formatDistance(distance),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper function to get time-related message
fun getTimeMessage(circle: Circle): String {
    val now = System.currentTimeMillis()
    
    // Check if the circle has a start time in the future
    if (circle.startTime != null && circle.startTime.toDate().time > now) {
        val diffMillis = circle.startTime.toDate().time - now
        val diffMinutes = diffMillis / (1000 * 60)
        
        return when {
            diffMinutes < 60 -> "Starting in ${diffMinutes.toInt()} minutes"
            diffMinutes < 24 * 60 -> "Starting in ${(diffMinutes / 60).toInt()} hours"
            else -> "Starting on ${SimpleDateFormat("MMM d", Locale.getDefault()).format(circle.startTime.toDate())}"
        }
    }
    
    // Check if the circle has an expiration time
    if (circle.expiresAt != null) {
        val diffMillis = circle.expiresAt.toDate().time - now
        
        // If already expired
        if (diffMillis <= 0) {
            return "Expired"
        }
        
        val diffMinutes = diffMillis / (1000 * 60)
        return when {
            diffMinutes < 60 -> "Ends in ${diffMinutes.toInt()} minutes"
            diffMinutes < 24 * 60 -> "Ends in ${(diffMinutes / 60).toInt()} hours"
            else -> "Ends on ${SimpleDateFormat("MMM d", Locale.getDefault()).format(circle.expiresAt.toDate())}"
        }
    }
    
    // If no start time or expiration time
    return "Active now"
}

// Function to calculate distance between two points using Haversine formula
private fun calculateDistance(lat1: Double?, lon1: Double?, lat2: Double, lon2: Double): Double? {
    if (lat1 == null || lon1 == null) return null
    
    val r = 6371 // Earth's radius in kilometers
    
    val lat1Rad = Math.toRadians(lat1)
    val lat2Rad = Math.toRadians(lat2)
    val deltaLat = Math.toRadians(lat2 - lat1)
    val deltaLon = Math.toRadians(lon2 - lon1)
    
    val a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
            Math.cos(lat1Rad) * Math.cos(lat2Rad) *
            Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2)
    
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    
    return r * c
}

// Function to format distance for display
private fun formatDistance(distanceKm: Double?): String {
    if (distanceKm == null) return "-- km"
    return when {
        distanceKm < 1 -> "${(distanceKm * 1000).toInt()}m"
        else -> "%.1f km".format(distanceKm)
    }
} 