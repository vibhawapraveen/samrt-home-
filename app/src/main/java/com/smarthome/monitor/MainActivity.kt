package com.smarthome.monitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.smarthome.monitor.ui.navigation.NavGraph
import com.smarthome.monitor.ui.theme.BackgroundDark
import com.smarthome.monitor.ui.theme.SmartHomeTheme
import com.smarthome.monitor.viewmodel.AuthViewModel
import com.smarthome.monitor.viewmodel.HomeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartHomeAppUI()
        }
    }
}

@Composable
fun SmartHomeAppUI() {
    SmartHomeTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = BackgroundDark
        ) {
            val navController = rememberNavController()
            val authViewModel: AuthViewModel = viewModel()
            val homeViewModel: HomeViewModel = viewModel()

            NavGraph(
                navController = navController,
                authViewModel = authViewModel,
                homeViewModel = homeViewModel
            )
        }
    }
}
