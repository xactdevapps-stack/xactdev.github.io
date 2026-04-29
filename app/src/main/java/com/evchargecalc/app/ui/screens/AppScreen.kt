package com.evchargecalc.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evchargecalc.app.model.ChargeSession
import com.evchargecalc.app.model.AppTab
import com.evchargecalc.app.model.ChargerProfile
import com.evchargecalc.app.model.DistanceUnit
import com.evchargecalc.app.model.ThemeMode
import com.evchargecalc.app.model.VehicleProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen(
    vehicles: List<VehicleProfile>,
    chargers: List<ChargerProfile>,
    chargeSessions: List<ChargeSession>,
    themeMode: ThemeMode,
    distanceUnit: DistanceUnit,
    currencyCode: String,
    onVehiclesChanged: (List<VehicleProfile>) -> Unit,
    onChargersChanged: (List<ChargerProfile>) -> Unit,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onDistanceUnitChanged: (DistanceUnit) -> Unit,
    onCurrencyCodeChanged: (String) -> Unit,
    onChargeSaved: (ChargeSession) -> Unit
) {
    var tab by remember { mutableStateOf(AppTab.CALCULATE) }
    var selectedVehicleId by remember { mutableStateOf(vehicles.firstOrNull { it.isDefault }?.id) }
    var selectedChargerId by remember { mutableStateOf(chargers.firstOrNull { it.isDefault }?.id) }

    LaunchedEffect(vehicles) {
        if (selectedVehicleId != null && vehicles.none { it.id == selectedVehicleId }) {
            selectedVehicleId = vehicles.firstOrNull { it.isDefault }?.id
        }
    }

    LaunchedEffect(chargers) {
        if (selectedChargerId != null && chargers.none { it.id == selectedChargerId }) {
            selectedChargerId = chargers.firstOrNull { it.isDefault }?.id
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("EV Charge Cost Calculator", letterSpacing = 1.4.sp) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                AppTab.entries.forEachIndexed { index, entry ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index, AppTab.entries.size),
                        onClick = { tab = entry },
                        selected = tab == entry,
                        icon = {}
                    ) {
                        Text(
                            text = when (entry) {
                                AppTab.CALCULATE -> "CALC"
                                AppTab.VEHICLES -> "VEHICLES"
                                AppTab.CHARGERS -> "CHARGERS"
                                AppTab.HISTORY -> "HISTORY"
                                AppTab.SETTINGS -> "SETTINGS"
                            }
                        )
                    }
                }
            }

            when (tab) {
                AppTab.CALCULATE -> CalculatorScreen(
                    vehicles = vehicles,
                    chargers = chargers,
                    distanceUnit = distanceUnit,
                    currencyCode = currencyCode,
                    selectedVehicleId = selectedVehicleId,
                    selectedChargerId = selectedChargerId,
                    onSelectedVehicleChanged = { selectedVehicleId = it },
                    onSelectedChargerChanged = { selectedChargerId = it },
                    onSaveCharge = onChargeSaved
                )

                AppTab.VEHICLES -> VehiclesScreen(
                    vehicles = vehicles,
                    distanceUnit = distanceUnit,
                    selectedVehicleId = selectedVehicleId,
                    onSelectVehicle = { selectedVehicleId = it },
                    onVehiclesChanged = onVehiclesChanged
                )

                AppTab.CHARGERS -> ChargersScreen(
                    chargers = chargers,
                    currencyCode = currencyCode,
                    selectedChargerId = selectedChargerId,
                    onSelectCharger = { selectedChargerId = it },
                    onChargersChanged = onChargersChanged
                )

                AppTab.HISTORY -> HistoryScreen(
                    chargeSessions = chargeSessions,
                    currencyCode = currencyCode
                )

                AppTab.SETTINGS -> SettingsScreen(
                    themeMode = themeMode,
                    distanceUnit = distanceUnit,
                    onThemeModeChanged = onThemeModeChanged,
                    currencyCode = currencyCode,
                    onDistanceUnitChanged = onDistanceUnitChanged,
                    onCurrencyCodeChanged = onCurrencyCodeChanged
                )
            }
        }
    }
}
