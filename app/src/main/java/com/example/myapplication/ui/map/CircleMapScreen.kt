package com.example.myapplication.ui.map

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.models.Circle
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.example.myapplication.ui.map.osm.OSMMapComponent

/**
 * A dedicated map screen for viewing and interacting with Circles
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleMapScreen(
    onCircleClick: (Circle) -> Unit,
    onCreateCircle: () -> Unit,
    onFilterChange: (String) -> Unit,
    onBackClick: () -> Unit,
    viewModel: CircleMapViewModel = viewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Filter state
    var selectedFilter by remember { mutableStateOf("All") }
    val filterOptions = listOf("All", "Active", "Upcoming", "Public", "Private", "Party", "Study")
    
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
        onFilterChange(selectedFilter)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Explore Circles") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadNearbyCircles() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
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
                        onClick = { 
                            selectedFilter = filter
                        },
                        label = { Text(filter) },
                        leadingIcon = if (selectedFilter == filter) {
                            { Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }
            
            // Map view
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    OSMMapComponent(
                        circles = viewModel.circles,
                        userLat = viewModel.userLat,
                        userLng = viewModel.userLng,
                        onCircleClick = onCircleClick,
                        modifier = Modifier.fillMaxSize(),
                        viewModel = viewModel
                    )
                }
                
                // Map controls overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    FloatingActionButton(
                        onClick = { viewModel.zoomIn() },
                        modifier = Modifier.size(40.dp),
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(20.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    FloatingActionButton(
                        onClick = { viewModel.zoomOut() },
                        modifier = Modifier.size(40.dp),
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(20.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    FloatingActionButton(
                        onClick = { viewModel.requestLocationUpdate() },
                        modifier = Modifier.size(40.dp),
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "My Location", modifier = Modifier.size(20.dp))
                    }
                }
            }
            
            // Circle count indicator
            Text(
                text = "${viewModel.circles.size} Circles found",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
} 