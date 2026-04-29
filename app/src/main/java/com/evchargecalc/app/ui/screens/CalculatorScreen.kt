package com.evchargecalc.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.evchargecalc.app.model.ChargeSession
import com.evchargecalc.app.model.ChargerProfile
import com.evchargecalc.app.model.DistanceUnit
import com.evchargecalc.app.model.VehicleProfile
import com.evchargecalc.app.model.formatCurrencyAmount
import com.evchargecalc.app.model.kmToSelected
import com.evchargecalc.app.model.selectedToKm
import com.evchargecalc.app.ui.components.NumberField
import com.evchargecalc.app.ui.components.ProfileDropdown
import com.evchargecalc.app.ui.components.TechCard
import androidx.compose.material3.ExperimentalMaterial3Api
import com.evchargecalc.app.ui.components.format1
import com.evchargecalc.app.ui.components.format2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    vehicles: List<VehicleProfile>,
    chargers: List<ChargerProfile>,
    distanceUnit: DistanceUnit,
    currencyCode: String,
    selectedVehicleId: String?,
    selectedChargerId: String?,
    onSelectedVehicleChanged: (String?) -> Unit,
    onSelectedChargerChanged: (String?) -> Unit,
    onSaveCharge: (ChargeSession) -> Unit
) {
    val selectedVehicle = vehicles.firstOrNull { it.id == selectedVehicleId }
    val selectedCharger = chargers.firstOrNull { it.id == selectedChargerId }

    var useSavedVehicle by remember { mutableStateOf(selectedVehicle != null) }
    var useSavedCharger by remember { mutableStateOf(selectedCharger != null) }

    var adhocBattery by remember { mutableStateOf("60") }
    var adhocRange by remember { mutableStateOf("420") }
    var adhocRate by remember { mutableStateOf("7.4") }
    var adhocPrice by remember { mutableStateOf("0.34") }

    var chargeRange by remember { mutableStateOf(20f..80f) }
    val rangeUnitLabel = if (distanceUnit == DistanceUnit.MI) "mi" else "km"

    LaunchedEffect(selectedVehicleId) {
        if (selectedVehicle != null) {
            val target = selectedVehicle.defaultTargetPercent.coerceIn(1, 100)
            val lower = (target - 30).coerceAtLeast(0)
            chargeRange = lower.toFloat()..target.toFloat()
        }
    }

    val batteryKwh = if (useSavedVehicle) selectedVehicle?.batteryCapacityKwh else adhocBattery.toDoubleOrNull()
    val estimatedRangeKm = if (useSavedVehicle) {
        selectedVehicle?.estimatedRangeKm
    } else {
        adhocRange.toDoubleOrNull()?.selectedToKm(distanceUnit)
    }
    val rateKw = if (useSavedCharger) selectedCharger?.chargeRateKw else adhocRate.toDoubleOrNull()
    val pricePerKwh = if (useSavedCharger) selectedCharger?.pricePerKwh else adhocPrice.toDoubleOrNull()

    val fromPercent = chargeRange.start.toDouble()
    val toPercent = chargeRange.endInclusive.toDouble()
    val deltaPercent = (toPercent - fromPercent).coerceAtLeast(0.0)
    val energyNeeded = batteryKwh?.let { it * (deltaPercent / 100.0) }
    val chargeCost = if (energyNeeded != null && pricePerKwh != null) energyNeeded * pricePerKwh else null
    val hoursNeeded = if (energyNeeded != null && rateKw != null && rateKw > 0) energyNeeded / rateKw else null
    val newRange = estimatedRangeKm?.let { it * (toPercent / 100.0) }
    val addedRange = estimatedRangeKm?.let { it * (deltaPercent / 100.0) }
    val displayedNewRange = newRange?.kmToSelected(distanceUnit)
    val displayedAddedRange = addedRange?.kmToSelected(distanceUnit)
    val canSaveCharge =
        useSavedVehicle && useSavedCharger && selectedVehicle != null && selectedCharger != null &&
            energyNeeded != null && hoursNeeded != null && chargeCost != null

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            TechCard(title = "Saved Vehicle") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Use Saved Vehicle", modifier = Modifier.weight(1f))
                    Switch(checked = useSavedVehicle, onCheckedChange = { useSavedVehicle = it })
                }
                if (useSavedVehicle) {
                    ProfileDropdown(
                        title = "Vehicle",
                        selectedText = selectedVehicle?.let { "${it.make} ${it.model}" } ?: "Select vehicle",
                        options = vehicles.map { it.id to "${it.make} ${it.model}" },
                        onSelect = { onSelectedVehicleChanged(it) }
                    )
                } else {
                    NumberField(
                        label = "Battery Capacity (kWh)",
                        value = adhocBattery,
                        onValueChange = { adhocBattery = it }
                    )
                    NumberField(
                        label = "Estimated Full Range ($rangeUnitLabel)",
                        value = adhocRange,
                        onValueChange = { adhocRange = it }
                    )
                }
            }
        }

        item {
            TechCard(title = "Saved Charger") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Use Saved Charger", modifier = Modifier.weight(1f))
                    Switch(checked = useSavedCharger, onCheckedChange = { useSavedCharger = it })
                }
                if (useSavedCharger) {
                    ProfileDropdown(
                        title = "Charger",
                        selectedText = selectedCharger?.let { "${it.name} (${it.location})" }
                            ?: "Select charger",
                        options = chargers.map { it.id to "${it.name} (${it.location})" },
                        onSelect = { onSelectedChargerChanged(it) }
                    )
                } else {
                    NumberField(
                        label = "Charge Rate (kW)",
                        value = adhocRate,
                        onValueChange = { adhocRate = it }
                    )
                    NumberField(
                        label = "Price per kWh",
                        value = adhocPrice,
                        onValueChange = { adhocPrice = it }
                    )
                }
            }
        }

        item {
            TechCard(title = "Charge Window") {
                val battery = batteryKwh ?: 0.0
                val fromKwh = battery * (fromPercent / 100.0)
                val toKwh = battery * (toPercent / 100.0)

                Text(
                    text = "Current ${fromPercent.toInt()}% (${format2(fromKwh)} kWh) -> " +
                        "Target ${toPercent.toInt()}% (${format2(toKwh)} kWh)",
                    style = MaterialTheme.typography.bodyMedium
                )

                RangeSlider(
                    value = chargeRange,
                    onValueChange = { range ->
                        chargeRange = range.start.coerceIn(0f, 100f)..
                            range.endInclusive.coerceIn(0f, 100f)
                    },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline
                    )
                )
            }
        }

        item {
            TechCard(title = "Result") {
                Text("Energy Needed: ${energyNeeded?.let { "${format2(it)} kWh" } ?: "Provide battery capacity"}")
                Text(
                    "Estimated Cost: ${chargeCost?.let { formatCurrencyAmount(it, currencyCode) } ?: "Provide price per kWh"}"
                )
                Text("Estimated Time: ${hoursNeeded?.let { "${format2(it)} hours" } ?: "Provide charge rate"}")
                Text(
                    "Expected New Range: ${displayedNewRange?.let { "${format1(it)} $rangeUnitLabel" } ?: "Select vehicle or enter range"}",
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    "Added Range: ${displayedAddedRange?.let { "+${format1(it)} $rangeUnitLabel" } ?: "-"}",
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        onSaveCharge(
                            ChargeSession(
                                vehicleId = selectedVehicle!!.id,
                                vehicleName = "${selectedVehicle.make} ${selectedVehicle.model}",
                                chargerId = selectedCharger!!.id,
                                chargerName = selectedCharger.name,
                                chargerLocation = selectedCharger.location,
                                energyKwh = energyNeeded!!,
                                timeHours = hoursNeeded!!,
                                costAmount = chargeCost!!,
                                currencyCode = currencyCode
                            )
                        )
                    },
                    enabled = canSaveCharge
                ) {
                    Text("Save Charge Session")
                }
                if (!canSaveCharge) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Select saved vehicle and charger to save this result.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
