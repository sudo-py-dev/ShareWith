package com.share.with

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val DarkColorScheme =
    darkColorScheme(
        // Indigo 400
        primary = Color(0xFF818CF8),
        onPrimary = Color(0xFF0F172A),
        // Indigo 900
        primaryContainer = Color(0xFF312E81),
        onPrimaryContainer = Color(0xFFE0E7FF),
        // Violet 400
        secondary = Color(0xFFA78BFA),
        onSecondary = Color(0xFF0F172A),
        // Extra dark slate
        background = Color(0xFF0B0F19),
        onBackground = Color(0xFFF8FAFC),
        // Slightly lighter surface
        surface = Color(0xFF151B2C),
        onSurface = Color(0xFFF1F5F9),
        // Slate 800
        surfaceVariant = Color(0xFF1E293B),
        onSurfaceVariant = Color(0xFFCBD5E1),
        outline = Color(0xFF475569),
    )

private val LightColorScheme =
    lightColorScheme(
        // Indigo 600
        primary = Color(0xFF4F46E5),
        onPrimary = Color(0xFFFFFFFF),
        // Indigo 100
        primaryContainer = Color(0xFFE0E7FF),
        onPrimaryContainer = Color(0xFF312E81),
        // Violet 600
        secondary = Color(0xFF7C3AED),
        onSecondary = Color(0xFFFFFFFF),
        // Slate 50
        background = Color(0xFFF8FAFC),
        onBackground = Color(0xFF0F172A),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF1E293B),
        // Slate 200
        surfaceVariant = Color(0xFFE2E8F0),
        onSurfaceVariant = Color(0xFF475569),
        outline = Color(0xFF94A3B8),
    )

private val AppTypography =
    Typography(
        headlineLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                lineHeight = 40.sp,
                letterSpacing = (-0.5).sp,
            ),
        titleLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                letterSpacing = 0.sp,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.5.sp,
            ),
        labelMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp,
            ),
    )

@Composable
fun ShareWithTheme(
    themeMode: String = AppState.selectedTheme,
    content: @Composable () -> Unit,
) {
    val darkTheme =
        when (themeMode) {
            "Dark" -> true
            "Light" -> false
            else -> isSystemInDarkTheme()
        }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}
