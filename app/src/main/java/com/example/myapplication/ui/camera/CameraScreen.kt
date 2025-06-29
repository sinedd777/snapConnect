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
import android.provider.MediaStore
import android.content.ContentValues
import android.media.MediaPlayer
import android.widget.VideoView
import android.widget.ImageView
import coil.load
import android.widget.MediaController

@Composable
private fun ReviewScreen(
    uri: Uri,
    isVideo: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Media preview
        if (isVideo) {
            var isPlaying by remember { mutableStateOf(true) }
            
            AndroidView(
                factory = { context ->
                    VideoView(context).apply {
                        setVideoURI(uri)
                        // Add media controls
                        val mediaController = MediaController(context)
                        mediaController.setAnchorView(this)
                        setMediaController(mediaController)
                        
                        // Configure video playback
                        setOnPreparedListener { mediaPlayer ->
                            mediaPlayer.isLooping = true
                            mediaPlayer.setOnCompletionListener {
                                isPlaying = false
                            }
                            if (isPlaying) start()
                        }
                        
                        // Handle errors
                        setOnErrorListener { _, what, extra ->
                            Log.e("ReviewScreen", "Video playback error: what=$what extra=$extra")
                            Toast.makeText(context, "Error playing video", Toast.LENGTH_SHORT).show()
                            true
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    view.setVideoURI(uri)
                    if (isPlaying) view.start() else view.pause()
                }
            )
            
            // Add play/pause button overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                isPlaying = !isPlaying
                            }
                        )
                    }
            )
        } else {
            // Photo preview using Coil
            AndroidView(
                factory = { context ->
                    ImageView(context).apply {
                        scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    view.load(uri) {
                        crossfade(true)
                    }
                }
            )
        }

        // Semi-transparent overlay at the top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
        ) {
            // Top bar with buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Close button
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Send button
                IconButton(
                    onClick = onConfirm,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

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
    var isVideoMode by remember { mutableStateOf(false) }
    var isFlashEnabled by remember { mutableStateOf(false) }
    var isBackCamera by remember { mutableStateOf(true) }
    var showReviewScreen by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }  // New state for tracking capture
    var isLastCaptureVideo by remember { mutableStateOf(false) }
    
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
        scaleType = PreviewView.ScaleType.FILL_CENTER  // Changed from default to FILL_CENTER
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

    // Add recording timer state
    var recordingDuration by remember { mutableStateOf(0L) }
    var recordingTimer by remember { mutableStateOf<Job?>(null) }

    // Cleanup timer on dispose
    DisposableEffect(Unit) {
        onDispose {
            recordingTimer?.cancel()
            recordingTimer = null
        }
    }

    // Modify the media capture callback
    val onMediaCaptured: (Uri, Boolean) -> Unit = { uri, isVideo ->
        capturedImageUri = uri
        isLastCaptureVideo = isVideo
        showReviewScreen = true
        isCapturing = false  // Reset capturing state
    }

    // Function to start/stop video recording
    val toggleRecording = remember(videoCapture) {
        {
            val videoCapture = videoCapture ?: run {
                Log.e("CameraScreen", "Cannot record video, videoCapture not initialized")
                Toast.makeText(context, "Camera not ready yet", Toast.LENGTH_SHORT).show()
                return@remember
            }

            if (isRecording) {
                // Stop recording
                Log.d("CameraScreen", "Stopping video recording")
                recordingTimer?.cancel()
                recordingTimer = null
                recordingDuration = 0L
                recording?.stop()
            } else {
                // Start recording
                Log.d("CameraScreen", "Starting video recording")
                isCapturing = true  // Set capturing state
                
                val name = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US)
                    .format(System.currentTimeMillis())
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                    if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.P) {
                        put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
                    }
                }

                val mediaStoreOutput = MediaStoreOutputOptions.Builder(
                    context.contentResolver,
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                )
                    .setContentValues(contentValues)
                    .build()

                try {
                    recording = videoCapture.output
                        .prepareRecording(context, mediaStoreOutput)
                        .apply {
                            if (PermissionChecker.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                                PermissionChecker.PERMISSION_GRANTED) {
                                withAudioEnabled()
                            }
                        }
                        .start(ContextCompat.getMainExecutor(context)) { event ->
                            when (event) {
                                is VideoRecordEvent.Start -> {
                                    Log.d("CameraScreen", "Video recording started")
                                    isRecording = true
                                    // Start timer
                                    recordingTimer = scope.launch {
                                        while (true) {
                                            delay(1000)
                                            recordingDuration += 1
                                        }
                                    }
                                }
                                is VideoRecordEvent.Finalize -> {
                                    if (!event.hasError()) {
                                        val uri = event.outputResults.outputUri
                                        Log.d("CameraScreen", "Video recording succeeded: $uri")
                                        Toast.makeText(context, "Video captured!", Toast.LENGTH_SHORT).show()
                                        onMediaCaptured(uri, true)
                                    } else {
                                        recording?.close()
                                        recording = null
                                        Log.e("CameraScreen", "Video capture failed: ${event.error}")
                                        Toast.makeText(context, "Video capture failed", Toast.LENGTH_SHORT).show()
                                    }
                                    isRecording = false
                                    isCapturing = false
                                    recordingTimer?.cancel()
                                    recordingTimer = null
                                    recordingDuration = 0L
                                }
                            }
                        }
                } catch (e: Exception) {
                    Log.e("CameraScreen", "Failed to start recording", e)
                    Toast.makeText(context, "Failed to start recording: ${e.message}", Toast.LENGTH_SHORT).show()
                    isCapturing = false
                }
            }
        }
    }

    // Modify the photo capture code to use onMediaCaptured
    val takePhoto = remember(imageCapture) {
        {
            val imageCapture = imageCapture ?: return@remember

            // Create time stamped name and MediaStore entry
            val name = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US)
                .format(System.currentTimeMillis())
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.P) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
                }
            }

            // Create output options object
            val outputOptions = ImageCapture.OutputFileOptions
                .Builder(
                    context.contentResolver,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                .build()

            Log.d("CameraScreen", "Taking photo with options: $outputOptions")

            // Take the picture
            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val savedUri = output.savedUri
                        Log.d("CameraScreen", "Photo capture succeeded: $savedUri")
                        Toast.makeText(context, "Photo captured!", Toast.LENGTH_SHORT).show()
                        savedUri?.let { onMediaCaptured(it, false) }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e("CameraScreen", "Photo capture failed: ${exception.message}", exception)
                        Toast.makeText(context, "Failed to take photo", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

    // Function to bind camera use cases
    val bindCamera = remember {
        {
            try {
                val cameraSelector = if (isBackCamera) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                }
                
                cameraProvider?.let { provider ->
                    // Unbind all use cases before rebinding
                    provider.unbindAll()
                    
                    if (!isArMode) {
                        // Bind use cases
                        camera = provider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture ?: return@let,
                            videoCapture ?: return@let
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("CameraScreen", "Failed to bind camera use cases", e)
            }
        }
    }

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
                // Wait for camera provider to be ready
                cameraProvider = cameraProviderFuture.get()
                
                // Create use cases
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setTargetResolution(android.util.Size(1920, 1080))  // FHD resolution
                    .build()
                
                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.FHD))  // Use FHD instead of HIGHEST
                    .build()
                videoCapture = VideoCapture.withOutput(recorder)
                
                // Add debug logging
                Log.d("CameraScreen", "Camera initialized with FHD quality")
                Log.d("CameraScreen", "imageCapture: $imageCapture")
                Log.d("CameraScreen", "videoCapture: $videoCapture")
                
                // Initial camera binding
                bindCamera()
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
            bindCamera()
        }
    }

    // Handle camera flipping
    LaunchedEffect(isBackCamera) {
        if (!isArMode) {
            bindCamera()
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

    // Show review screen if we have captured media
    if (showReviewScreen && capturedImageUri != null) {
        ReviewScreen(
            uri = capturedImageUri!!,
            isVideo = isLastCaptureVideo,
            onConfirm = {
                showReviewScreen = false
                if (circleId != null) {
                    showCaptionDialog = true
                } else {
                    showRecipientSelector = true
                }
            },
            onCancel = {
                showReviewScreen = false
                capturedImageUri = null
            }
        )
    } else {
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
                        // The preview view is already set up in the factory block
                        // and the camera binding is handled by LaunchedEffect blocks
                        // No need for additional setup here
                    }
                )
            }

            // Recording indicator
            if (isRecording) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pulsating recording indicator
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(
                                        MaterialTheme.colorScheme.error,
                                        CircleShape
                                    )
                            )
                            Text(
                                "Recording",
                                color = MaterialTheme.colorScheme.onError
                            )
                        }
                        
                        // Recording duration
                        Text(
                            String.format(
                                "%02d:%02d",
                                recordingDuration / 60,
                                recordingDuration % 60
                            ),
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                }
            }

            // Right side control buttons
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Mode selector button
                FloatingActionButton(
                    onClick = { 
                        if (!isRecording) { // Don't allow mode change during recording
                            isVideoMode = !isVideoMode 
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ) {
                    Icon(
                        if (isVideoMode) Icons.Default.PhotoCamera else Icons.Default.Videocam,
                        contentDescription = if (isVideoMode) "Switch to Photo Mode" else "Switch to Video Mode"
                    )
                }
                
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

            // Bottom controls
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
                    onClick = { 
                        if (!showReviewScreen) {  // Only allow capture if not in review
                            if (isVideoMode) {
                                toggleRecording()
                            } else {
                                // Photo mode
                                if (!isRecording) {
                                    isCapturing = true  // Set capturing state
                                    if (isArMode) {
                                        // Take screenshot using DeepAR
                                        deepARManager.takeScreenshot()
                                        scope.launch {
                                            delay(500)
                                            val uri = deepARManager.getLastScreenshotUri()
                                            if (uri != null) {
                                                onMediaCaptured(uri, false)
                                            } else {
                                                isCapturing = false  // Reset capturing state on error
                                                Toast.makeText(context, "Failed to capture photo", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } else {
                                        Log.d("CameraScreen", "Taking photo")
                                        val imageCapture = imageCapture
                                        if (imageCapture != null) {
                                            // Create time stamped name and MediaStore entry
                                            val name = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US)
                                                .format(System.currentTimeMillis())
                                            val contentValues = ContentValues().apply {
                                                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                                                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                                                if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.P) {
                                                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
                                                }
                                            }

                                            val outputOptions = ImageCapture.OutputFileOptions
                                                .Builder(
                                                    context.contentResolver,
                                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                                    contentValues
                                                )
                                                .build()

                                            imageCapture.takePicture(
                                                outputOptions,
                                                ContextCompat.getMainExecutor(context),
                                                object : ImageCapture.OnImageSavedCallback {
                                                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                                        val savedUri = output.savedUri
                                                        Log.d("CameraScreen", "Photo capture succeeded: $savedUri")
                                                        savedUri?.let { onMediaCaptured(it, false) }
                                                    }

                                                    override fun onError(exception: ImageCaptureException) {
                                                        Log.e("CameraScreen", "Photo capture failed: ${exception.message}", exception)
                                                        Toast.makeText(context, "Failed to take photo", Toast.LENGTH_SHORT).show()
                                                        isCapturing = false  // Reset capturing state on error
                                                    }
                                                }
                                            )
                                        } else {
                                            Toast.makeText(context, "Camera not ready yet", Toast.LENGTH_SHORT).show()
                                            isCapturing = false  // Reset capturing state on error
                                        }
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.size(64.dp),
                    containerColor = when {
                        isRecording -> MaterialTheme.colorScheme.error
                        isVideoMode -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.primary
                    }
                ) {
                    Icon(
                        when {
                            isRecording -> Icons.Default.Stop
                            isVideoMode -> Icons.Default.Videocam
                            else -> Icons.Default.PhotoCamera
                        },
                        contentDescription = when {
                            isRecording -> "Stop Recording"
                            isVideoMode -> "Start Recording"
                            else -> "Take Photo"
                        },
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
} 