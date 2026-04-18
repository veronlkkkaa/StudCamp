package com.example.studcampapp.ui.navigation

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.studcampapp.model.ChatClient
import com.example.studcampapp.navigation.Route
import com.example.studcampapp.ui.feature.auth.AuthScreen
import com.example.studcampapp.ui.feature.auth.RegisterScreen
import com.example.studcampapp.ui.feature.chat.ChatListScreen
import com.example.studcampapp.ui.feature.chat.ChatScreen
import com.example.studcampapp.ui.feature.chat.RoomInfoScreen
import com.example.studcampapp.ui.feature.profile.EditProfileScreen
import com.example.studcampapp.ui.feature.profile.ProfileScreen
import com.example.studcampapp.ui.feature.room.CreateRoomScreen
import com.example.studcampapp.ui.feature.room.JoinRoomScreen
import com.example.studcampapp.ui.feature.room.RoomOptionsScreen
import com.example.studcampapp.ui.feature.start.StartScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val focusManager = LocalFocusManager.current

    NavHost(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            },
        navController = navController,
        startDestination = Route.Start
    ) {
        composable<Route.Start> {
            StartScreen(
                onGuestLogin = { navController.navigate(Route.ChatList) },
                onAuthLogin  = { navController.navigate(Route.Auth) },
                onRegister   = { navController.navigate(Route.Register) }
            )
        }

        composable<Route.Auth> {
            AuthScreen(
                onLoginSuccess = {
                    navController.navigate(Route.ChatList) {
                        popUpTo(Route.Start) { inclusive = false }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable<Route.Register> {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Route.ChatList) {
                        popUpTo(Route.Start) { inclusive = false }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable<Route.ChatList> {
            ChatListScreen(
                onRoomConnected = { navController.navigate(Route.Chat) },
                onCreateRoom    = { navController.navigate(Route.RoomOptions) },
                onProfileClick  = { navController.navigate(Route.Profile) }
            )
        }

        composable<Route.Chat> {
            ChatScreen(
                onLeave    = {
                    ChatClient.disconnect()
                    navController.popBackStack()
                },
                onRoomInfo = { navController.navigate(Route.RoomInfo) }
            )
        }

        composable<Route.RoomInfo> {
            RoomInfoScreen(onBack = { navController.popBackStack() })
        }

        composable<Route.Profile> {
            ProfileScreen(
                onBack        = { navController.popBackStack() },
                onLogout      = {
                    navController.navigate(Route.Start) {
                        popUpTo(Route.Start) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onEditProfile = { navController.navigate(Route.EditProfile) }
            )
        }

        composable<Route.EditProfile> {
            EditProfileScreen(onBack = { navController.popBackStack() })
        }

        composable<Route.RoomOptions> {
            RoomOptionsScreen(
                onBack       = { navController.popBackStack() },
                onCreateRoom = { navController.navigate(Route.CreateRoom) },
                onJoinRoom   = { navController.navigate(Route.JoinRoom) }
            )
        }

        composable<Route.CreateRoom> {
            CreateRoomScreen(
                onBack        = { navController.popBackStack() },
                onRoomCreated = { navController.navigate(Route.Chat) }
            )
        }

        composable<Route.JoinRoom> {
            JoinRoomScreen(
                onBack   = { navController.popBackStack() },
                onJoined = { navController.navigate(Route.Chat) }
            )
        }
    }
}
