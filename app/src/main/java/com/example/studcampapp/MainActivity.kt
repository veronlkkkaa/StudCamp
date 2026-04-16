package com.example.studcampapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.studcampapp.ui.screens.StartScreen
import com.example.studcampapp.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                StartScreen(
                    onGuestLogin = { /* потом */ },
                    onAuthLogin = { /* потом */ }
                )
            }
        }
    }
}