package com.example.studcampapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        darkColorScheme(
            primary = Purple,
            background = DarkBackground,
            surface = DarkSurface,
        )
    } else {
        lightColorScheme(
            primary = Purple,
            background = LightBackground,
            surface = LightSurface,
        )
    }

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
