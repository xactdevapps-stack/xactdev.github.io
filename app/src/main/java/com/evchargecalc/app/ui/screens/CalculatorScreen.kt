package com.evchargecalc.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.evchargecalc.app.model.chargeSessionTagOptions
import com.evchargecalc.app.model.formatCurrencyAmount
import com.evchargecalc.app.model.kmToSelected
import com.evchargecalc.app.model.selectedToKm
import com.evchargecalc.app.ui.components.ConfirmationDialog
import com.evchargecalc.app.ui.components.NumberField
import com.evchargecalc.app.ui.components.ProfileDropdown
import com.evchargecalc.app.ui.components.SelectionDropdown
import com.evchargecalc.app.ui.components.TechCard
import com.evchargecalc.app.ui.components.format1
import com.evchargecalc.app.ui.components.format2

private enum class ChargeInputMode { PERCENT, TARGET_RANGE }

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
    var adhocRange by remember { mutableStateOf(if (distanceUnit == DistanceUnit.MI) "260" else "420") }
    var adhocRate by remember { mutableStateOf("7.4") }
    var adhocPrice by remember { mutableStateOf("0.34") }

    var chargeRange by remember { mutableStateOf(20f..80f) }
    var chargeInputMode by remember { mutableStateOf(ChargeInputMode.PERCENT) }
    var targetRangeInput by remember { mutableStateOf("") }

    var efficiencyEnabled by remember { mutableStateOf(false) }
    var efficiencyPercent by remember { mutableStateOf("90") }

    var sessionTag by remember { mutableStateOf(chargeSessionTagOptions.first()) }
    var notes by remember { mutableStateOf("") }
    var distanceDrivenInput by remember { mutableStateOf("") }
    val segmentedColors = SegmentedButtonDefaults.colors(
        activeContainerColor = MaterialTheme.colorScheme.primary,
        activeContentColor = MaterialTheme.colorScheme.onPrimary
    )

    val rangeUnitLabel = if (distanceUnit == DistanceUnit.MI) "mi" else "km"

    LaunchedEffect(selectedVehicle?.id, useSavedVehicle) {
        if (useSavedVehicle && selectedVehicle != null) {
            val target = selectedVehicle.defaultTargetPercent.coerceIn(1, 100).toFloat()
            val from = chargeRange.start.coerceIn(0f, target)
            chargeRange = from..target
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

    val batteryEnergyNeeded = batteryKwh?.let { it * (deltaPercent / 100.0) }
    val efficiencyRatio = efficiencyPercent.toDoubleOrNull()?.coerceIn(1.0, 100.0)?.div(100.0)
    val energyNeeded = if (batteryEnergyNeeded != null) {
        if (efficiencyEnabled && efficiencyRatio != null) batteryEnergyNeeded / efficiencyRatio else batteryEnergyNeeded
    } else {
        null
    }

    val chargeCost = if (energyNeeded != null && pricePerKwh != null) energyNeeded * pricePerKwh else null
    val hoursNeeded = if (energyNeeded != null && rateKw != null && rateKw > 0) energyNeeded / rateKw else null
    val newRange = estimatedRangeKm?.let { it * (toPercent / 100.0) }
    val addedRange = estimatedRangeKm?.let { it * (deltaPercent / 100.0) }
    val displayedNewRange = newRange?.kmToSelected(distanceUnit)
    val displayedAddedRange = addedRange?.kmToSelected(distanceUnit)

    val distanceDrivenKm = distanceDrivenInput.toDoubleOrNull()?.takeIf { it > 0.0 }?.selectedToKm(distanceUnit)
    val energyEfficiencyKwhPer100Km = if (batteryEnergyNeeded != null && distanceDrivenKm != null && distanceDrivenKm > 0.0) {
        (batteryEnergyNeeded / distanceDrivenKm) * 100.0
    } else {
        null
    }
    val distanceEfficiency = if (batteryEnergyNeeded != null && batteryEnergyNeeded > 0.0 && distanceDrivenKm != null) {
        val selectedDistance = distanceDrivenKm.kmToSelected(distanceUnit)
        selectedDistance / batteryEnergyNeeded
    } else {
        null
    }

    fun syncTargetRangeFromPercent() {
        val fullRangeKm = estimatedRangeKm ?: return
        val targetKm = fullRangeKm * (chargeRange.endInclusive.toDouble() / 100.0)
        targetRangeInput = format1(targetKm.kmToSelected(distanceUnit))
    }

    LaunchedEffect(estimatedRangeKm, distanceUnit) {
        if (estimatedRangeKm != null) {
            syncTargetRangeFromPercent()
        } else {
            targetRangeInput = ""
        }
    }

    val canSaveCharge =
        useSavedVehicle && useSavedCharger && selectedVehicle != null && selectedCharger != null &&
            energyNeeded != null && hoursNeeded != null && chargeCost != null

    var showSaveConfirm by remember { mutableStateOf(false) }

    val pendingSession = if (canSaveCharge) {
        ChargeSession(
            vehicleId = selectedVehicle!!.id,
            vehicleName = "${selectedVehicle.make} ${selectedVehicle.model}",
            chargerId = selectedCharger!!.id,
            chargerName = selectedCharger.name,
            chargerLocation = selectedCharger.location,
            chargerNetwork = selectedCharger.networkName,
            energyKwh = energyNeeded!!,
            timeHours = hoursNeeded!!,
            costAmount = chargeCost!!,
            currencyCode = currencyCode,
            sessionTag = sessionTag,
            notes = notes.trim(),
            distanceDrivenKm = distanceDrivenKm
        )
    } else {
        null
    }

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
                        onValueChange = { adhocBattery = it },
                        example = "60"
                    )
                    NumberField(
                        label = "Estimated Full Range ($rangeUnitLabel)",
                        value = adhocRange,
                        onValueChange = { adhocRange = it },
                        example = if (distanceUnit == DistanceUnit.MI) "260" else "420"
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
                        onValueChange = { adhocRate = it },
                        example = "7.4"
                    )
                    NumberField(
                        label = "Price per kWh",
                        value = adhocPrice,
                        onValueChange = { adhocPrice = it },
                        example = "0.34"
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

                Spacer(Modifier.height(8.dp))
                Text("Input Mode")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ChargeInputMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index, ChargeInputMode.entries.size),
                            selected = chargeInputMode == mode,
                            onClick = { chargeInputMode = mode },
                            colors = segmentedColors,
                            icon = {}
                        ) {
                            Text(if (mode == ChargeInputMode.PERCENT) "%" else "Target Range")
                        }
                    }
                }

                RangeSlider(
                    value = chargeRange,
                    onValueChange = { range ->
                        chargeRange = range.start.coerceIn(0f, 100f)..
                            range.endInclusive.coerceIn(0f, 100f)
                        if (estimatedRangeKm != null) {
                            syncTargetRangeFromPercent()
                        }
                    },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline
                    )
                )

                if (chargeInputMode == ChargeInputMode.TARGET_RANGE) {
                    if (estimatedRangeKm == null || estimatedRangeKm <= 0.0) {
                        Text(
                            "Target range input needs an estimated full range.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    } else {
                        NumberField(
                            label = "Target Range ($rangeUnitLabel)",
                            value = targetRangeInput,
                            onValueChange = {
                                targetRangeInput = it
                                val targetSelected = it.toDoubleOrNull() ?: return@NumberField
                                val targetKm = targetSelected.selectedToKm(distanceUnit)
                                val targetPercent = ((targetKm / estimatedRangeKm) * 100.0)
                                    .coerceIn(chargeRange.start.toDouble(), 100.0)
                                chargeRange = chargeRange.start..targetPercent.toFloat()
                            },
                            example = if (distanceUnit == DistanceUnit.MI) "180" else "300"
                        )
                        Text(
                            "Changing target range updates the % slider and vice versa.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        item {
            TechCard(title = "Session Details") {
                SelectionDropdown(
                    title = "Charge Type",
                    selectedText = sessionTag,
                    options = chargeSessionTagOptions.map { it to it },
                    onSelect = { selected -> if (selected != null) sessionTag = selected },
                    includeNoneOption = false
                )
                Spacer(Modifier.height(8.dp))
                NumberField(
                    label = "Distance Since Last Charge ($rangeUnitLabel, optional)",
                    value = distanceDrivenInput,
                    onValueChange = { distanceDrivenInput = it },
                    example = if (distanceUnit == DistanceUnit.MI) "45" else "70"
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }
        }

        item {
            TechCard(title = "Result") {
                Text("Energy Needed: ${energyNeeded?.let { "${format2(it)} kWh" } ?: "Provide battery capacity"}")
                if (efficiencyEnabled && batteryEnergyNeeded != null && efficiencyRatio != null) {
                    Text(
                        "Battery Energy (without losses): ${format2(batteryEnergyNeeded)} kWh",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
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
                if (energyEfficiencyKwhPer100Km != null) {
                    Text(
                        "Driving Efficiency: ${format2(energyEfficiencyKwhPer100Km)} kWh/100km",
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                if (distanceEfficiency != null) {
                    Text(
                        "Distance per kWh: ${format2(distanceEfficiency)} $rangeUnitLabel/kWh",
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply charging efficiency", modifier = Modifier.weight(1f))
                    Switch(checked = efficiencyEnabled, onCheckedChange = { efficiencyEnabled = it })
                }
                if (efficiencyEnabled) {
                    NumberField(
                        label = "Efficiency (%)",
                        value = efficiencyPercent,
                        onValueChange = { efficiencyPercent = it },
                        example = "90"
                    )
                    Text(
                        "Optional: if empty/invalid, standard calculation is used.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        showSaveConfirm = true
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

    if (showSaveConfirm && pendingSession != null) {
        ConfirmationDialog(
            title = "Save charge session?",
            message = "Save this charge result for ${pendingSession.vehicleName} at ${pendingSession.chargerName}?",
            confirmText = "Save",
            dismissText = "Cancel",
            isDestructive = false,
            onConfirm = {
                onSaveCharge(pendingSession)
                showSaveConfirm = false
            },
            onDismiss = { showSaveConfirm = false }
        )
    }
}
