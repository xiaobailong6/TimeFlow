package com.lifeordered.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    background = DarkThemeBg,
    surface = DarkThemeSurface,
    onPrimary = Color.Black,
    onBackground = Color(0xFFEFE6DF),
    onSurface = Color(0xFFEFE6DF)
)

private val LightColorScheme = lightColorScheme(
    primary = WarmOrange,
    background = WarmCreamBg,
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = TextDark,
    onSurface = TextDark,
    secondary = PastelPeach,
    tertiary = PastelBlue,
    inverseSurface = LightCream
)

@Composable
fun LifeOrderedTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
