package com.example.myapplication.ui.camera

import android.Manifest
import android.net.Uri
import android.util.Log
import android.view.SurfaceView
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.LifecycleOwner
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.myapplication.data.repositories.SnapRepository
import com.example.myapplication.ui.camera.filters.DeepARFilter
import com.example.myapplication.ui.camera.filters.DeepARManager
import com.example.myapplication.ui.camera.filters.FilterCarousel
import com.example.myapplication.ui.theme.ScreenshotProtection
import com.google.accompanist.permissions.*
import kotlinx.coroutines.*
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
    
    // Initialize repositories
    val snapRepo = remember { SnapRepository() }
    
    // Camera state
    var hasPermission by rememberSaveable { mutableStateOf(false) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showRecipientSelector by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var isArMode by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf<DeepARFilter?>(null) }
    var captionText by remember { mutableStateOf("") }
    var showCaptionDialog by remember { mutableStateOf(false) }
    var isGeneratingCaption by remember { mutableStateOf(false) }
    var ragGeneratedCaption by remember { mutableStateOf<String?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingStartTime by remember { mutableStateOf(0L) }
    var isFlashEnabled by remember { mutableStateOf(false) }
    var isBackCamera by remember { mutableStateOf(true) }
    
    // Camera setup
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var videoCapture: VideoCapture<Recorder>? by remember { mutableStateOf(null) }
    var recording: Recording? by remember { mutableStateOf(null) }
    var camera: Camera? by remember { mutableStateOf(null) }
    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }
    
    // Create and remember PreviewView
    val previewView = remember { PreviewView(context).apply {
        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
    }}
    
    // Remember Preview use case
    val preview = remember { Preview.Builder()
        .build()
        .also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
    }
    
    // Create SurfaceView for DeepAR
    val surfaceView = remember { SurfaceView(context) }
    
    // Create DeepAR manager
    val deepARManager = remember { DeepARManager(context, scope, surfaceView) }
    
    // Available filters
    val availableFilters by remember { mutableStateOf(deepARManager.getAvailableFilters()) }

    // Request camera permission
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
    LaunchedEffect(cameraPermissionState.status) {
        when (cameraPermissionState.status) {
            is PermissionStatus.Granted -> {
                hasPermission = true
            }
            is PermissionStatus.Denied -> {
                cameraPermissionState.launchPermissionRequest()
            }
        }
    }

    // Initialize camera when permission is granted
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            try {
                cameraProvider = cameraProviderFuture.get()
                
                // Create use cases
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                
                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(
                        Quality.HIGHEST,
                        FallbackStrategy.higherQualityOrLowerThan(Quality.SD)
                    ))
                    .build()
                videoCapture = VideoCapture.withOutput(recorder)
                
                // Select back camera
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                
                // Unbind all use cases before rebinding
                cameraProvider?.unbindAll()
                
                if (!isArMode) {
                    // Bind use cases
                    camera = cameraProvider?.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture!!,
                        videoCapture!!
                    )
                }
            } catch (e: Exception) {
                Log.e("CameraScreen", "Camera initialization failed", e)
            }
        }
    }

    // Handle AR mode changes
    LaunchedEffect(isArMode) {
        if (isArMode) {
            cameraProvider?.unbindAll()
            delay(500)
            deepARManager.startCamera(lifecycleOwner)
            
            delay(1500)
            if (availableFilters.isNotEmpty()) {
                val filterOptions = listOf("Devil Horns", "Viking Helmet", "Makeup Look", "None")
                val defaultFilter = filterOptions.firstNotNullOfOrNull { filterName ->
                    availableFilters.firstOrNull { it.name == filterName }
                } ?: availableFilters.first()
                
                selectedFilter = defaultFilter
                deepARManager.applyFilter(defaultFilter)
            }
        } else {
            deepARManager.stopCamera()
            
            // Rebind regular camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider?.unbindAll()
                camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture!!,
                    videoCapture!!
                )
            } catch (e: Exception) {
                Log.e("CameraScreen", "Failed to bind camera use cases", e)
            }
        }
    }

    // Cleanup
    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                cameraProvider?.unbindAll()
                recording?.close()
                deepARManager.stopCamera()
            } catch (e: Exception) {
                Log.e("CameraScreen", "Error cleaning up camera", e)
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
        // Camera preview
        if (isArMode) {
            AndroidView(
                factory = { surfaceView },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    try {
                        val cameraSelector = if (isBackCamera) {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        } else {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        }
                        
                        cameraProvider?.unbindAll()
                        camera = cameraProvider?.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture ?: return@AndroidView,
                            videoCapture ?: return@AndroidView
                        )
                    } catch (e: Exception) {
                        Log.e("CameraScreen", "Use case binding failed", e)
                    }
                }
            )
        }

        // Right side control buttons
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Flash button
            FloatingActionButton(
                onClick = { 
                    isFlashEnabled = !isFlashEnabled
                    camera?.cameraControl?.enableTorch(isFlashEnabled)
                },
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            ) {
                Icon(
                    if (isFlashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = if (isFlashEnabled) "Disable Flash" else "Enable Flash"
                )
            }
            
            // Toggle AR mode button
            FloatingActionButton(
                onClick = { 
                    isArMode = !isArMode
                    if (!isArMode) {
                        selectedFilter = null
                    }
                },
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            ) {
                Icon(
                    if (isArMode) Icons.Default.ArrowBack else Icons.Default.Face,
                    contentDescription = if (isArMode) "Switch to Camera" else "Switch to AR"
                )
            }
            
            // Camera flip button
            if(!isArMode){
                FloatingActionButton(
                onClick = { 
                    scope.launch {
                        // Reset filters when switching to back camera
                        if (!isBackCamera) {
                            selectedFilter = null
                            isArMode = false // Disable AR mode when switching to back camera
                        }
                        isBackCamera = !isBackCamera
                        val newCameraSelector = if (isBackCamera) {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        } else {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        }
                        
                        try {
                            if (isArMode) {
                                // For AR mode, we need to stop and restart the camera
                                deepARManager.stopCamera()
                                delay(500) // Give time for camera to release
                                deepARManager.startCamera(lifecycleOwner)
                            } else {
                                cameraProvider?.unbindAll()
                                camera = cameraProvider?.bindToLifecycle(
                                    lifecycleOwner,
                                    newCameraSelector,
                                    preview,
                                    imageCapture ?: return@launch,
                                    videoCapture ?: return@launch
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("CameraScreen", "Failed to flip camera", e)
                            Toast.makeText(context, "Failed to flip camera", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            ) {
                Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Flip Camera")
            }}
        }
        
        // Close button at top-left
        FloatingActionButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(48.dp),
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close")
        }
        
        // Circle mode indicator if applicable
        if (circleId != null) {
            FloatingActionButton(
                onClick = { /* No action needed */ },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(48.dp),
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            ) {
                Icon(
                    Icons.Default.Groups,
                    contentDescription = "Circle Mode"
                )
            }
        }

        // Capture button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            // Record indicator
            if (isRecording) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                )
            }
            
            // Main capture button
            FloatingActionButton(
                onClick = { },  // Handle in pointerInput
                modifier = Modifier
                    .size(64.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = { pressPosition ->
                                val pressStartTime = System.currentTimeMillis()
                                val job = scope.launch {
                                    delay(1000) // Wait 1 second before starting video
                                    if (isActive) {
                                        // Start video recording
                                        if (!isRecording) {
                                            val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                                                .format(System.currentTimeMillis())
                                            val contentValues = android.content.ContentValues().apply {
                                                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, name)
                                                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                                            }
                                            
                                            val mediaStoreOutput = MediaStoreOutputOptions.Builder(
                                                context.contentResolver,
                                                android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                                            ).setContentValues(contentValues).build()

                                            if (videoCapture == null) {
                                                Toast.makeText(context, "Camera not ready yet", Toast.LENGTH_SHORT).show()
                                                return@launch
                                            }

                                            recording = videoCapture?.output
                                                ?.prepareRecording(context, mediaStoreOutput)
                                                ?.apply { 
                                                    if (PermissionChecker.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == 
                                                        PermissionChecker.PERMISSION_GRANTED) {
                                                        withAudioEnabled()
                                                    }
                                                }
                                                ?.start(ContextCompat.getMainExecutor(context)) { event ->
                                                    when(event) {
                                                        is VideoRecordEvent.Start -> {
                                                            isRecording = true
                                                            recordingStartTime = System.currentTimeMillis()
                                                        }
                                                        is VideoRecordEvent.Finalize -> {
                                                            if (!event.hasError()) {
                                                                val uri = event.outputResults.outputUri
                                                                Toast.makeText(context, "Video captured", Toast.LENGTH_SHORT).show()
                                                                capturedImageUri = uri
                                                                if (circleId != null) {
                                                                    showCaptionDialog = true
                                                                } else {
                                                                    showRecipientSelector = true
                                                                }
                                                            } else {
                                                                recording?.close()
                                                                recording = null
                                                                Toast.makeText(context, "Video capture failed: ${event.error}", Toast.LENGTH_SHORT).show()
                                                            }
                                                            isRecording = false
                                                        }
                                                    }
                                                }

                                            if (recording == null) {
                                                Toast.makeText(context, "Failed to prepare video recording", Toast.LENGTH_SHORT).show()
                                                return@launch
                                            }

                                            // Start a timer to stop recording after 10 seconds
                                            scope.launch {
                                                delay(10000) // 10 seconds
                                                if (isRecording) {
                                                    recording?.stop()
                                                    recording = null
                                                }
                                            }
                                        }
                                    }
                                }

                                tryAwaitRelease()
                                job.cancel()
                                
                                // If it was a short press and we're not recording, take a photo
                                if (!isRecording && System.currentTimeMillis() - pressStartTime < 1000) {
                                    if (isArMode) {
                                        // Take screenshot using DeepAR
                                        deepARManager.takeScreenshot()
                                        scope.launch {
                                            delay(500)
                                            val uri = deepARManager.getLastScreenshotUri()
                                            if (uri != null) {
                                                Toast.makeText(context, "Photo captured", Toast.LENGTH_SHORT).show()
                                                capturedImageUri = uri
                                                if (circleId != null) {
                                                    showCaptionDialog = true
                                                } else {
                                                    showRecipientSelector = true
                                                }
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
                                        
                                        if (imageCapture == null) {
                                            Toast.makeText(context, "Camera not ready yet", Toast.LENGTH_SHORT).show()
                                            return@detectTapGestures
                                        }
                                        
                                        imageCapture?.takePicture(
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
                                } else if (isRecording) {
                                    // Stop video recording
                                    recording?.stop()
                                    recording = null
                                }
                            }
                        )
                    },
                containerColor = if (isRecording) 
                    MaterialTheme.colorScheme.error 
                else MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    if (isRecording) Icons.Default.Stop else Icons.Default.Camera,
                    contentDescription = if (isRecording) "Stop Recording" else "Take Photo",
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Filter carousel
        AnimatedVisibility(
            visible = isArMode && !isBackCamera,
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