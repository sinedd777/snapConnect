package com.example.myapplication.ui.circles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.myapplication.data.models.Circle
import com.example.myapplication.data.models.Snap
import com.example.myapplication.data.models.User
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleDetailScreen(
    circleId: String,
    onBack: () -> Unit,
    onViewSnap: (String) -> Unit,
    onCaptureForCircle: (String) -> Unit,
    viewModel: CircleDetailViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // State
    var showInviteDialog by remember { mutableStateOf(false) }
    var inviteEmail by remember { mutableStateOf("") }
    
    // Load data
    LaunchedEffect(circleId) {
        viewModel.loadCircleDetails(circleId)
        viewModel.loadCircleSnaps(circleId)
    }
    
    // Handle errors
    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = viewModel.circle?.name ?: "Circle",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Invite members button (only for creator)
                    if (viewModel.isCreator) {
                        IconButton(onClick = { showInviteDialog = true }) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Invite Members")
                        }
                    }
                    
                    // Leave circle button (for members who are not the creator)
                    if (viewModel.isMember && !viewModel.isCreator) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    viewModel.leaveCircle()
                                    if (viewModel.errorMessage == null) {
                                        onBack()
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Leave Circle")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (viewModel.isMember) {
                FloatingActionButton(
                    onClick = { onCaptureForCircle(circleId) }
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Capture for Circle")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (viewModel.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (viewModel.circle == null) {
                Text(
                    text = "Circle not found",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Circle info
                    item {
                        CircleInfoSection(
                            circle = viewModel.circle!!,
                            memberCount = viewModel.circle?.members?.size ?: 0
                        )
                    }
                    
                    // Location info (if enabled)
                    if (viewModel.circle?.locationEnabled == true) {
                        item {
                            LocationInfoSection(
                                latitude = viewModel.circle?.locationLat,
                                longitude = viewModel.circle?.locationLng,
                                radius = viewModel.circle?.locationRadius
                            )
                        }
                    }
                    
                    // Members section
                    item {
                        Text(
                            text = "Members",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    
                    // Member list
                    items(viewModel.members) { member ->
                        MemberItem(
                            user = member,
                            isCreator = member.id == viewModel.circle?.creatorId
                        )
                    }
                    
                    // Content section
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Circle Content",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    
                    // Snaps
                    if (viewModel.snaps.isEmpty()) {
                        item {
                            Text(
                                text = "No content yet. Be the first to share!",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp)
                            )
                        }
                    } else {
                        items(viewModel.snaps) { snap ->
                            SnapItem(
                                snap = snap,
                                onClick = { onViewSnap(snap.id) }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Invite dialog
    if (showInviteDialog) {
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text("Invite to Circle") },
            text = {
                Column {
                    Text("Enter email address to invite:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inviteEmail,
                        onValueChange = { inviteEmail = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            viewModel.inviteUserByEmail(inviteEmail)
                            inviteEmail = ""
                            showInviteDialog = false
                        }
                    },
                    enabled = inviteEmail.isNotEmpty()
                ) {
                    Text("Invite")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInviteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CircleInfoSection(circle: Circle, memberCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        // Description
        if (!circle.description.isNullOrEmpty()) {
            Text(
                text = circle.description,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Expiration
        val formatter = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
        Text(
            text = "Expires: ${formatter.format(circle.expiresAt?.toDate() ?: Date())}",
            style = MaterialTheme.typography.bodySmall
        )
        
        // Privacy
        Text(
            text = if (circle.isPrivate) "Private Circle" else "Public Circle",
            style = MaterialTheme.typography.bodySmall
        )
        
        // Member count
        Text(
            text = "$memberCount member${if (memberCount != 1) "s" else ""}",
            style = MaterialTheme.typography.bodySmall
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    }
}

@Composable
fun LocationInfoSection(latitude: Double?, longitude: Double?, radius: Double?) {
    if (latitude != null && longitude != null && radius != null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "Location-Based Circle",
                style = MaterialTheme.typography.titleSmall
            )
            
            Text(
                text = "Content is only visible when you're within ${radius.toInt()} meters of the circle location.",
                style = MaterialTheme.typography.bodySmall
            )
            
            // Here you could add a small map preview if desired
            
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }
}

@Composable
fun MemberItem(user: User, isCreator: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile picture
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (user.profilePictureUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(user.profilePictureUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Profile picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = user.username?.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        
        // User info
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = user.username ?: user.email ?: "Unknown User",
                style = MaterialTheme.typography.bodyMedium
            )
            
            if (isCreator) {
                Text(
                    text = "Creator",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnapItem(snap: Snap, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Sender and time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = snap.senderName ?: "Unknown",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                val formatter = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                Text(
                    text = formatter.format(snap.createdAt.toDate()),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // Caption if available
            if (!snap.caption.isNullOrEmpty()) {
                Text(
                    text = snap.caption,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            // Preview indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = "Tap to view",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
} 