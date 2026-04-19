package com.example.studcampapp.feature.chat.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studcampapp.ui.theme.InterFontFamily

private val rainbowColors = listOf(
    Color(0xFFFF0000),
    Color(0xFFFF6600),
    Color(0xFFFFFF00),
    Color(0xFF00CC00),
    Color(0xFF0066FF),
    Color(0xFF9900FF),
)

@Composable
fun NyanCatProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    label: String = "Загружается..."
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(200, easing = LinearEasing),
        label = "nyan_progress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "sparkle")
    val sparkleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(350), RepeatMode.Reverse),
        label = "sparkle_alpha"
    )
    val starOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Restart),
        label = "star_offset"
    )

    Column(modifier = modifier) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(Color(0xFF0D0828))
        ) {
            val totalWidth = maxWidth

            // Rainbow fill
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .background(Brush.horizontalGradient(rainbowColors))
            )

            // Stars in the trail
            if (animatedProgress > 0.08f) {
                val starsText = listOf("✦", "✧", "★", "✦", "✧")[starOffset.toInt() % 5]
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) {
                        Text(
                            text = starsText,
                            color = Color.White.copy(alpha = sparkleAlpha),
                            fontSize = 8.sp
                        )
                    }
                }
            }

            // Cat at the leading edge
            val catX = (totalWidth * animatedProgress - 20.dp).coerceAtLeast(0.dp)
            Text(
                text = "🐱",
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = catX),
                fontSize = 16.sp
            )
        }

        Spacer(Modifier.height(3.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontFamily = InterFontFamily,
                color = Color.White.copy(alpha = 0.6f)
            )
            Text(
                text = "${(animatedProgress * 100).toInt()}%",
                fontSize = 10.sp,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}
