package com.example.myapplication.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
    
    // Filter state
    var selectedFilter by remember { mutableStateOf("All") }
    val filterOptions = listOf("All", "Active", "Upcoming", "Public", "Private")
    
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
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SnapCircle") },
                actions = {
                    IconButton(onClick = { viewModel.loadNearbyCircles() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onOpenProfile) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                }
            )
        },
        bottomBar = {
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateCircle,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Circle")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Map header
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
                
                TextButton(onClick = onOpenMap) {
                    Text("View Full Map")
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            // Map preview
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (viewModel.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        MapComponent(
                            circles = viewModel.circles,
                            userLat = viewModel.userLat,
                            userLng = viewModel.userLng,
                            onCircleClick = { /* Handle in full map view */ },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    
                    // Map overlay
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "${viewModel.circles.size} Circles nearby",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        if (viewModel.collegeTown != null) {
                            Text(
                                text = "College Town: ${viewModel.collegeTown}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
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
            
            // Circle list
            Text(
                text = "Recent Circles",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
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
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(viewModel.circles) { circle ->
                        CircleListItem(
                            circle = circle,
                            onClick = { onOpenCircleDetail(circle.id) }
                        )
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