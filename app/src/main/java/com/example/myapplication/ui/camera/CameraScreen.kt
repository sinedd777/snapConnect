package com.example.myapplication.ui.camera

import android.Manifest
import android.net.Uri
import android.util.Log
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
import com.example.myapplication.ui.camera.filters.ARFilter
import com.example.myapplication.ui.camera.filters.ARFilterManager
import com.example.myapplication.ui.camera.filters.FilterSelector
import com.google.accompanist.permissions.*
import io.github.sceneview.ar.ArSceneView
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    circleId: String? = null,
    onSnapCaptured: (Uri?) -> Unit,
    onBack: () -> Unit = {}
) {
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
    var selectedFilter by remember { mutableStateOf<ARFilter?>(null) }
    var captionText by remember { mutableStateOf("") }
    var showCaptionDialog by remember { mutableStateOf(false) }
    
    // AR state
    val arSceneView = remember { ArSceneView(context) }
    val arFilterManager = remember { ARFilterManager(context, arSceneView, scope) }
    
    // Available filters
    val availableFilters by remember { mutableStateOf(arFilterManager.getAvailableFilters()) }

    // Request camera permission
    if (!hasPermission) {
        val permission = Manifest.permission.CAMERA
        val permState = rememberPermissionState(permission)
        LaunchedEffect(key1 = permState.status) {
            if (permState.status == PermissionStatus.Granted) {
                hasPermission = true
            } else if (permState.status is PermissionStatus.Denied) {
                permState.launchPermissionRequest()
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
                OutlinedTextField(
                    value = captionText,
                    onValueChange = { captionText = it },
                    label = { Text("Caption (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
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
                                if (captionText.isBlank()) null else captionText
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
    }

    if (showRecipientSelector && capturedImageUri != null && circleId == null) {
        RecipientSelectorScreen(
            onBack = { showRecipientSelector = false },
            onSendToRecipients = { recipients ->
                isUploading = true
                scope.launch {
                    val result = snapRepo.uploadSnap(capturedImageUri!!, recipients)
                    isUploading = false
                    result.fold(
                        onSuccess = { 
                            Toast.makeText(context, "Snap sent!", Toast.LENGTH_SHORT).show()
                            onSnapCaptured(capturedImageUri)
                        },
                        onFailure = { e ->
                            Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            showRecipientSelector = false
                        }
                    )
                }
            }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview or AR view
        if (isArMode) {
            // AR view - in a real implementation, this would show AR content
            // For now, we'll just show a placeholder Box
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = selectedFilter?.name ?: "AR Mode (No Filter Selected)",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
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
                onClick = { isArMode = !isArMode },
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
                    // Take AR screenshot
                    scope.launch {
                        val uri = arFilterManager.takeScreenshot()
                        if (uri != null) {
                            Toast.makeText(context, "AR photo captured", Toast.LENGTH_SHORT).show()
                            capturedImageUri = uri
                            
                            if (circleId != null) {
                                showCaptionDialog = true
                            } else {
                                showRecipientSelector = true
                            }
                        } else {
                            Toast.makeText(context, "AR capture failed", Toast.LENGTH_SHORT).show()
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

        // Filter selector
        AnimatedVisibility(
            visible = showFilterSelector,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp),
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) {
            FilterSelector(
                filters = availableFilters,
                selectedFilter = selectedFilter,
                onFilterSelected = { filter ->
                    selectedFilter = filter
                    arFilterManager.applyFilter(filter)
                    showFilterSelector = false
                },
                onClearFilter = {
                    selectedFilter = null
                    arFilterManager.clearFilter()
                    showFilterSelector = false
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