package com.example.myapplication.ui.circles

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
            // Permission granted, get current location
            getCurrentLocation(
                fusedLocationClient = fusedLocationClient,
                onLocationResult = { location ->
                    locationLat = location.latitude
                    locationLng = location.longitude
                    hasLocation = true
                    isLoadingLocation = false
                },
                onError = { error ->
                    scope.launch {
                        snackbarHostState.showSnackbar("Failed to get location: $error")
                    }
                    isLoadingLocation = false
                }
            )
        } else {
            // Permission denied
            scope.launch {
                snackbarHostState.showSnackbar("Location permission denied")
            }
            isLoadingLocation = false
        }
    }
    
    val durationOptions = listOf(
        DurationOption("1 hour", CircleRepository.DURATION_1_HOUR),
        DurationOption("24 hours", CircleRepository.DURATION_24_HOURS),
        DurationOption("48 hours", CircleRepository.DURATION_48_HOURS),
        DurationOption("72 hours", CircleRepository.DURATION_72_HOURS),
        DurationOption("7 days", CircleRepository.DURATION_7_DAYS)
    )
    
    // Handle success/error
    LaunchedEffect(viewModel.createdCircleId) {
        viewModel.createdCircleId?.let {
            onCircleCreated(it)
        }
    }
    
    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
            }
        }
    }
    
    // Location permission explanation dialog
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
                
                Column(Modifier.selectableGroup()) {
                    durationOptions.forEach { option ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .selectable(
                                    selected = (selectedDuration == option.durationMillis),
                                    onClick = { selectedDuration = option.durationMillis },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedDuration == option.durationMillis),
                                onClick = null // null because we're handling the click on the row
                            )
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Location-Based",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Members must be in the specified location to view content",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    Switch(
                        checked = locationEnabled,
                        onCheckedChange = { locationEnabled = it }
                    )
                }
                
                // Location settings when locationEnabled is true
                if (locationEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Location Settings",
                                style = MaterialTheme.typography.titleMedium
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            if (!hasLocation) {
                                Button(
                                    onClick = {
                                        when {
                                            // Check if we already have permission
                                            ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.ACCESS_FINE_LOCATION
                                            ) == PackageManager.PERMISSION_GRANTED -> {
                                                isLoadingLocation = true
                                                getCurrentLocation(
                                                    fusedLocationClient = fusedLocationClient,
                                                    onLocationResult = { location ->
                                                        locationLat = location.latitude
                                                        locationLng = location.longitude
                                                        hasLocation = true
                                                        isLoadingLocation = false
                                                    },
                                                    onError = { error ->
                                                        scope.launch {
                                                            snackbarHostState.showSnackbar("Failed to get location: $error")
                                                        }
                                                        isLoadingLocation = false
                                                    }
                                                )
                                            }
                                            // Show rationale dialog
                                            else -> {
                                                showLocationPermissionDialog = true
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isLoadingLocation
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        if (isLoadingLocation) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.MyLocation,
                                                contentDescription = null
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Text(text = if (isLoadingLocation) "Getting location..." else "Use Current Location")
                                    }
                                }
                            } else {
                                // Show location details
                                Text(
                                    text = "Current Location",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = "Latitude: ${String.format("%.6f", locationLat)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                
                                Text(
                                    text = "Longitude: ${String.format("%.6f", locationLng)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                    text = "Radius: ${locationRadius.toInt()} meters",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Slider(
                                    value = locationRadius.toFloat(),
                                    onValueChange = { locationRadius = it.toDouble() },
                                    valueRange = 50f..1000f,
                                    steps = 19,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                Text(
                                    text = "Members will only see content when they are within this radius",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Button(
                                    onClick = {
                                        isLoadingLocation = true
                                        getCurrentLocation(
                                            fusedLocationClient = fusedLocationClient,
                                            onLocationResult = { location ->
                                                locationLat = location.latitude
                                                locationLng = location.longitude
                                                isLoadingLocation = false
                                            },
                                            onError = { error ->
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Failed to update location: $error")
                                                }
                                                isLoadingLocation = false
                                            }
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isLoadingLocation
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        if (isLoadingLocation) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.MyLocation,
                                                contentDescription = null
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Text(text = if (isLoadingLocation) "Updating..." else "Update Location")
                                    }
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
                    onError("Location is null")
                }
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Unknown error")
            }
    } catch (e: Exception) {
        onError(e.message ?: "Unknown error")
    }
}

data class DurationOption(val label: String, val durationMillis: Long) 