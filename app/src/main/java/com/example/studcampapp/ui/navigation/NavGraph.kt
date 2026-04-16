package com.example.studcampapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.studcampapp.ui.feature.auth.AuthScreen
import com.example.studcampapp.ui.feature.chat.ChatScreen
import com.example.studcampapp.ui.feature.start.StartScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "start"
    ) {
        composable("start") {
            StartScreen(
                onGuestLogin = { navController.navigate("chat") },
                onAuthLogin = { navController.navigate("auth") }
            )
        }
        composable("auth") {
            AuthScreen(
                onLoginSuccess = {},
                onBack = { navController.popBackStack() }
            )
        }
        composable("chat") {
            ChatScreen(
                onLeave = { navController.popBackStack() }
            )
        }
    }
}
