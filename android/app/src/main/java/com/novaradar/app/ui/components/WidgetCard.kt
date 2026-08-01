package com.novaradar.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Wc {
    val primary = Color(0xFF0D7DB3)
    val primaryGlow = Color(0xFF0D7DB3).copy(alpha = 0.15f)
    val success = Color(0xFF10B981)
    val successLight = Color(0xFF34D399)
    val warning = Color(0xFFF59E0B)
    val error = Color(0xFFEF4444)
    val info = Color(0xFF3B82F6)
    val surfaceDark = Color(0xFF0A0E1A)
    val surfaceVariantDark = Color(0xFF151B2D)
    val surfaceContainerDark = Color(0xFF1E2740)
    val onSurfaceDark = Color(0xFFFFFFFF)
    val onSurfaceVariantDark = Color(0xFFE2E8F0)
    val lightText = Color(0xFF0F172A)
    val lightTextSecondary = Color(0xFF334155)
    val lightBg = Color(0xFFF5F7FA)
    val lightCard = Color.White
    val lightBorder = Color(0xFFCBD5E1)
}

@Composable
fun WidgetCard(
    modifier: Modifier = Modifier,
    isLightTheme: Boolean,
    borderColor: Color = Wc.primary.copy(alpha = 0.18f),
    glassIntensity: Float = 0.7f,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLightTheme) Color.White.copy(alpha = glassIntensity)
            else Color(0xFF161D2A).copy(alpha = glassIntensity.coerceIn(0.5f, 0.95f))
        ),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            content = content
        )
    }
}

@Composable
fun StatWidget(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: Color,
    accentColor: Color,
    isLightTheme: Boolean
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLightTheme) Color.White.copy(alpha = 0.6f)
            else Color(0xFF1C2333).copy(alpha = 0.7f)
        ),
        border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                ),
                color = valueColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                ),
                color = if (isLightTheme) Color(0xFF4A5568).copy(alpha = 0.7f)
                else Color(0xFF8B95A8).copy(alpha = 0.7f)
            )
        }
    }
}
