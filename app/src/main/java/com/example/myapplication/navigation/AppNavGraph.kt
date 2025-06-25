package com.example.myapplication.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.ui.auth.AuthScreen
import com.example.myapplication.ui.camera.CameraScreen
import com.example.myapplication.ui.friends.FriendsScreen
import com.example.myapplication.ui.home.HomeScreen
import com.example.myapplication.ui.snap.SnapViewerScreen
import com.google.firebase.auth.FirebaseAuth

object Destinations {
    const val AUTH = "auth"
    const val HOME = "home"
    const val CAMERA = "camera"
    const val FRIENDS = "friends"
    const val SNAP_VIEWER = "snap_viewer/{snapId}"
    
    fun snapViewer(snapId: String) = "snap_viewer/$snapId"
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
            AuthScreen(onAuthSuccess = {
                navController.navigate(Destinations.HOME) {
                    popUpTo(Destinations.AUTH) { inclusive = true }
                }
            })
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
        
        composable(Destinations.FRIENDS) {
            FriendsScreen(
                onBack = {
                    navController.popBackStack()
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
    }
} 