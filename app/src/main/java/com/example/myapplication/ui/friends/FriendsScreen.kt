package com.example.myapplication.ui.friends

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.models.FriendRequest
import com.example.myapplication.data.models.User
import com.example.myapplication.data.repositories.FriendRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    onBack: () -> Unit
) {
    val friendRepository = remember { FriendRepository() }
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    
    var friends by remember { mutableStateOf<List<User>>(emptyList()) }
    var pendingRequests by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var searchResults by remember { mutableStateOf<List<User>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSearching by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var showSearchResults by remember { mutableStateOf(false) }
    var friendRequestSuccess by remember { mutableStateOf<String?>(null) }
    
    // Load friends and requests on first composition
    LaunchedEffect(Unit) {
        loadFriendsAndRequests(friendRepository) { friendsResult, requestsResult ->
            isLoading = false
            friendsResult.fold(
                onSuccess = { friendsList -> friends = friendsList },
                onFailure = { e -> error = e.message }
            )
            requestsResult.fold(
                onSuccess = { requestsList -> pendingRequests = requestsList },
                onFailure = { e -> error = e.message }
            )
        }
    }
    
    // Auto-hide success message after 3 seconds
    LaunchedEffect(friendRequestSuccess) {
        if (friendRequestSuccess != null) {
            delay(3000)
            friendRequestSuccess = null
        }
    }
    
    // Function to perform search
    fun performSearch() {
        if (searchQuery.length < 2) {
            searchError = "Please enter at least 2 characters"
            return
        }
        
        focusManager.clearFocus()
        isSearching = true
        searchError = null
        showSearchResults = true
        
        coroutineScope.launch {
            val result = friendRepository.searchUsers(searchQuery)
            isSearching = false
            result.fold(
                onSuccess = { users -> 
                    searchResults = users
                    if (users.isEmpty()) {
                        searchError = "No users found matching '$searchQuery'"
                    }
                },
                onFailure = { e -> 
                    searchError = "Search failed: ${e.message}"
                    searchResults = emptyList()
                }
            )
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Friends") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it
                    searchError = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search by username or email") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        Row {
                            IconButton(onClick = { 
                                searchQuery = ""
                                searchError = null
                                showSearchResults = false
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                            IconButton(onClick = { performSearch() }) {
                                Icon(Icons.Default.Send, contentDescription = "Search")
                            }
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { performSearch() }),
                isError = searchError != null,
                supportingText = searchError?.let { { Text(it) } }
            )
            
            // Friend request success message
            AnimatedVisibility(visible = friendRequestSuccess != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Success",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = friendRequestSuccess ?: "",
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            // Show search results if any
            if (showSearchResults) {
                SearchResultsSection(
                    searchResults = searchResults,
                    isSearching = isSearching,
                    onSendRequest = { userId, username ->
                        coroutineScope.launch {
                            val result = friendRepository.sendFriendRequest(userId)
                            result.fold(
                                onSuccess = { 
                                    // Remove from search results
                                    searchResults = searchResults.filter { it.id != userId }
                                    friendRequestSuccess = "Friend request sent to $username"
                                },
                                onFailure = { e -> searchError = "Failed to send request: ${e.message}" }
                            )
                        }
                    },
                    onBack = {
                        showSearchResults = false
                        searchQuery = ""
                        searchError = null
                    }
                )
            } else {
                // Tabs for Friends and Requests
                TabRow(selectedTabIndex = activeTab) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("Friends (${friends.size})") }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("Requests (${pendingRequests.size})") }
                    )
                }
                
                when (activeTab) {
                    0 -> FriendsTab(
                        friends = friends,
                        isLoading = isLoading,
                        error = error,
                        onRefresh = {
                            coroutineScope.launch {
                                isLoading = true
                                error = null
                                loadFriendsAndRequests(friendRepository) { friendsResult, requestsResult ->
                                    isLoading = false
                                    friendsResult.fold(
                                        onSuccess = { friendsList -> friends = friendsList },
                                        onFailure = { e -> error = e.message }
                                    )
                                    requestsResult.fold(
                                        onSuccess = { requestsList -> pendingRequests = requestsList },
                                        onFailure = { e -> error = e.message }
                                    )
                                }
                            }
                        }
                    )
                    1 -> RequestsTab(
                        requests = pendingRequests,
                        isLoading = isLoading,
                        error = error,
                        onAccept = { requestId ->
                            coroutineScope.launch {
                                val result = friendRepository.acceptFriendRequest(requestId)
                                result.fold(
                                    onSuccess = {
                                        // Refresh lists
                                        loadFriendsAndRequests(friendRepository) { friendsResult, requestsResult ->
                                            friendsResult.fold(
                                                onSuccess = { friendsList -> friends = friendsList },
                                                onFailure = { e -> error = e.message }
                                            )
                                            requestsResult.fold(
                                                onSuccess = { requestsList -> pendingRequests = requestsList },
                                                onFailure = { e -> error = e.message }
                                            )
                                        }
                                        friendRequestSuccess = "Friend request accepted"
                                    },
                                    onFailure = { e -> error = e.message }
                                )
                            }
                        },
                        onReject = { requestId ->
                            coroutineScope.launch {
                                val result = friendRepository.rejectFriendRequest(requestId)
                                result.fold(
                                    onSuccess = {
                                        // Remove from list
                                        pendingRequests = pendingRequests.filter { it.id != requestId }
                                        friendRequestSuccess = "Friend request rejected"
                                    },
                                    onFailure = { e -> error = e.message }
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SearchResultsSection(
    searchResults: List<User>,
    isSearching: Boolean,
    onSendRequest: (String, String?) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back to friends")
            }
            Text(
                text = "Search Results",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
        }
        
        Box(modifier = Modifier.fillMaxSize()) {
            if (isSearching) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (searchResults.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.PersonSearch,
                        contentDescription = "No results",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "No users found",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Try a different search term",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(searchResults) { user ->
                        UserSearchItem(
                            user = user,
                            onSendRequest = { onSendRequest(user.id, user.username) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserSearchItem(
    user: User,
    onSendRequest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
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
                    text = user.username?.firstOrNull()?.toString() 
                        ?: user.email?.firstOrNull()?.toString() 
                        ?: "?",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.username ?: "No username",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = user.email ?: "No email",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Button(onClick = onSendRequest) {
                Text("Add")
            }
        }
    }
}

@Composable
fun FriendsTab(
    friends: List<User>,
    isLoading: Boolean,
    error: String?,
    onRefresh: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (error != null) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Error: $error")
                Button(onClick = onRefresh) {
                    Text("Retry")
                }
            }
        } else if (friends.isEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No friends yet")
                Text(
                    text = "Search for users to add friends",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(friends) { friend ->
                    FriendItem(friend = friend)
                }
            }
        }
    }
}

@Composable
fun FriendItem(friend: User) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
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
        }
    }
}

@Composable
fun RequestsTab(
    requests: List<FriendRequest>,
    isLoading: Boolean,
    error: String?,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (error != null) {
            Text(
                text = "Error: $error",
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            )
        } else if (requests.isEmpty()) {
            Text(
                text = "No pending requests",
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(requests) { request ->
                    RequestItem(
                        request = request,
                        onAccept = { onAccept(request.id) },
                        onReject = { onReject(request.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun RequestItem(
    request: FriendRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
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
                        text = request.requesterDetails?.email?.firstOrNull()?.toString() ?: "?",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = request.requesterDetails?.username ?: "Unknown user",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = request.requesterDetails?.email ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Reject")
                }
                
                Button(onClick = onAccept) {
                    Text("Accept")
                }
            }
        }
    }
}

private suspend fun loadFriendsAndRequests(
    repository: FriendRepository,
    callback: (Result<List<User>>, Result<List<FriendRequest>>) -> Unit
) {
    val friendsResult = repository.getFriends()
    val requestsResult = repository.getPendingFriendRequests()
    callback(friendsResult, requestsResult)
} 