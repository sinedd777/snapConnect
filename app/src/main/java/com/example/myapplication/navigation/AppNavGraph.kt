package com.example.myapplication.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.ui.auth.AuthScreen
import com.example.myapplication.ui.auth.CollegeTownSelectionScreen
import com.example.myapplication.ui.camera.CameraScreen
import com.example.myapplication.ui.camera.RecipientSelectorScreen
import com.example.myapplication.ui.circles.CircleDetailScreen
import com.example.myapplication.ui.circles.CirclesScreen
import com.example.myapplication.ui.circles.CreateCircleScreen
import com.example.myapplication.ui.friends.FriendsScreen
import com.example.myapplication.ui.home.HomeScreen
import com.example.myapplication.ui.map.CircleMapScreen
import com.example.myapplication.ui.profile.ProfileScreen
import com.example.myapplication.ui.snap.SnapViewerScreen
import com.google.firebase.auth.FirebaseAuth

object Destinations {
    const val AUTH = "auth"
    const val HOME = "home"
    const val CAMERA = "camera"
    const val FRIENDS = "friends"
    const val SNAP_VIEWER = "snap_viewer/{snapId}"
    const val CIRCLES = "circles"
    const val CREATE_CIRCLE = "create_circle"
    const val CIRCLE_DETAIL = "circle_detail/{circleId}"
    const val CAMERA_FOR_CIRCLE = "camera_for_circle/{circleId}"
    const val PROFILE = "profile"
    const val MAP = "map"
    const val COLLEGE_TOWN_SELECTION = "college_town_selection"
    
    fun snapViewer(snapId: String) = "snap_viewer/$snapId"
    fun circleDetail(circleId: String) = "circle_detail/$circleId"
    fun cameraForCircle(circleId: String) = "camera_for_circle/$circleId"
}

@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    val startDest = if (FirebaseAuth.getInstance().currentUser != null) {
        Destinations.HOME
    } else {
        Destinations.AUTH
    }

    NavHost(navController = navController, startDestination = startDest) {
        composable(Destinations.AUTH) {
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate(Destinations.HOME) {
                        popUpTo(Destinations.AUTH) { inclusive = true }
                    }
                },
                onNavigateToCollegeTownSelection = {
                    navController.navigate(Destinations.COLLEGE_TOWN_SELECTION)
                }
            )
        }
        
        composable(Destinations.HOME) {
            HomeScreen(
                onOpenCamera = {
                    navController.navigate(Destinations.CAMERA)
                },
                onOpenSnapViewer = { snapId ->
                    navController.navigate(Destinations.snapViewer(snapId))
                },
                onOpenFriends = {
                    navController.navigate(Destinations.FRIENDS)
                },
                onOpenCircles = {
                    navController.navigate(Destinations.CIRCLES)
                },
                onOpenProfile = {
                    navController.navigate(Destinations.PROFILE)
                },
                onCreateCircle = {
                    navController.navigate(Destinations.CREATE_CIRCLE)
                },
                onOpenMap = {
                    navController.navigate(Destinations.MAP)
                }
            )
        }
        
        composable(Destinations.CAMERA) {
            CameraScreen(
                onSnapCaptured = {
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = Destinations.CAMERA_FOR_CIRCLE,
            arguments = listOf(
                navArgument("circleId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val circleId = backStackEntry.arguments?.getString("circleId") ?: ""
            CameraScreen(
                circleId = circleId,
                onSnapCaptured = {
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Destinations.FRIENDS) {
            FriendsScreen(
                onBack = {
                    navController.popBackStack()
                },
                onOpenHome = {
                    navController.navigate(Destinations.HOME) {
                        popUpTo(Destinations.HOME) { inclusive = true }
                    }
                },
                onOpenCircles = {
                    navController.navigate(Destinations.CIRCLES)
                },
                onOpenCamera = {
                    navController.navigate(Destinations.CAMERA)
                }
            )
        }
        
        composable(
            route = Destinations.SNAP_VIEWER,
            arguments = listOf(
                navArgument("snapId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val snapId = backStackEntry.arguments?.getString("snapId") ?: ""
            SnapViewerScreen(
                snapId = snapId,
                onClose = {
                    navController.popBackStack()
                }
            )
        }
        
        // Profile screen
        composable(Destinations.PROFILE) {
            ProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLogout = {
                    // Navigate to auth screen and clear back stack
                    navController.navigate(Destinations.AUTH) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }
        
        // Circle screens
        composable(Destinations.CIRCLES) {
            CirclesScreen(
                onCreateCircle = {
                    navController.navigate(Destinations.CREATE_CIRCLE)
                },
                onCircleSelected = { circleId ->
                    navController.navigate(Destinations.circleDetail(circleId))
                },
                onInvitationAction = { circleId, accepted ->
                    // Handle invitation acceptance/rejection
                    // If accepted, navigate to the circle
                    if (accepted) {
                        navController.navigate(Destinations.circleDetail(circleId))
                    }
                },
                onOpenHome = {
                    navController.navigate(Destinations.HOME) {
                        popUpTo(Destinations.HOME) { inclusive = true }
                    }
                },
                onOpenCamera = {
                    navController.navigate(Destinations.CAMERA)
                },
                onOpenFriends = {
                    navController.navigate(Destinations.FRIENDS)
                }
            )
        }
        
        composable(Destinations.CREATE_CIRCLE) {
            CreateCircleScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onCircleCreated = { circleId ->
                    // Navigate to the newly created circle
                    navController.navigate(Destinations.circleDetail(circleId)) {
                        popUpTo(Destinations.CIRCLES)
                    }
                }
            )
        }
        
        composable(
            route = Destinations.CIRCLE_DETAIL,
            arguments = listOf(
                navArgument("circleId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val circleId = backStackEntry.arguments?.getString("circleId") ?: ""
            CircleDetailScreen(
                circleId = circleId,
                onBack = {
                    navController.popBackStack()
                },
                onCaptureForCircle = { selectedCircleId ->
                    navController.navigate(Destinations.cameraForCircle(selectedCircleId))
                },
                onViewSnap = { snapId ->
                    navController.navigate(Destinations.snapViewer(snapId))
                }
            )
        }

        // Add the map screen route
        composable(Destinations.MAP) {
            CircleMapScreen(
                onCircleClick = { circle ->
                    navController.navigate(Destinations.circleDetail(circle.id))
                },
                onCreateCircle = {
                    navController.navigate(Destinations.CREATE_CIRCLE)
                },
                onFilterChange = { /* Filter handled in ViewModel */ },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // Add the college town selection route
        composable(Destinations.COLLEGE_TOWN_SELECTION) {
            CollegeTownSelectionScreen(
                onCollegeTownSelected = { collegeTown ->
                    // Save the college town and navigate to home
                    navController.navigate(Destinations.HOME) {
                        popUpTo(Destinations.AUTH) { inclusive = true }
                    }
                },
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }
    }
} 