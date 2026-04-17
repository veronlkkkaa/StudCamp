package com.example.studcampapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.studcampapp.ui.feature.auth.AuthScreen
import com.example.studcampapp.ui.feature.auth.RegisterScreen
import com.example.studcampapp.ui.feature.chat.ChatListScreen
import com.example.studcampapp.ui.feature.chat.ChatScreen
import com.example.studcampapp.ui.feature.profile.EditProfileScreen
import com.example.studcampapp.ui.feature.profile.ProfileScreen
import com.example.studcampapp.ui.feature.room.CreateRoomScreen
import com.example.studcampapp.ui.feature.room.JoinRoomScreen
import com.example.studcampapp.ui.feature.room.RoomOptionsScreen
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
                onGuestLogin = { navController.navigate("chat_list") },
                onAuthLogin = { navController.navigate("auth") },
                onRegister = { navController.navigate("register") }
            )
        }
        composable("auth") {
            AuthScreen(
                onLoginSuccess = { navController.navigate("chat_list") },
                onBack = { navController.popBackStack() }
            )
        }
        composable("register") {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate("chat_list") {
                        popUpTo("start") { inclusive = false }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable("chat_list") {
            ChatListScreen(
                onRoomClick = { navController.navigate("chat") },
                onCreateRoom = { navController.navigate("room_options") },
                onProfileClick = { navController.navigate("profile") }
            )
        }
        composable("profile") {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate("start") {
                        popUpTo("start") { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onEditProfile = { navController.navigate("edit_profile") }
            )
        }
        composable("edit_profile") {
            EditProfileScreen(onBack = { navController.popBackStack() })
        }
        composable("room_options") {
            RoomOptionsScreen(
                onBack = { navController.popBackStack() },
                onCreateRoom = { navController.navigate("create_room") },
                onJoinRoom = { navController.navigate("join_room") }
            )
        }
        composable("create_room") {
            CreateRoomScreen(
                onBack = { navController.popBackStack() },
                onRoomCreated = { navController.navigate("chat") }
            )
        }
        composable("join_room") {
            JoinRoomScreen(
                onBack = { navController.popBackStack() },
                onJoined = { navController.navigate("chat") }
            )
        }
        composable("chat") {
            ChatScreen(
                onLeave = { navController.popBackStack() }
            )
        }
    }
}
