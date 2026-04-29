package com.evchargecalc.app.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.evchargecalc.app.model.allCurrencyOptions
import com.evchargecalc.app.model.DistanceUnit
import com.evchargecalc.app.model.ThemeMode
import androidx.compose.material3.ExperimentalMaterial3Api
import com.evchargecalc.app.ui.components.SelectionDropdown
import com.evchargecalc.app.ui.components.TechCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    distanceUnit: DistanceUnit,
    currencyCode: String,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onDistanceUnitChanged: (DistanceUnit) -> Unit,
    onCurrencyCodeChanged: (String) -> Unit
) {
    val currencies = remember { allCurrencyOptions() }

    TechCard(title = "Theme") {
        Text("Default is DARK. Select a persistent app theme mode:")
        Spacer(Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ThemeMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                    selected = themeMode == mode,
                    onClick = { onThemeModeChanged(mode) },
                    icon = {}
                ) {
                    Text(mode.name)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Minimalist high-tech style with matrix contrast, rounded geometry, and clear mono typography.",
            style = MaterialTheme.typography.bodySmall
        )
    }


    Spacer(Modifier.height(12.dp))

    TechCard(title = "Currency") {
        SelectionDropdown(
            title = "Currency",
            selectedText = currencies.firstOrNull { it.code == currencyCode }?.label ?: currencyCode,
            options = currencies.map { it.code to it.label },
            onSelect = { selected -> if (selected != null) onCurrencyCodeChanged(selected) },
            includeNoneOption = false
        )
    }
    Spacer(Modifier.height(12.dp))

    TechCard(title = "Distance Unit") {
        Text("Choose the unit used for all range values and calculations:")
        Spacer(Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            DistanceUnit.entries.forEachIndexed { index, unit ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index, DistanceUnit.entries.size),
                    selected = distanceUnit == unit,
                    onClick = { onDistanceUnitChanged(unit) },
                    icon = {}
                ) {
                    Text(unit.name)
                }
            }
        }
    }
}
