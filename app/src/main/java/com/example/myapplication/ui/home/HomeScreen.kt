package com.example.myapplication.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.models.Snap
import com.example.myapplication.data.repositories.SnapRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenCamera: () -> Unit,
    onOpenSnapViewer: (String) -> Unit = {},
    onOpenFriends: () -> Unit = {},
    onOpenCircles: () -> Unit = {}
) {
    val snapRepository = remember { SnapRepository() }
    val coroutineScope = rememberCoroutineScope()
    
    var snaps by remember { mutableStateOf<List<Snap>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    // Load snaps on first composition
    LaunchedEffect(Unit) {
        loadSnaps(snapRepository) { result ->
            isLoading = false
            result.fold(
                onSuccess = { snapsList -> snaps = snapsList },
                onFailure = { e -> error = e.message }
            )
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SnapCircle") },
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            loadSnaps(snapRepository) { result ->
                                isLoading = false
                                result.fold(
                                    onSuccess = { snapsList -> snaps = snapsList },
                                    onFailure = { e -> error = e.message }
                                )
                            }
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = { onOpenCircles() }) {
                        Icon(Icons.Default.Groups, contentDescription = "Circles")
                    }
                    IconButton(onClick = { onOpenFriends() }) {
                        Icon(Icons.Default.Person, contentDescription = "Friends")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onOpenCamera) {
                Icon(Icons.Default.Camera, contentDescription = "Take Snap")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (error != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Error: $error")
                    Button(onClick = {
                        error = null
                        coroutineScope.launch {
                            isLoading = true
                            loadSnaps(snapRepository) { result ->
                                isLoading = false
                                result.fold(
                                    onSuccess = { snapsList -> snaps = snapsList },
                                    onFailure = { e -> error = e.message }
                                )
                            }
                        }
                    }) {
                        Text("Retry")
                    }
                }
            } else if (snaps.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("No snaps yet")
                    Button(onClick = onOpenCamera) {
                        Text("Take a Snap")
                    }
                    Button(onClick = onOpenCircles) {
                        Text("Explore Circles")
                    }
                    Button(onClick = onOpenFriends) {
                        Text("Add Friends")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Button(
                            onClick = onOpenCircles,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Groups, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("My Circles")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Recent Snaps",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    items(snaps) { snap ->
                        SnapItem(
                            snap = snap,
                            onClick = { onOpenSnapViewer(snap.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SnapItem(
    snap: Snap,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar placeholder
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = snap.senderName?.firstOrNull()?.toString() ?: "?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = snap.senderName ?: "Unknown",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = formatTimestamp(snap.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (!snap.isViewed) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Timestamp): String {
    val date = timestamp.toDate()
    val now = Date()
    
    // If today, show time only
    return if (isSameDay(date, now)) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
    } else {
        SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
    }
}

private fun isSameDay(date1: Date, date2: Date): Boolean {
    val cal1 = Calendar.getInstance().apply { time = date1 }
    val cal2 = Calendar.getInstance().apply { time = date2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private suspend fun loadSnaps(
    repository: SnapRepository,
    callback: (Result<List<Snap>>) -> Unit
) {
    val result = repository.getSnapsForUser()
    callback(result)
} 