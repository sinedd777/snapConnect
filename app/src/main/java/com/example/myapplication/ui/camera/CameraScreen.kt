package com.example.myapplication.ui.camera

import android.Manifest
import android.net.Uri
import android.util.Log
import android.view.SurfaceView
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.myapplication.data.repositories.SnapRepository
import com.example.myapplication.ui.camera.filters.DeepARFilter
import com.example.myapplication.ui.camera.filters.DeepARManager
import com.example.myapplication.ui.camera.filters.FilterCarousel
import com.example.myapplication.ui.theme.ScreenshotProtection
import com.google.accompanist.permissions.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.auth.FirebaseAuth
import com.example.myapplication.data.repositories.RAGRepository
import com.example.myapplication.data.models.Snap

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    circleId: String? = null,
    onSnapCaptured: (Uri?) -> Unit,
    onBack: () -> Unit = {}
) {
    // Apply screenshot protection
    ScreenshotProtection()
    
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
    // Camera state
    val imageCapture = remember { ImageCapture.Builder().build() }
    val snapRepo = remember { SnapRepository() }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    
    // UI state
    var hasPermission by rememberSaveable { mutableStateOf(false) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showRecipientSelector by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var isArMode by remember { mutableStateOf(false) }
    var showFilterSelector by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf<DeepARFilter?>(null) }
    var captionText by remember { mutableStateOf("") }
    var showCaptionDialog by remember { mutableStateOf(false) }
    var isGeneratingCaption by remember { mutableStateOf(false) }
    var ragGeneratedCaption by remember { mutableStateOf<String?>(null) }
    
    // Create SurfaceView for DeepAR
    val surfaceView = remember { SurfaceView(context) }
    
    // Create DeepAR manager
    val deepARManager = remember { DeepARManager(context, scope, surfaceView) }
    
    // Available filters
    val availableFilters by remember { mutableStateOf(deepARManager.getAvailableFilters()) }
    
    // Handle AR mode and filter application
    LaunchedEffect(Unit) {
        delay(500) // Short delay to ensure UI is ready
        Log.d("CameraScreen", "Switching to AR mode")
        isArMode = true
        
        // Start camera with lifecycle owner
        deepARManager.startCamera(lifecycleOwner)
        
        // Apply a default filter after a short delay to ensure DeepAR is initialized
        delay(1500)
        if (availableFilters.isNotEmpty()) {
            // Try a few different filters to see if any work
            val filterOptions = listOf("Devil Horns", "Viking Helmet", "Makeup Look", "None")
            val defaultFilter = filterOptions.firstNotNullOfOrNull { filterName ->
                availableFilters.firstOrNull { it.name == filterName }
            } ?: availableFilters.first()
            
            Log.d("CameraScreen", "Applying default filter: ${defaultFilter.name}")
            selectedFilter = defaultFilter
            deepARManager.applyFilter(defaultFilter)
        }
    }
    
    // Cleanup when leaving the screen
    DisposableEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(deepARManager)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(deepARManager)
        }
    }

    // Request camera permission
    if (!hasPermission) {
        val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
        
        LaunchedEffect(key1 = cameraPermission.status) {
            Log.d("CameraScreen", "Camera permission status: ${cameraPermission.status}")
            if (cameraPermission.status == PermissionStatus.Granted) {
                hasPermission = true
                Log.d("CameraScreen", "Camera permission granted")
                
                // Force AR mode immediately when permission is granted
                isArMode = true
            } else if (cameraPermission.status is PermissionStatus.Denied) {
                Log.d("CameraScreen", "Requesting camera permission")
                cameraPermission.launchPermissionRequest()
            }
        }
    }

    if (!hasPermission) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Camera permission required")
        }
        return
    }

    // If we're in Circle mode and have captured an image, show caption dialog
    if (circleId != null && capturedImageUri != null && showCaptionDialog) {
        AlertDialog(
            onDismissRequest = { showCaptionDialog = false },
            title = { Text("Add a caption") },
            text = {
                Column {
                    OutlinedTextField(
                        value = captionText,
                        onValueChange = { captionText = it },
                        label = { Text("Your caption (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (isGeneratingCaption) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                        Text(
                            "Generating AI caption...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    } else if (ragGeneratedCaption != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "✨ AI Suggested Caption:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            ragGeneratedCaption!!,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        TextButton(
                            onClick = { captionText = ragGeneratedCaption!! }
                        ) {
                            Text("Use AI Caption")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCaptionDialog = false
                        isUploading = true
                        scope.launch {
                            val result = snapRepo.uploadSnapToCircle(
                                capturedImageUri!!,
                                circleId,
                                if (captionText.isBlank()) null else captionText,
                                context // Pass context for RAG
                            )
                            isUploading = false
                            result.fold(
                                onSuccess = { 
                                    Toast.makeText(context, "Snap shared to Circle!", Toast.LENGTH_SHORT).show()
                                    onSnapCaptured(capturedImageUri)
                                },
                                onFailure = { e ->
                                    Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                ) {
                    Text("Share")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCaptionDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
        
        // Generate RAG caption when dialog opens
        LaunchedEffect(Unit) {
            isGeneratingCaption = true
            val ragRepo = RAGRepository()
            val dummySnap = Snap(
                id = UUID.randomUUID().toString(),
                sender = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                mediaUrl = capturedImageUri.toString(),
                mediaType = "image/jpeg",
                recipients = listOf(),
                createdAt = com.google.firebase.Timestamp.now()
            )
            
            ragRepo.generateAndStoreSnapCaption(dummySnap, capturedImageUri!!, context)
                .onSuccess { caption ->
                    ragGeneratedCaption = caption
                }
                .onFailure { e ->
                    Log.e("CameraScreen", "Failed to generate RAG caption: ${e.message}")
                }
            
            isGeneratingCaption = false
        }
    }

    if (showRecipientSelector && capturedImageUri != null && circleId == null) {
        RecipientSelectorScreen(
            onBack = { showRecipientSelector = false },
            onSendToRecipients = { recipients ->
                isUploading = true
                scope.launch {
                    val result = snapRepo.uploadSnap(
                        capturedImageUri!!, 
                        recipients,
                        context = context // Pass context for RAG
                    )
                    isUploading = false
                    result.fold(
                        onSuccess = { 
                            Toast.makeText(context, "Snap sent!", Toast.LENGTH_SHORT).show()
                            onSnapCaptured(capturedImageUri)
                        },
                        onFailure = { e ->
                            Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview or AR view
        if (isArMode) {
            // DeepAR view
            AndroidView(
                factory = { surfaceView },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Regular camera preview
            val previewView = remember { PreviewView(context) }
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            ) { view ->
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().apply {
                    setSurfaceProvider(view.surfaceProvider)
                }
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                } catch (exc: Exception) {
                    Log.e("CameraScreen", "Use case binding failed", exc)
                }
            }
        }

        // Top action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Close button
            FloatingActionButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
            
            // Circle mode indicator if applicable
            if (circleId != null) {
                FloatingActionButton(
                    onClick = { /* No action needed */ },
                    modifier = Modifier.size(48.dp),
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                ) {
                    Icon(
                        Icons.Default.Groups,
                        contentDescription = "Circle Mode"
                    )
                }
            }
            
            // Toggle AR mode button
            FloatingActionButton(
                onClick = { 
                    isArMode = !isArMode
                    // If switching to regular camera, clear any active filter
                    if (!isArMode && selectedFilter != null) {
                        selectedFilter = null
                    }
                },
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            ) {
                Icon(
                    if (isArMode) Icons.Default.CameraAlt else Icons.Default.Face,
                    contentDescription = if (isArMode) "Switch to Camera" else "Switch to AR"
                )
            }
            
            // Filter button (only in AR mode)
            AnimatedVisibility(
                visible = isArMode,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                FloatingActionButton(
                    onClick = { showFilterSelector = !showFilterSelector },
                    modifier = Modifier.size(48.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ) {
                    Icon(
                        Icons.Default.FilterVintage,
                        contentDescription = "Filters"
                    )
                }
            }
        }

        // Capture button
        FloatingActionButton(
            onClick = {
                if (isArMode) {
                    // Take screenshot using DeepAR
                    deepARManager.takeScreenshot()
                    
                    // Wait a moment for the screenshot to be processed
                    scope.launch {
                        delay(500) // Wait for screenshot to be processed
                        val uri = deepARManager.getLastScreenshotUri()
                        if (uri != null) {
                            Toast.makeText(context, "Photo captured", Toast.LENGTH_SHORT).show()
                            capturedImageUri = uri
                            
                            if (circleId != null) {
                                showCaptionDialog = true
                            } else {
                                showRecipientSelector = true
                            }
                        } else {
                            Toast.makeText(context, "Capture failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    // Take regular photo
                    val photoFile = File(
                        context.cacheDir,
                        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                            .format(System.currentTimeMillis()) + ".jpg"
                    )

                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                    imageCapture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onError(exception: ImageCaptureException) {
                                Log.e("CameraScreen", "Photo capture failed: $exception")
                                Toast.makeText(context, "Capture failed", Toast.LENGTH_SHORT).show()
                                onSnapCaptured(null)
                            }

                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                                Toast.makeText(context, "Photo captured", Toast.LENGTH_SHORT).show()
                                capturedImageUri = savedUri
                                
                                if (circleId != null) {
                                    showCaptionDialog = true
                                } else {
                                    showRecipientSelector = true
                                }
                            }
                        }
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                Icons.Default.Camera,
                contentDescription = "Take Photo",
                modifier = Modifier.size(32.dp)
            )
        }

        // Filter carousel
        AnimatedVisibility(
            visible = showFilterSelector,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp),
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) {
            FilterCarousel(
                filters = availableFilters,
                selectedFilter = selectedFilter,
                onFilterSelected = { filter ->
                    selectedFilter = filter
                    deepARManager.applyFilter(filter)
                }
            )
        }

        // Loading indicator
        if (isUploading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
} 