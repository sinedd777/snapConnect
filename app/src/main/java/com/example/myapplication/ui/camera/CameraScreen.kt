package com.example.myapplication.ui.camera

import android.Manifest
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.permissions.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

@OptIn(com.google.accompanist.permissions.ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    onSnapCaptured: (Uri?) -> Unit,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current

    val imageCapture = remember { ImageCapture.Builder().build() }
    val snapRepo = remember { com.example.myapplication.data.repositories.SnapRepository() }
    val scope = rememberCoroutineScope()
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    var hasPermission by rememberSaveable { mutableStateOf(false) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showRecipientSelector by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }

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

    if (showRecipientSelector && capturedImageUri != null) {
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
        val previewView = remember { PreviewView(context) }

        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize()) { view ->
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

        // Close button
        FloatingActionButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close")
        }

        // Capture button
        FloatingActionButton(
            onClick = {
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
                            showRecipientSelector = true
                        }
                    }
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Icon(Icons.Default.Camera, contentDescription = "Capture")
        }
    }
} 