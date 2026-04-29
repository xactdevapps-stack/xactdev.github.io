package com.evchargecalc.app.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.evchargecalc.app.model.allCurrencyOptions
import com.evchargecalc.app.model.DistanceUnit
import com.evchargecalc.app.model.ThemeMode
import androidx.compose.material3.ExperimentalMaterial3Api
import com.evchargecalc.app.ui.components.SearchableSelectionDialog
import com.evchargecalc.app.ui.components.SelectionField
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
    val currencyOptions = remember(currencies) { currencies.map { it.code to it.label } }
    var showAbout by remember { mutableStateOf(false) }
    var showCurrencyPicker by remember { mutableStateOf(false) }
    val segmentedColors = SegmentedButtonDefaults.colors(
        activeContainerColor = MaterialTheme.colorScheme.primary,
        activeContentColor = MaterialTheme.colorScheme.onPrimary
    )

    LazyColumn {
        item {
            TechCard(title = "Theme") {
                Text("Default is DARK. Select a persistent app theme mode:")
                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                            selected = themeMode == mode,
                            onClick = { onThemeModeChanged(mode) },
                            colors = segmentedColors,
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
        }

        item {
            Spacer(Modifier.height(12.dp))
            TechCard(title = "Currency") {
                SelectionField(
                    title = "Currency",
                    selectedText = currencies.firstOrNull { it.code == currencyCode }?.label ?: currencyCode,
                    onClick = { showCurrencyPicker = true }
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = { showCurrencyPicker = true }) {
                    Text("Change Currency")
                }
            }
        }

        item {
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
                            colors = segmentedColors,
                            icon = {}
                        ) {
                            Text(unit.name)
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(12.dp))
            TechCard(title = "About") {
                Text("View app details, version info, and support links.")
                Spacer(Modifier.height(8.dp))
                Button(onClick = { showAbout = !showAbout }) {
                    Text(if (showAbout) "Hide About" else "Open About")
                }
            }
        }

        if (showAbout) {
            item {
                Spacer(Modifier.height(12.dp))
                AboutScreen()
            }
        }
    }

    if (showCurrencyPicker) {
        SearchableSelectionDialog(
            title = "Select Currency",
            options = currencyOptions,
            onDismiss = { showCurrencyPicker = false },
            onSelect = {
                onCurrencyCodeChanged(it)
                showCurrencyPicker = false
            }
        )
    }
}
