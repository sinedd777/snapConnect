package com.example.myapplication.ui.circles

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.models.Circle
import com.example.myapplication.ui.theme.SnapBlue
import com.example.myapplication.ui.theme.Success
import com.example.myapplication.ui.theme.Error
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CirclesScreen(
    onCreateCircle: () -> Unit,
    onCircleSelected: (String) -> Unit,
    onInvitationAction: (String, Boolean) -> Unit,
    onOpenHome: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenFriends: () -> Unit,
    viewModel: CirclesViewModel = viewModel()
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("My Circles", "Invitations")
    
    LaunchedEffect(key1 = Unit) {
        viewModel.loadCircles()
        viewModel.loadInvitations()
    }
    
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = false,
                    onClick = onOpenHome,
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = true,
                    onClick = { /* Already on circles */ },
                    icon = { Icon(Icons.Default.Groups, contentDescription = "Circles") },
                    label = { Text("Circles") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onOpenCamera,
                    icon = { Icon(Icons.Default.AddCircle, contentDescription = "Create") },
                    label = { Text("Create") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onOpenFriends,
                    icon = { Icon(Icons.Default.People, contentDescription = "Friends") },
                    label = { Text("Friends") }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateCircle,
                containerColor = SnapBlue,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.semantics {
                    contentDescription = "Create new circle"
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null // Already provided in parent
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = SnapBlue
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { 
                            if (index == 1 && viewModel.invitations.isNotEmpty()) {
                                BadgedBox(
                                    badge = {
                                        Badge { 
                                            Text(
                                                text = viewModel.invitations.size.toString(),
                                                modifier = Modifier.semantics {
                                                    contentDescription = "${viewModel.invitations.size} pending invitations"
                                                }
                                            )
                                        }
                                    }
                                ) {
                                    Text(title)
                                }
                            } else {
                                Text(title)
                            }
                        }
                    )
                }
            }
            
            AnimatedVisibility(
                visible = selectedTabIndex == 0,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                MyCirclesTab(
                    circles = viewModel.circles,
                    isLoading = viewModel.isLoading,
                    onCircleSelected = onCircleSelected
                )
            }
            
            AnimatedVisibility(
                visible = selectedTabIndex == 1,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                InvitationsTab(
                    invitations = viewModel.invitations,
                    isLoading = viewModel.isLoading,
                    onAccept = { circleId -> onInvitationAction(circleId, true) },
                    onDecline = { circleId -> onInvitationAction(circleId, false) }
                )
            }
        }
    }
}

@Composable
fun MyCirclesTab(
    circles: List<Circle>,
    isLoading: Boolean,
    onCircleSelected: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = SnapBlue
            )
        } else if (circles.isEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No Circles Yet",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Create a new Circle to share moments with friends",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
            ) {
                items(
                    items = circles,
                    key = { it.id }
                ) { circle ->
                    CircleCard(
                        circle = circle,
                        onClick = { onCircleSelected(circle.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun CircleCard(
    circle: Circle,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "Circle ${circle.name} with ${circle.members.size} members"
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = circle.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    circle.description?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                if (circle.expiresAt != null) {
                    ExpirationBadge(expiresAt = circle.expiresAt)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.semantics {
                    contentDescription = "${circle.members.size} members, ${if (circle.locationEnabled) "location enabled" else "location disabled"}"
                }
            ) {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Text(
                    text = "${circle.members.size} members",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                if (circle.locationEnabled) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Text(
                        text = "Location enabled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun InvitationsTab(
    invitations: List<Circle>,
    isLoading: Boolean,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = SnapBlue
            )
        } else if (invitations.isEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No Pending Invitations",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "When someone invites you to their circle, it will appear here",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
            ) {
                items(
                    items = invitations,
                    key = { it.id }
                ) { circle ->
                    InvitationCard(
                        circle = circle,
                        onAccept = { onAccept(circle.id) },
                        onDecline = { onDecline(circle.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun InvitationCard(
    circle: Circle,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Invitation to join ${circle.name} circle with ${circle.members.size} members"
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Circle Invitation",
                style = MaterialTheme.typography.labelMedium,
                color = SnapBlue
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = circle.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            circle.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Text(
                    text = "${circle.members.size} members",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (circle.expiresAt != null) {
                    Spacer(modifier = Modifier.width(16.dp))
                    ExpirationBadge(expiresAt = circle.expiresAt)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(1.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Spacer(modifier = Modifier.height(1.dp))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = Error.copy(alpha = 0.1f),
                    onClick = onDecline
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = Error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Decline",
                            color = Error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = Success.copy(alpha = 0.1f),
                    onClick = onAccept
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Success
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Accept",
                            color = Success,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExpirationBadge(expiresAt: Timestamp) {
    val now = Timestamp.now()
    val remainingMs = expiresAt.seconds * 1000 - now.seconds * 1000
    val remainingHours = TimeUnit.MILLISECONDS.toHours(remainingMs)
    
    val (badgeColor, text) = when {
        remainingHours < 1 -> Error to "Expires soon"
        remainingHours < 24 -> Error.copy(alpha = 0.7f) to "Expires in ${remainingHours}h"
        remainingHours < 48 -> MaterialTheme.colorScheme.tertiary to "Expires tomorrow"
        remainingHours < 72 -> MaterialTheme.colorScheme.tertiary to "Expires in 2 days"
        remainingHours < 168 -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f) to "Expires in ${TimeUnit.MILLISECONDS.toDays(remainingMs)} days"
        else -> {
            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f) to "Expires ${sdf.format(Date(expiresAt.seconds * 1000))}"
        }
    }
    
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = badgeColor.copy(alpha = 0.1f),
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = badgeColor,
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .semantics {
                    contentDescription = text
                }
        )
    }
} 