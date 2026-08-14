package com.smarthome.monitor.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.smarthome.monitor.ui.screens.camera.CameraScreen
import com.smarthome.monitor.ui.screens.home.HomeScreen
import com.smarthome.monitor.ui.screens.login.LoginScreen
import com.smarthome.monitor.ui.screens.reports.UsageReportScreen
import com.smarthome.monitor.viewmodel.AuthViewModel
import com.smarthome.monitor.viewmodel.HomeViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    homeViewModel: HomeViewModel
) {
    val authState by authViewModel.uiState.collectAsState()

    val startDestination = if (authState.user != null) Screen.Home.route else Screen.Login.route

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Screen.Login.route) {
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                homeViewModel = homeViewModel,
                authViewModel = authViewModel,
                onNavigateToCamera = { deviceId ->
                    navController.navigate(Screen.Camera.createRoute(deviceId))
                },
                onNavigateToReports = {
                    navController.navigate(Screen.Reports.route)
                },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Camera.route,
            arguments = listOf(navArgument("deviceId") { type = NavType.StringType })
        ) { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: ""
            CameraScreen(
                deviceId = deviceId,
                homeViewModel = homeViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Reports.route) {
            UsageReportScreen(
                homeViewModel = homeViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
