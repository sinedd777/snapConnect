package com.example.myapplication.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.outlined.Camera
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.material3.BadgedBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.models.Circle
import com.example.myapplication.data.repositories.CircleRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.horizontalScroll
import com.example.myapplication.ui.map.osm.OSMMapComponent
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

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
    viewModel: HomeViewModel = viewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Filter state
    var selectedFilter by remember { mutableStateOf("All") }
    val filterOptions = listOf("All", "Active", "Upcoming", "Public", "Private")
    
    // Fullscreen map state
    var isMapFullscreen by remember { mutableStateOf(false) }
    
    // Bottom sheet state
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var showBottomSheet by remember { mutableStateOf(false) }
    
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
    
    // Load data
    LaunchedEffect(Unit) {
        viewModel.loadNearbyCircles()
    }
    
    // Error handling
    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }
    
    // Filter change handler
    LaunchedEffect(selectedFilter) {
        viewModel.setFilter(selectedFilter)
    }
    
    // Function to handle circle selection
    val onOpenCircleDetail = { circleId: String ->
        // Navigate to circle detail screen
        onOpenSnapViewer(circleId) // For now, reuse the snap viewer navigation
    }
    
    // Show bottom sheet when circles are loaded and not in fullscreen mode
    LaunchedEffect(viewModel.circles, isMapFullscreen) {
        if (viewModel.circles.isNotEmpty() && !isMapFullscreen) {
            showBottomSheet = true
        } else {
            showBottomSheet = false
        }
    }
    
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
                        onClick = onOpenCircles,
                        icon = { Icon(Icons.Default.Groups, contentDescription = "Circles") },
                        label = { Text("Circles") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = onOpenCamera,
                        icon = { Icon(Icons.Default.AddCircle, contentDescription = "Create") },
                        label = { Text("Create") }
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
        floatingActionButton = {
            if (!isMapFullscreen) {
                FloatingActionButton(
                    onClick = onCreateCircle,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Circle")
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
                                circles = viewModel.circles,
                                userLat = viewModel.userLat,
                                userLng = viewModel.userLng,
                                onCircleClick = { /* Handle in full map view */ },
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
                    // Filter chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filterOptions.forEach { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text(filter) },
                                leadingIcon = if (selectedFilter == filter) {
                                    { Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                    
                    // Empty state if no circles
                    if (viewModel.circles.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            if (viewModel.isLoading) {
                                CircularProgressIndicator()
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "No circles found nearby",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(onClick = onCreateCircle) {
                                        Text("Create a Circle")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Bottom Sheet for Circles
            if (showBottomSheet && !isMapFullscreen && viewModel.circles.isNotEmpty()) {
                ModalBottomSheet(
                    onDismissRequest = { showBottomSheet = false },
                    sheetState = bottomSheetState,
                    dragHandle = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Drag handle
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(4.dp)
                                    .background(
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        RoundedCornerShape(2.dp)
                                    )
                                    .padding(vertical = 16.dp)
                            )
                            
                            // Title
                            Text(
                                text = "Nearby Circles",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 32.dp), // Extra padding at the bottom
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(viewModel.circles) { circle ->
                            CircleBottomSheetItem(
                                circle = circle,
                                onClick = { onOpenCircleDetail(circle.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CircleBottomSheetItem(
    circle: Circle,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (circle.isPrivate) 
                MaterialTheme.colorScheme.secondaryContainer 
            else 
                MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circle icon with category-based icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (circle.isPrivate) 
                            MaterialTheme.colorScheme.secondary 
                        else 
                            MaterialTheme.colorScheme.primary
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (circle.category) {
                        "Party" -> Icons.Default.Celebration
                        "Study" -> Icons.Default.School
                        "Sports" -> Icons.Default.SportsSoccer
                        "Food" -> Icons.Default.Restaurant
                        "Music" -> Icons.Default.MusicNote
                        else -> Icons.Default.Groups
                    },
                    contentDescription = null,
                    tint = if (circle.isPrivate) 
                        MaterialTheme.colorScheme.onSecondary 
                    else 
                        MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Circle info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = circle.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Time-related message
                val timeMessage = getTimeMessage(circle)
                Text(
                    text = timeMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        timeMessage.contains("Starting") -> MaterialTheme.colorScheme.tertiary
                        timeMessage.contains("Ends") -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Participants count
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${circle.members.size} participants",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (circle.locationEnabled && circle.locationRadius != null) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${circle.locationRadius.toInt()}m radius",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Privacy indicator and action button
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.height(56.dp)
            ) {
                Icon(
                    imageVector = if (circle.isPrivate) Icons.Default.Lock else Icons.Default.Public,
                    contentDescription = if (circle.isPrivate) "Private" else "Public",
                    tint = if (circle.isPrivate) 
                        MaterialTheme.colorScheme.onSecondaryContainer 
                    else 
                        MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                FilledTonalIconButton(
                    onClick = onClick,
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = if (circle.isPrivate) 
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f) 
                        else 
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "View Circle",
                        modifier = Modifier.size(16.dp)
                    )
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (circle.isPrivate) 
                MaterialTheme.colorScheme.secondaryContainer 
            else 
                MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circle icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (circle.isPrivate) 
                            MaterialTheme.colorScheme.secondary 
                        else 
                            MaterialTheme.colorScheme.primary
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = circle.name.first().toString(),
                    color = if (circle.isPrivate) 
                        MaterialTheme.colorScheme.onSecondary 
                    else 
                        MaterialTheme.colorScheme.onPrimary
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Circle info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = circle.name,
                    style = MaterialTheme.typography.titleMedium
                )
                
                Text(
                    text = "${circle.members.size} members" + 
                        if (circle.locationEnabled && circle.locationRadius != null) 
                            " • ${circle.locationRadius.toInt()}m radius" 
                        else "",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // Privacy indicator
            if (circle.isPrivate) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Private",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            } else {
                Icon(
                    Icons.Default.Public,
                    contentDescription = "Public",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
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