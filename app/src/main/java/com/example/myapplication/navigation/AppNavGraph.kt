package com.example.myapplication.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.auth.AuthScreen
import com.example.myapplication.ui.camera.CameraScreen
import com.example.myapplication.ui.home.HomeScreen
import com.google.firebase.auth.FirebaseAuth

object Destinations {
    const val AUTH   = "auth"
    const val HOME   = "home"
    const val CAMERA = "camera"
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
            HomeScreen(onOpenCamera = {
                navController.navigate(Destinations.CAMERA)
            })
        }
        composable(Destinations.CAMERA) {
            CameraScreen(onSnapCaptured = {
                navController.popBackStack()
            }, onBack = {
                navController.popBackStack()
            })
        }
    }
} 