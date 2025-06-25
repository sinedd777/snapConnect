package com.example.myapplication.ui.snap

import android.graphics.Bitmap
import android.view.ViewTreeObserver
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.myapplication.data.models.Snap
import com.example.myapplication.data.repositories.SnapRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnapViewerScreen(
    snapId: String,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val snapRepository = remember { SnapRepository() }
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current
    
    var snap by remember { mutableStateOf<Snap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var remainingSeconds by remember { mutableStateOf(10) }
    var hasMarkedViewed by remember { mutableStateOf(false) }
    
    // Detect screenshots
    val callback = remember {
        ViewTreeObserver.OnPreDrawListener {
            // This is a simple placeholder for screenshot detection
            // In a real app, you'd use a more robust method
            coroutineScope.launch {
                if (snap != null && !hasMarkedViewed) {
                    snapRepository.markSnapViewed(snap!!.id)
                    hasMarkedViewed = true
                }
            }
            true
        }
    }
    
    DisposableEffect(view) {
        val observer = view.viewTreeObserver
        observer.addOnPreDrawListener(callback)
        onDispose {
            observer.removeOnPreDrawListener(callback)
        }
    }
    
    // Load snap
    LaunchedEffect(snapId) {
        val result = snapRepository.getSnapById(snapId)
        isLoading = false
        result.fold(
            onSuccess = { 
                snap = it
                // Mark as viewed
                snapRepository.markSnapViewed(snapId)
                hasMarkedViewed = true
            },
            onFailure = { e -> error = e.message }
        )
    }
    
    // Auto-destruction countdown
    LaunchedEffect(snap) {
        if (snap != null) {
            while (remainingSeconds > 0) {
                delay(1000)
                remainingSeconds--
            }
            // Auto-close after countdown
            delay(500)
            onClose()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(snap?.senderName ?: "Snap") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            } else if (error != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Error: $error",
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onClose) {
                        Text("Close")
                    }
                }
            } else if (snap != null) {
                // Display the snap image
                val painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(context)
                        .data(snap!!.mediaUrl)
                        .crossfade(true)
                        .build()
                )
                
                val imageState = painter.state
                if (imageState is AsyncImagePainter.State.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.White
                    )
                }
                
                Image(
                    painter = painter,
                    contentDescription = "Snap",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                
                // Countdown timer
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.medium)
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = remainingSeconds.toString(),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
} 