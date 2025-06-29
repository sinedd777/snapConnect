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
    onBack: () -> Unit,
    onOpenHome: () -> Unit,
    onOpenCircles: () -> Unit,
    onOpenCamera: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Friends", "Requests", "Search")
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    // State
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<User>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    
    // Friends state
    var friends by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoadingFriends by remember { mutableStateOf(true) }
    var friendsError by remember { mutableStateOf<String?>(null) }
    
    // Friend requests state
    var friendRequests by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var isLoadingRequests by remember { mutableStateOf(true) }
    var requestsError by remember { mutableStateOf<String?>(null) }
    
    val friendRepository = remember { FriendRepository() }
    
    // Load friends and requests
    LaunchedEffect(Unit) {
        // Load friends
        val friendsResult = friendRepository.getFriends()
        isLoadingFriends = false
        friendsResult.fold(
            onSuccess = { friendsList -> friends = friendsList },
            onFailure = { e -> friendsError = e.message }
        )
        
        // Load friend requests
        val requestsResult = friendRepository.getPendingFriendRequests()
        isLoadingRequests = false
        requestsResult.fold(
            onSuccess = { requestsList -> friendRequests = requestsList },
            onFailure = { e -> requestsError = e.message }
        )
    }
    
    // Search handler
    val handleSearch = {
        if (searchQuery.isNotEmpty()) {
            isSearching = true
            searchError = null
            focusManager.clearFocus()
            
            coroutineScope.launch {
                val result = friendRepository.searchUsers(searchQuery)
                isSearching = false
                result.fold(
                    onSuccess = { users -> searchResults = users },
                    onFailure = { e -> searchError = e.message }
                )
            }
        }
    }
    
    // Friend request handlers
    val handleAcceptRequest = { request: FriendRequest ->
        coroutineScope.launch {
            val result = friendRepository.acceptFriendRequest(request.id)
            result.fold(
                onSuccess = {
                    // Remove from requests and add to friends
                    friendRequests = friendRequests.filter { it.id != request.id }
                    friends = friends + request.requesterDetails!!
                    snackbarHostState.showSnackbar("Friend request accepted")
                },
                onFailure = { e ->
                    snackbarHostState.showSnackbar("Failed to accept request: ${e.message}")
                }
            )
        }
    }
    
    val handleDeclineRequest = { request: FriendRequest ->
        coroutineScope.launch {
            val result = friendRepository.rejectFriendRequest(request.id)
            result.fold(
                onSuccess = {
                    friendRequests = friendRequests.filter { it.id != request.id }
                    snackbarHostState.showSnackbar("Friend request declined")
                },
                onFailure = { e ->
                    snackbarHostState.showSnackbar("Failed to decline request: ${e.message}")
                }
            )
        }
    }
    
    val handleSendRequest = { user: User ->
        coroutineScope.launch {
            val result = friendRepository.sendFriendRequest(user.id)
            result.fold(
                onSuccess = {
                    snackbarHostState.showSnackbar("Friend request sent")
                    // Remove from search results
                    searchResults = searchResults.filter { it.id != user.id }
                },
                onFailure = { e ->
                    snackbarHostState.showSnackbar("Failed to send request: ${e.message}")
                }
            )
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Friends") }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                NavigationBarItem(
                    selected = false,
                    onClick = onOpenHome,
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onOpenCamera,
                    icon = { Icon(Icons.Default.AddCircle, contentDescription = "Create") },
                    label = { Text("Create") }
                )
                NavigationBarItem(
                    selected = true,
                    onClick = onOpenCircles,
                    icon = { Icon(Icons.Default.Groups, contentDescription = "Circles") },
                    label = { Text("Circles") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { /* Already on friends */ },
                    icon = { Icon(Icons.Default.People, contentDescription = "Friends") },
                    label = { Text("Friends") }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search users...") },
                trailingIcon = {
                    Row {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                searchError = null
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                            IconButton(onClick = { handleSearch() }) {
                                Icon(Icons.Default.Send, contentDescription = "Search")
                            }
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { handleSearch() }),
                isError = searchError != null,
                supportingText = searchError?.let { { Text(it) } }
            )
            
            // Show search results if any
            if (searchResults.isNotEmpty()) {
                SearchResultsSection(
                    searchResults = searchResults,
                    isSearching = isSearching,
                    onSendRequest = { user -> handleSendRequest(user) },
                    onBack = {
                        searchResults = emptyList()
                        searchQuery = ""
                        searchError = null
                    }
                )
            } else {
                // Tabs for Friends and Requests
                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) }
                        )
                    }
                }
                
                when (selectedTabIndex) {
                    0 -> FriendsTab(
                        friends = friends,
                        isLoading = isLoadingFriends,
                        error = friendsError,
                        onRefresh = {
                            coroutineScope.launch {
                                isLoadingFriends = true
                                friendsError = null
                                val result = friendRepository.getFriends()
                                isLoadingFriends = false
                                result.fold(
                                    onSuccess = { friendsList -> friends = friendsList },
                                    onFailure = { e -> friendsError = e.message }
                                )
                            }
                        }
                    )
                    1 -> RequestsTab(
                        requests = friendRequests,
                        isLoading = isLoadingRequests,
                        error = requestsError,
                        onAccept = { request -> handleAcceptRequest(request) },
                        onReject = { request -> handleDeclineRequest(request) }
                    )
                    2 -> {
                        // Search tab is handled by the search bar above
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultsSection(
    searchResults: List<User>,
    isSearching: Boolean,
    onSendRequest: (User) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (isSearching) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(searchResults) { user ->
                    UserSearchItem(
                        user = user,
                        onSendRequest = { onSendRequest(user) }
                    )
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
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
                text = friend.username?.firstOrNull()?.toString() 
                    ?: friend.email?.firstOrNull()?.toString() 
                    ?: "?",
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

@Composable
fun RequestsTab(
    requests: List<FriendRequest>,
    isLoading: Boolean,
    error: String?,
    onAccept: (FriendRequest) -> Unit,
    onReject: (FriendRequest) -> Unit
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
            }
        } else if (requests.isEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No pending requests")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(requests) { request ->
                    RequestItem(
                        request = request,
                        onAccept = { onAccept(request) },
                        onReject = { onReject(request) }
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
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
                text = request.requesterDetails?.username?.firstOrNull()?.toString() 
                    ?: request.requesterDetails?.email?.firstOrNull()?.toString() 
                    ?: "?",
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = request.requesterDetails?.username ?: "No username",
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = request.requesterDetails?.email ?: "No email",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Accept")
            }
            
            Button(
                onClick = onReject,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Reject")
            }
        }
    }
} 