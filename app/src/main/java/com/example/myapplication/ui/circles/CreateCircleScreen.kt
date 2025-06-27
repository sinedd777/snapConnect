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
    var selectedDuration by remember { mutableLongStateOf(CircleRepository.DURATION_24_HOURS) }
    
    // Location state - always enabled by default
    var locationLat by remember { mutableDoubleStateOf(0.0) }
    var locationLng by remember { mutableDoubleStateOf(0.0) }
    var locationRadius by remember { mutableDoubleStateOf(100.0) } // Default 100 meters
    var hasLocation by remember { mutableStateOf(false) }
    var showLocationPermissionDialog by remember { mutableStateOf(false) }
    var isLoadingLocation by remember { mutableStateOf(false) }
    var showLocationErrorDialog by remember { mutableStateOf(false) }
    
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    // Permission request handler
    val requestLocationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, get location
            isLoadingLocation = true
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
            // Permission denied - show error dialog and navigate back
            showLocationErrorDialog = true
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
    
    // Check location permission when entering the screen
    LaunchedEffect(Unit) {
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
            onDismissRequest = { 
                showLocationPermissionDialog = false
                // Navigate back if user dismisses dialog
                onNavigateBack()
            },
            title = { Text("Location Permission Required") },
            text = { Text("This app needs access to your location to create Circles. All Circles are location-based and require your current location to function properly.") },
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
                        // Navigate back if user cancels
                        onNavigateBack()
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Location error dialog - shown when permission is denied
    if (showLocationErrorDialog) {
        AlertDialog(
            onDismissRequest = { 
                showLocationErrorDialog = false
                onNavigateBack()
            },
            title = { Text("Location Permission Denied") },
            text = { Text("Location permission is required to create Circles. Please enable location permissions in your device settings to use this feature.") },
            confirmButton = {
                Button(
                    onClick = {
                        showLocationErrorDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("OK")
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
                
                
                Button(
                    onClick = {
                        viewModel.createCircle(
                            name = name,
                            description = description,
                            durationMillis = selectedDuration,
                            isPrivate = isPrivate,
                            locationEnabled = true, // Always enabled
                            locationLat = if (hasLocation) locationLat else null,
                            locationLng = if (hasLocation) locationLng else null,
                            locationRadius = if (hasLocation) locationRadius else null
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.isLoading && hasLocation // Must have location to create circle
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