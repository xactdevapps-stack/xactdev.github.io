package com.evchargecalc.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.evchargecalc.app.model.parseHexColor
import com.evchargecalc.app.model.ThemeMode

private val matrixDarkBase = darkColorScheme(
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

private val matrixLightBase = lightColorScheme(
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

private fun adjustBrightness(color: Color, factor: Float): Color {
    val r = (color.red * factor).coerceIn(0f, 1f)
    val g = (color.green * factor).coerceIn(0f, 1f)
    val b = (color.blue * factor).coerceIn(0f, 1f)
    return Color(r, g, b, color.alpha)
}

private fun themedDarkScheme(accent: Color) = darkColorScheme(
    primary = accent,
    onPrimary = if (accent.luminance() > 0.45f) Color(0xFF091104) else Color.White,
    secondary = lerp(accent, Color.White, 0.20f),
    onSecondary = Color(0xFF06150D),
    background = lerp(matrixDarkBase.background, accent, 0.08f),
    onBackground = lerp(matrixDarkBase.onBackground, accent, 0.18f),
    surface = lerp(matrixDarkBase.surface, accent, 0.12f),
    onSurface = lerp(matrixDarkBase.onSurface, accent, 0.12f),
    outline = lerp(matrixDarkBase.outline, accent, 0.35f)
)

private fun themedLightScheme(accent: Color): ColorScheme {
    val primary = adjustBrightness(accent, 0.55f)
    val secondary = adjustBrightness(accent, 0.68f)
    return lightColorScheme(
        primary = primary,
        onPrimary = if (primary.luminance() > 0.5f) Color(0xFF0D2011) else Color.White,
        secondary = secondary,
        onSecondary = if (secondary.luminance() > 0.5f) Color(0xFF0D2011) else Color.White,
        background = lerp(matrixLightBase.background, accent, 0.08f),
        onBackground = lerp(matrixLightBase.onBackground, accent, 0.10f),
        surface = lerp(matrixLightBase.surface, accent, 0.10f),
        onSurface = lerp(matrixLightBase.onSurface, accent, 0.10f),
        outline = lerp(matrixLightBase.outline, accent, 0.25f)
    )
}

@Composable
fun EvTheme(mode: ThemeMode, accentHex: String, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val accent = parseHexColor(accentHex)

    MaterialTheme(
        colorScheme = if (dark) themedDarkScheme(accent) else themedLightScheme(accent),
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
