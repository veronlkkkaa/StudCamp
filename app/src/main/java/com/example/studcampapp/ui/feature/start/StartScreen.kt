package com.example.studcampapp.ui.feature.start

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studcampapp.ui.theme.*
import com.example.studcampapp.ui.theme.InterFontFamily

@Composable
fun StartScreen(
    onGuestLogin: () -> Unit,
    onAuthLogin: () -> Unit
) {
    // Звёзды как доли экрана (0f..1f)
    val stars = listOf(
        Offset(0.05f, 0.03f), Offset(0.15f, 0.08f), Offset(0.28f, 0.05f),
        Offset(0.08f, 0.14f), Offset(0.20f, 0.20f), Offset(0.35f, 0.11f),
        Offset(0.02f, 0.24f), Offset(0.42f, 0.07f), Offset(0.32f, 0.28f),
        Offset(0.50f, 0.04f), Offset(0.45f, 0.17f), Offset(0.62f, 0.12f),
        Offset(0.57f, 0.22f), Offset(0.72f, 0.07f), Offset(0.68f, 0.19f),
        Offset(0.82f, 0.04f), Offset(0.92f, 0.10f), Offset(0.77f, 0.18f),
        Offset(0.96f, 0.20f), Offset(0.87f, 0.28f),
        Offset(0.10f, 0.38f), Offset(0.25f, 0.44f), Offset(0.40f, 0.36f),
        Offset(0.58f, 0.40f), Offset(0.76f, 0.34f), Offset(0.93f, 0.42f),
        Offset(0.14f, 0.52f), Offset(0.33f, 0.56f), Offset(0.66f, 0.50f),
        Offset(0.88f, 0.55f),
        Offset(0.06f, 0.66f), Offset(0.22f, 0.70f), Offset(0.40f, 0.64f),
        Offset(0.54f, 0.72f), Offset(0.68f, 0.66f), Offset(0.82f, 0.73f),
        Offset(0.94f, 0.62f), Offset(0.13f, 0.80f), Offset(0.30f, 0.84f),
        Offset(0.47f, 0.87f), Offset(0.61f, 0.80f), Offset(0.74f, 0.89f),
        Offset(0.86f, 0.83f), Offset(0.97f, 0.76f), Offset(0.20f, 0.93f),
        Offset(0.50f, 0.95f), Offset(0.72f, 0.97f)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Звёзды
        Canvas(modifier = Modifier.fillMaxSize()) {
            stars.forEachIndexed { i, star ->
                val cx = star.x * size.width
                val cy = star.y * size.height
                val r = if (i % 5 == 0) 3.5f else if (i % 3 == 0) 2.0f else 2.8f
                val alpha = if (i % 4 == 0) 0.6f else 0.9f
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = r,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = Wisteria.copy(alpha = 0.35f),
                    radius = r * 2.5f,
                    center = Offset(cx, cy)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "✦",
                fontSize = 52.sp,
                color = Wisteria,
                fontFamily = InterFontFamily
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "StudcampApp",
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily,
                color = TextPrimary,
                style = TextStyle(
                    shadow = Shadow(
                        color = Purple.copy(alpha = 0.8f),
                        offset = Offset(0f, 4f),
                        blurRadius = 16f
                    )
                )
            )

            Text(
                text = "Общайся с теми, кто рядом",
                fontSize = 14.sp,
                fontFamily = InterFontFamily,
                color = TextSecondary,
                modifier = Modifier.padding(top = 8.dp, bottom = 56.dp)
            )

            // Кнопка войти — объёмная
            Button(
                onClick = onAuthLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(20.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 2.dp
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(PurpleVibrant, Purple)
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Войти",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFontFamily,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Кнопка гость — стеклянная
            OutlinedButton(
                onClick = onGuestLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(
                        colors = listOf(Wisteria, Purple)
                    )
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Purple.copy(alpha = 0.15f)
                )
            ) {
                Text(
                    "Войти как гость",
                    fontSize = 16.sp,
                    fontFamily = InterFontFamily,
                    color = Wisteria,
                    fontWeight = FontWeight.Medium
                )
            }

        }
    }
}