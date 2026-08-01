package com.novaradar.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.novaradar.app.ui.viewmodel.AppTheme

// Dark Mode: Deep navy with vibrant accents
val DarkBg = Color(0xFF05080F)
val DarkMeshColors = listOf(Color(0xFF05080F), Color(0xFF0D1B2A), Color(0xFF030712))
val DarkSurface = Color(0xFF111827)
val DarkSurfaceVariant = Color(0xFF1E293B)
val DarkPrimary = Color(0xFF60B5FF)
val DarkSecondary = Color(0xFFA78BFA)
val DarkTertiary = Color(0xFF34D399)
val DarkOnBg = Color(0xFFF1F5F9)
val DarkOnSurface = Color(0xFFFFFFFF)
val DarkOutline = Color(0xFF1E3A5F)

// Light Mode: Clean white with strong contrast
val LightBg = Color(0xFFF0F4F8)
val LightMeshColors = listOf(Color(0xFFFFFFFF), Color(0xFFEFF6FF), Color(0xFFF8FAFC))
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE2E8F0)
val LightPrimary = Color(0xFF1D4ED8)
val LightSecondary = Color(0xFF7C3AED)
val LightTertiary = Color(0xFF059669)
val LightOnBg = Color(0xFF0F172A)
val LightOnSurface = Color(0xFF0F172A)
val LightOutline = Color(0xFF94A3B8)

val SuccessGreen = Color(0xFF34D399)
val WarningAmber = Color(0xFFFBBF24)
val ErrorRed = Color(0xFFEF4444)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color(0xFF0A0E1A),
    secondary = DarkSecondary,
    onSecondary = Color.White,
    tertiary = DarkTertiary,
    background = DarkBg,
    onBackground = DarkOnBg,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    outline = DarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    secondary = LightSecondary,
    onSecondary = Color.White,
    tertiary = LightTertiary,
    background = LightBg,
    onBackground = LightOnBg,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    outline = LightOutline
)

@Composable
fun NovaRadarTheme(
    theme: AppTheme = AppTheme.PRISM_DARK,
    content: @Composable () -> Unit
) {
    val colorScheme = when (theme) {
        AppTheme.PRISM_DARK -> DarkColorScheme
        AppTheme.PRISM_LIGHT -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
