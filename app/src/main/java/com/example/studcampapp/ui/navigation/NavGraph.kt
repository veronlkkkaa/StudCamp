package com.example.studcampapp.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.studcampapp.backend.server.HostForegroundService
import com.example.studcampapp.backend.server.HostRuntime
import com.example.studcampapp.data.UserStore
import com.example.studcampapp.data.repository.impl.RoomRepositoryImpl
import com.example.studcampapp.data.repository.impl.ChatRepositoryImpl
import com.example.studcampapp.feature.auth.ui.AuthScreen
import com.example.studcampapp.feature.auth.ui.RegisterScreen
import com.example.studcampapp.feature.auth.ui.StartScreen
import com.example.studcampapp.feature.chat.ui.ChatListScreen
import com.example.studcampapp.feature.chat.ui.ChatScreen
import com.example.studcampapp.feature.chat.ui.RoomInfoScreen
import com.example.studcampapp.feature.profile.ui.EditProfileScreen
import com.example.studcampapp.feature.profile.ui.ProfileScreen
import com.example.studcampapp.feature.room.ui.CreateRoomScreen
import com.example.studcampapp.feature.room.ui.JoinRoomScreen
import com.example.studcampapp.feature.room.ui.RoomOptionsScreen
import com.example.studcampapp.navigation.Route

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    val startDestination: Any = if (UserStore.isLoggedIn) Route.ChatList else Route.Start

    NavHost(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            },
        navController = navController,
        startDestination = startDestination
    ) {
        composable<Route.Start> {
            StartScreen(
                onGuestLogin = { nickname ->
                    UserStore.loginAsGuest(nickname)
                    navController.navigate(Route.ChatList) {
                        popUpTo(Route.Start) { inclusive = true }
                    }
                },
                onAuthLogin  = { navController.navigate(Route.Auth) },
                onRegister   = { navController.navigate(Route.Register) }
            )
        }

        composable<Route.Auth> {
            AuthScreen(
                onLoginSuccess = {
                    navController.navigate(Route.ChatList) {
                        popUpTo(Route.Start) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable<Route.Register> {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Route.ChatList) {
                        popUpTo(Route.Start) { inclusive = true }
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
            val isHost = HostRuntime.isRunning()
            ChatScreen(
                isHost     = isHost,
                onLeave    = {
                    // Back navigation should not reset chat session/state.
                    navController.popBackStack()
                },
                onCloseRoom = {
                    ChatRepositoryImpl.disconnect()
                    HostForegroundService.stop(context)
                    navController.popBackStack()
                },
                onRoomInfo = { navController.navigate(Route.RoomInfo) }
            )
        }

        composable<Route.RoomInfo> {
            RoomInfoScreen(
                onBack = { navController.popBackStack() },
                isHost = HostRuntime.isRunning()
            )
        }

        composable<Route.Profile> {
            ProfileScreen(
                onBack        = { navController.popBackStack() },
                onLogout      = {
                    HostForegroundService.stop(context)
                    ChatRepositoryImpl.disconnect()
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
                onJoinRoom   = { navController.navigate(Route.JoinRoom) },
                isHostRunning = HostRuntime.isRunning()
            )
        }

        composable<Route.CreateRoom> {
            CreateRoomScreen(
                onBack        = { navController.popBackStack() },
                onRoomCreated = {
                    navController.navigate(Route.Chat) {
                        popUpTo(Route.ChatList) { inclusive = false }
                    }
                },
                onStartHost = { roomName ->
                    HostForegroundService.start(context, roomName)
                },
                isHostRunning = HostRuntime.isRunning()
            )
        }

        composable<Route.JoinRoom> {
            JoinRoomScreen(
                onBack          = { navController.popBackStack() },
                onJoined        = {
                    navController.navigate(Route.Chat) {
                        popUpTo(Route.ChatList) { inclusive = false }
                    }
                },
                isHostRunning   = HostRuntime.isRunning(),
                hostedRoomName  = ChatRepositoryImpl.currentRoomName
            )
        }
    }
}
