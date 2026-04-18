package com.example.studcampapp.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val Purple = Color(0xFF7B4BBF)
val PurpleLight = Color(0xFF9E82D9)
val PurpleVibrant = Color(0xFF5317A6)
val Wisteria = Color(0xFFC1A9D9)

val DarkBackground = Color(0xFF010440)
val DarkSurface = Color(0xFF1A0A4A)
val DarkCard = Color(0xFF2D1B69)

val LightBackground = Color(0xFFF5F5F5)
val LightSurface = Color(0xFFFFFFFF)

val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFC1A9D9)

val ChatTextDark = Color(0xFF1A1A1A)
val ChatTextMuted = Color(0xFF888888)
val ChatTimestamp = Color(0xFF999999)
val ChatOverlayDark = Color(0xE6000000)

data class AppColors(
    val background: Color,
    val surface: Color,
    val card: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val chatBackground: Color,
    val chatSurface: Color,
    val chatText: Color,
    val chatSubtitle: Color,
    val chatBubbleOther: Color,
    val chatBubbleOtherText: Color,
    val chatSystemBg: Color,
    val chatSystemText: Color,
    val isDark: Boolean
)

val DarkAppColors = AppColors(
    background = DarkBackground,
    surface = DarkSurface,
    card = DarkCard,
    textPrimary = TextPrimary,
    textSecondary = TextSecondary,
    accent = Wisteria,
    chatBackground = DarkBackground,
    chatSurface = DarkSurface,
    chatText = TextPrimary,
    chatSubtitle = TextSecondary,
    chatBubbleOther = DarkCard,
    chatBubbleOtherText = TextPrimary,
    chatSystemBg = DarkSurface,
    chatSystemText = TextSecondary,
    isDark = true
)

val LightAppColors = AppColors(
    background = Color(0xFFF0EBF8),
    surface = LightSurface,
    card = Color(0xFFE8DEF8),
    textPrimary = Color(0xFF1A1035),
    textSecondary = Color(0xFF6B5591),
    accent = Purple,
    chatBackground = LightBackground,
    chatSurface = LightSurface,
    chatText = ChatTextDark,
    chatSubtitle = ChatTextMuted,
    chatBubbleOther = LightSurface,
    chatBubbleOtherText = ChatTextDark,
    chatSystemBg = Color(0xFFEEEEEE),
    chatSystemText = ChatTimestamp,
    isDark = false
)

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }
