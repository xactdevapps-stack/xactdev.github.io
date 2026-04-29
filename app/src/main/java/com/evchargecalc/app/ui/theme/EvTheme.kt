package com.evchargecalc.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.evchargecalc.app.model.ThemeMode

private val matrixDarkScheme = darkColorScheme(
    primary = Color(0xFF9FFF5E),
    onPrimary = Color(0xFF091104),
    secondary = Color(0xFF4BEA89),
    onSecondary = Color(0xFF06150D),
    background = Color(0xFF090F0A),
    onBackground = Color(0xFFC8FFD4),
    surface = Color(0xFF101A12),
    onSurface = Color(0xFFD7FFE0),
    outline = Color(0xFF3C5A43)
)

private val matrixLightScheme = lightColorScheme(
    primary = Color(0xFF245A2E),
    onPrimary = Color.White,
    secondary = Color(0xFF147D45),
    onSecondary = Color.White,
    background = Color(0xFFE8F6E7),
    onBackground = Color(0xFF0D2011),
    surface = Color(0xFFF5FFF3),
    onSurface = Color(0xFF102514),
    outline = Color(0xFF507B56)
)

@Composable
fun EvTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    MaterialTheme(
        colorScheme = if (dark) matrixDarkScheme else matrixLightScheme,
        typography = MaterialTheme.typography.copy(
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
            bodyMedium = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            titleLarge = MaterialTheme.typography.titleLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            ),
            titleMedium = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )
        ),
        shapes = MaterialTheme.shapes.copy(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(20.dp)
        ),
        content = content
    )
}
