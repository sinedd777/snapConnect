package com.example.myapplication.ui.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.models.User
import com.example.myapplication.data.repositories.FriendRepository
import com.example.myapplication.ui.theme.ScreenshotProtection
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipientSelectorScreen(
    onBack: () -> Unit,
    onSendToRecipients: (List<String>, List<String>) -> Unit
) {
    // Apply screenshot protection
    ScreenshotProtection()
    
    val context = LocalContext.current
    val friendRepository = remember { FriendRepository() }
    val coroutineScope = rememberCoroutineScope()
    
    var friends by remember { mutableStateOf<List<User>>(emptyList()) }
    var circles by remember { mutableStateOf<List<com.example.myapplication.data.models.Circle>>(emptyList()) }
    var selectedFriends by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedCircles by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var isSending by remember { mutableStateOf(false) }
    
    // Load friends and circles on first composition
    val circleRepository = remember { com.example.myapplication.data.repositories.CircleRepository() }
    LaunchedEffect(Unit) {
        val friendResult = friendRepository.getFriends()
        val circleResult = circleRepository.getUserCircles()
        isLoading = false
        friendResult.fold(
            onSuccess = { friendsList -> friends = friendsList },
            onFailure = { e -> error = e.message }
        )
        circleResult.fold(
            onSuccess = { circleList -> circles = circleList },
            onFailure = { e -> error = e.message ?: error }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Send to") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val hasSelection = selectedFriends.isNotEmpty() || selectedCircles.isNotEmpty()

                    if (isSending) {
                        // Show small progress indicator while sending
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(
                            onClick = {
                                isSending = true
                                onSendToRecipients(selectedFriends.toList(), selectedCircles.toList())
                            },
                            enabled = hasSelection
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "Send",
                                tint = if (hasSelection) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                }
                            )
                        }
                    }
                }
            )
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
                        isLoading = true
                        coroutineScope.launch {
                            val result = friendRepository.getFriends()
                            isLoading = false
                            result.fold(
                                onSuccess = { friendsList -> friends = friendsList },
                                onFailure = { e -> error = e.message }
                            )
                        }
                    }) {
                        Text("Retry")
                    }
                }
            } else if (friends.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("No friends yet")
                    Text(
                        text = "Add friends to send snaps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = onBack) {
                        Text("Go Back")
                    }
                }
            } else {
                Column {
                    // Selected count
                    val totalSelected = selectedFriends.size + selectedCircles.size
                    if (totalSelected > 0) {
                        Text(
                            text = "$totalSelected selected",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // Friends and circles list
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Friends section
                        if (friends.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Friends",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            items(friends) { friend ->
                                FriendSelectionItem(
                                    friend = friend,
                                    isSelected = selectedFriends.contains(friend.id),
                                    onToggleSelection = {
                                        selectedFriends = if (selectedFriends.contains(friend.id)) {
                                            selectedFriends - friend.id
                                        } else {
                                            selectedFriends + friend.id
                                        }
                                    }
                                )
                            }
                        }

                        // Circles section
                        if (circles.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Circles",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            items(circles) { circle ->
                                CircleSelectionItem(
                                    circle = circle,
                                    isSelected = selectedCircles.contains(circle.id),
                                    onToggleSelection = {
                                        selectedCircles = if (selectedCircles.contains(circle.id)) {
                                            selectedCircles - circle.id
                                        } else {
                                            selectedCircles + circle.id
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FriendSelectionItem(
    friend: User,
    isSelected: Boolean,
    onToggleSelection: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleSelection),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
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
                    text = friend.email?.firstOrNull()?.toString() ?: "?",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = friend.username ?: "No username",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = friend.email ?: "No email",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun CircleSelectionItem(
    circle: com.example.myapplication.data.models.Circle,
    isSelected: Boolean,
    onToggleSelection: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleSelection),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar placeholder (first letter of circle name)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = circle.name.firstOrNull()?.toString() ?: "?",
                    color = MaterialTheme.colorScheme.onSecondary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = circle.name,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!circle.description.isNullOrEmpty()) {
                    Text(
                        text = circle.description ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
} 