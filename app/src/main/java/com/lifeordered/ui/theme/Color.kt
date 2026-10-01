package com.lifeordered.ui.theme

import androidx.compose.ui.graphics.Color

// Warm Cream & 3D Clay theme colors
val WarmCreamBg = Color(0xFFFFFBF7)
val WarmOrange = Color(0xFFFF7E40)
val DarkOrange = Color(0xFFE05C1E)
val LightCream = Color(0xFFFFF1E6)

// Cozy Soft Pastels for Claymorphic card backings from the Warm Organic theme
val PastelPeach = Color(0xFFFFF1E6)
val PastelBlue = Color(0xFFE0F2FE)
val PastelPink = Color(0xFFFFF0F3)
val PastelYellow = Color(0xFFFFFAED)
val PastelMint = Color(0xFFECEFF1)

// Status & Accent Text
val OrangePrimaryText = Color(0xFFFF7E40)
val BlueAccentText = Color(0xFF38BDF8)
val RedWarningText = Color(0xFFEF4444)
val GrayTextMuted = Color(0xFF8A7D73)
val TextDark = Color(0xFF2D2926)

// Standard Dark colors as adaptive fallback
val DarkThemeBg = Color(0xFF1D1B18)
val DarkThemeSurface = Color(0xFF2C241E)
val PrimaryDark = Color(0xFFFFAD80)

fun parseHexColor(hexString: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hexString))
    } catch (e: Exception) {
        Color(0xFFFF7E40) // Default peaches/peach orange
    }
}
