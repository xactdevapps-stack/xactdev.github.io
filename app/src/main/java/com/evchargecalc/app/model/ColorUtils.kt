package com.evchargecalc.app.model

import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color

fun parseHexColor(hex: String, fallbackHex: String = DEFAULT_VEHICLE_CHART_COLOR): Color {
    return runCatching { Color(AndroidColor.parseColor(hex)) }
        .getOrDefault(Color(AndroidColor.parseColor(fallbackHex)))
}
