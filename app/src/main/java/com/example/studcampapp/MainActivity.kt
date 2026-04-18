package com.example.studcampapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.studcampapp.backend.server.HostRuntime
import com.example.studcampapp.model.RoomHistoryStore
import com.example.studcampapp.ui.navigation.NavGraph
import com.example.studcampapp.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HostRuntime.start(cacheDir)
        RoomHistoryStore.init(this)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(0x662D1B69)
        )
        setContent {
            AppTheme {
                NavGraph()
            }
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            HostRuntime.stop()
        }
        super.onDestroy()
    }
}