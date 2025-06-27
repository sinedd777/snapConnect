package com.example.myapplication.ui.auth

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollegeTownSelectionScreen(
    onCollegeTownSelected: (String) -> Unit,
    onBackPressed: () -> Unit,
    viewModel: CollegeTownViewModel = viewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // State
    var searchQuery by remember { mutableStateOf("") }
    var selectedTown by remember { mutableStateOf<String?>(null) }
    var showLocationPermissionDialog by remember { mutableStateOf(false) }
    var isLoadingLocation by remember { mutableStateOf(false) }
    
    // Location services
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    // Permission request handler
    val requestLocationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, get location
            getCurrentLocation(
                fusedLocationClient = fusedLocationClient,
                onLocationResult = { location ->
                    isLoadingLocation = false
                    viewModel.detectCollegeTown(
                        location.latitude,
                        location.longitude
                    ) { detectedTown ->
                        if (detectedTown != null) {
                            selectedTown = detectedTown
                            scope.launch {
                                snackbarHostState.showSnackbar("Detected college town: $detectedTown")
                            }
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("No college town detected near your location")
                            }
                        }
                    }
                },
                onError = { errorMessage ->
                    isLoadingLocation = false
                    scope.launch {
                        snackbarHostState.showSnackbar(errorMessage)
                    }
                }
            )
        } else {
            // Permission denied
            isLoadingLocation = false
            scope.launch {
                snackbarHostState.showSnackbar("Location permission denied")
            }
        }
    }
    
    // Filtered towns based on search
    val filteredTowns = remember(searchQuery, viewModel.collegeTowns) {
        if (searchQuery.isBlank()) {
            viewModel.collegeTowns
        } else {
            viewModel.collegeTowns.filter { it.contains(searchQuery, ignoreCase = true) }
        }
    }
    
    // Error handling
    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }
    
    // Load current college town if available
    LaunchedEffect(Unit) {
        viewModel.getCurrentCollegeTown { town ->
            town?.let { selectedTown = it }
        }
    }
    
    // Location permission dialog
    if (showLocationPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showLocationPermissionDialog = false },
            title = { Text("Location Permission") },
            text = { Text("This app needs access to your location to detect your college town. Your location will only be used when you explicitly enable it.") },
            confirmButton = {
                Button(
                    onClick = {
                        showLocationPermissionDialog = false
                        isLoadingLocation = true
                        requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLocationPermissionDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Your College Town") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (selectedTown != null) {
                ExtendedFloatingActionButton(
                    onClick = { 
                        viewModel.saveCollegeTown(selectedTown!!) {
                            onCollegeTownSelected(selectedTown!!)
                        }
                    },
                    icon = { Icon(Icons.Default.Check, contentDescription = null) },
                    text = { Text("Continue") },
                    containerColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search for your college town") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )
            
            // Current location option
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // Check if we already have location permission
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            isLoadingLocation = true
                            getCurrentLocation(
                                fusedLocationClient = fusedLocationClient,
                                onLocationResult = { location ->
                                    isLoadingLocation = false
                                    viewModel.detectCollegeTown(
                                        location.latitude,
                                        location.longitude
                                    ) { detectedTown ->
                                        if (detectedTown != null) {
                                            selectedTown = detectedTown
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Detected college town: $detectedTown")
                                            }
                                        } else {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("No college town detected near your location")
                                            }
                                        }
                                    }
                                },
                                onError = { errorMessage ->
                                    isLoadingLocation = false
                                    scope.launch {
                                        snackbarHostState.showSnackbar(errorMessage)
                                    }
                                }
                            )
                        } else {
                            showLocationPermissionDialog = true
                        }
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLoadingLocation) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Use Current Location",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Automatically detect your college town",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Popular college towns
            Text(
                text = "College Towns",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            if (viewModel.isLoading && viewModel.collegeTowns.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTowns) { town ->
                        CollegeTownItem(
                            town = town,
                            isSelected = town == selectedTown,
                            onClick = { selectedTown = town }
                        )
                    }
                    
                    if (filteredTowns.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No matching college towns found.\nTry a different search term.",
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CollegeTownItem(
    town: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = town,
                style = MaterialTheme.typography.bodyLarge
            )
            
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun getCurrentLocation(
    fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient,
    onLocationResult: (android.location.Location) -> Unit,
    onError: (String) -> Unit
) {
    try {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    onLocationResult(location)
                } else {
                    onError("Location not available. Please try again later.")
                }
            }
            .addOnFailureListener { e ->
                onError("Failed to get location: ${e.message}")
            }
    } catch (e: Exception) {
        onError("Error accessing location: ${e.message}")
    }
} 