package com.example.myapplication.ui.circles

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.repositories.CircleRepository
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCircleScreen(
    onNavigateBack: () -> Unit,
    onCircleCreated: (String) -> Unit,
    viewModel: CreateCircleViewModel = viewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Form state
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(true) }
    var locationEnabled by remember { mutableStateOf(false) }
    var selectedDuration by remember { mutableLongStateOf(CircleRepository.DURATION_24_HOURS) }
    
    // Location state
    var locationLat by remember { mutableDoubleStateOf(0.0) }
    var locationLng by remember { mutableDoubleStateOf(0.0) }
    var locationRadius by remember { mutableDoubleStateOf(100.0) } // Default 100 meters
    var hasLocation by remember { mutableStateOf(false) }
    var showLocationPermissionDialog by remember { mutableStateOf(false) }
    var isLoadingLocation by remember { mutableStateOf(false) }
    
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
                    locationLat = location.latitude
                    locationLng = location.longitude
                    hasLocation = true
                    isLoadingLocation = false
                    scope.launch {
                        snackbarHostState.showSnackbar("Location acquired: ${location.latitude}, ${location.longitude}")
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
            locationEnabled = false
            scope.launch {
                snackbarHostState.showSnackbar("Location permission denied")
            }
        }
    }
    
    // Duration options
    val durationOptions = listOf(
        Pair("1 hour", CircleRepository.DURATION_1_HOUR),
        Pair("24 hours", CircleRepository.DURATION_24_HOURS),
        Pair("48 hours", CircleRepository.DURATION_48_HOURS),
        Pair("72 hours", CircleRepository.DURATION_72_HOURS),
        Pair("7 days", CircleRepository.DURATION_7_DAYS)
    )
    
    // Observe created circle ID
    LaunchedEffect(viewModel.createdCircleId) {
        viewModel.createdCircleId?.let { circleId ->
            onCircleCreated(circleId)
        }
    }
    
    // Observe error messages
    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }
    
    // Location permission dialog
    if (showLocationPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showLocationPermissionDialog = false },
            title = { Text("Location Permission") },
            text = { Text("This app needs access to your location to create location-based Circles. Your location will only be used when you explicitly enable it for a Circle.") },
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
                    onClick = { 
                        showLocationPermissionDialog = false
                        locationEnabled = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Circle") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Circle Name") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = name.isBlank() && viewModel.hasAttemptedSubmit,
                    supportingText = {
                        if (name.isBlank() && viewModel.hasAttemptedSubmit) {
                            Text(
                                text = "Name is required",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Circle Duration",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Column(
                    modifier = Modifier
                        .selectableGroup()
                        .fillMaxWidth()
                ) {
                    durationOptions.forEach { (label, duration) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .selectable(
                                    selected = selectedDuration == duration,
                                    onClick = { selectedDuration = duration },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedDuration == duration,
                                onClick = null // null because we're handling the click on the row
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isPrivate) Icons.Default.Check else Icons.Default.Public,
                            contentDescription = null
                        )
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Private Circle",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Only invited members can join",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        Switch(
                            checked = isPrivate,
                            onCheckedChange = { isPrivate = it }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Enable Location",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Switch(
                        checked = locationEnabled,
                        onCheckedChange = { enabled ->
                            locationEnabled = enabled
                            
                            if (enabled && !hasLocation) {
                                // Check if we have permission
                                if (ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    // We have permission, get location
                                    isLoadingLocation = true
                                    getCurrentLocation(
                                        fusedLocationClient = fusedLocationClient,
                                        onLocationResult = { location ->
                                            locationLat = location.latitude
                                            locationLng = location.longitude
                                            hasLocation = true
                                            isLoadingLocation = false
                                        },
                                        onError = { errorMessage ->
                                            isLoadingLocation = false
                                            locationEnabled = false
                                            scope.launch {
                                                snackbarHostState.showSnackbar(errorMessage)
                                            }
                                        }
                                    )
                                } else {
                                    // Need to request permission
                                    showLocationPermissionDialog = true
                                }
                            }
                        }
                    )
                }
                
                if (locationEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            if (isLoadingLocation) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text("Getting location...")
                                }
                            } else if (hasLocation) {
                                Text(
                                    text = "Location",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = "Latitude: $locationLat\nLongitude: $locationLng",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                    text = "Radius: ${locationRadius.toInt()} meters",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                
                                Slider(
                                    value = locationRadius.toFloat(),
                                    onValueChange = { locationRadius = it.toDouble() },
                                    valueRange = 50f..500f,
                                    steps = 9,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Button(
                                    onClick = {
                                        // Request a location update
                                        isLoadingLocation = true
                                        getCurrentLocation(
                                            fusedLocationClient = fusedLocationClient,
                                            onLocationResult = { location ->
                                                locationLat = location.latitude
                                                locationLng = location.longitude
                                                hasLocation = true
                                                isLoadingLocation = false
                                            },
                                            onError = { errorMessage ->
                                                isLoadingLocation = false
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(errorMessage)
                                                }
                                            }
                                        )
                                    },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MyLocation,
                                        contentDescription = "Update Location"
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Update Location")
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = "Location not available. Please enable location services.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = {
                        viewModel.createCircle(
                            name = name,
                            description = description,
                            durationMillis = selectedDuration,
                            isPrivate = isPrivate,
                            locationEnabled = locationEnabled,
                            locationLat = if (locationEnabled && hasLocation) locationLat else null,
                            locationLng = if (locationEnabled && hasLocation) locationLng else null,
                            locationRadius = if (locationEnabled && hasLocation) locationRadius else null
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.isLoading && (!locationEnabled || (locationEnabled && hasLocation))
                ) {
                    if (viewModel.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Create Circle")
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun getCurrentLocation(
    fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient,
    onLocationResult: (Location) -> Unit,
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