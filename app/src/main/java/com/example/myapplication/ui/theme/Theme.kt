package com.example.myapplication.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryAccent,
    onPrimary = Color.White,
    primaryContainer = PrimaryAccent,
    onPrimaryContainer = Color.White,
    secondary = SecondaryAccent,
    onSecondary = Color.White,
    secondaryContainer = SecondaryAccent,
    onSecondaryContainer = Color.White,
    tertiary = SecondaryAccent,
    onTertiary = Color.White,
    background = PrimaryBackground,
    onBackground = Color.White,
    surface = SecondarySurface,
    onSurface = Color.White,
    surfaceVariant = SecondarySurface,
    onSurfaceVariant = TextSecondary,
    outline = SecondaryAccent,
    inversePrimary = PrimaryAccent,
    inverseSurface = SecondarySurface,
    inverseOnSurface = Color.White,
    error = Color.Red, // Default error, can be adjusted
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryAccent,
    onPrimary = Color.White,
    primaryContainer = PrimaryAccent,
    onPrimaryContainer = Color.White,
    secondary = SecondaryAccent,
    onSecondary = Color.White,
    secondaryContainer = SecondaryAccent,
    onSecondaryContainer = Color.White,
    tertiary = SecondaryAccent,
    onTertiary = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color.DarkGray,
    outline = Color.LightGray
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}