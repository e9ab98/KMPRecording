package com.e9ab98.kmprecording

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.e9ab98.kmprecording.ui.screen.RecordingScreenActual
import com.e9ab98.kmprecording.ui.screen.SettingsScreenActual
import com.e9ab98.kmprecording.ui.screen.VideoListScreenActual

sealed class Screen(val route: String) {
    object Recording : Screen("recording")
    object VideoList : Screen("video_list")
    object Settings : Screen("settings")
}

@Composable
fun App() {
    MaterialTheme {
        val navController = rememberNavController()

        AppNavigation(navController)
    }
}

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Recording.route
    ) {
        composable(Screen.Recording.route) {
            RecordingScreenActual(
                onNavigateToVideoList = {
                    navController.navigate(Screen.VideoList.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.VideoList.route) {
            VideoListScreenActual(
                onNavigateToRecording = {
                    navController.popBackStack()
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreenActual(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
