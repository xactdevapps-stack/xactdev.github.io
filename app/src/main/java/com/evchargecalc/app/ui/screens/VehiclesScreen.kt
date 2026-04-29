package com.evchargecalc.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.evchargecalc.app.model.DEFAULT_VEHICLE_CHART_COLOR
import com.evchargecalc.app.model.DistanceUnit
import com.evchargecalc.app.model.VehicleProfile
import com.evchargecalc.app.model.VehicleChartColorOption
import com.evchargecalc.app.model.kmToSelected
import com.evchargecalc.app.model.knownManufacturers
import com.evchargecalc.app.model.parseHexColor
import com.evchargecalc.app.model.selectedToKm
import com.evchargecalc.app.model.vehicleChartColorOptions
import com.evchargecalc.app.ui.components.ConfirmationDialog
import com.evchargecalc.app.ui.components.EmptyStateCard
import com.evchargecalc.app.ui.components.NumberField
import com.evchargecalc.app.ui.components.SelectionDropdown
import com.evchargecalc.app.ui.components.TechCard
import com.evchargecalc.app.ui.components.format1

@Composable
fun VehiclesScreen(
    vehicles: List<VehicleProfile>,
    distanceUnit: DistanceUnit,
    selectedVehicleId: String?,
    onSelectVehicle: (String?) -> Unit,
    onVehiclesChanged: (List<VehicleProfile>) -> Unit
) {
    var editingId by remember { mutableStateOf<String?>(null) }
    var selectedMake by remember { mutableStateOf("Tesla") }
    var customMake by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var battery by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("80") }
    var range by remember { mutableStateOf("") }
    var selectedChartColorHex by remember { mutableStateOf(DEFAULT_VEHICLE_CHART_COLOR) }
    var deleteConfirmVehicleId by remember { mutableStateOf<String?>(null) }

    val rangeUnitLabel = if (distanceUnit == DistanceUnit.MI) "mi" else "km"
    val validationMessage = when {
        (if (selectedMake == "Other") customMake.trim() else selectedMake.trim()).isBlank() -> "Select a manufacturer or enter a custom make."
        model.isBlank() -> "Enter a model name."
        battery.toDoubleOrNull() == null || battery.toDoubleOrNull()!! <= 0.0 -> "Enter a battery capacity greater than zero."
        target.toIntOrNull() == null || target.toIntOrNull() !in 1..100 -> "Default charge target must be between 1 and 100."
        range.toDoubleOrNull() == null || range.toDoubleOrNull()!! <= 0.0 -> "Enter a full-range estimate greater than zero."
        else -> null
    }

    fun colorLabel(hex: String): String {
        return vehicleChartColorOptions.firstOrNull { it.hex == hex }?.label ?: hex
    }

    fun resetForm() {
        editingId = null
        selectedMake = "Tesla"
        customMake = ""
        model = ""
        battery = ""
        target = "80"
        range = ""
        selectedChartColorHex = DEFAULT_VEHICLE_CHART_COLOR
    }

    if (deleteConfirmVehicleId != null) {
        val vehicleToDelete = vehicles.firstOrNull { it.id == deleteConfirmVehicleId }
        vehicleToDelete?.let {
            ConfirmationDialog(
                title = "Delete Vehicle?",
                message = "Are you sure you want to delete '${it.make} ${it.model}'? This action cannot be undone.",
                confirmText = "Delete",
                onConfirm = {
                    onVehiclesChanged(vehicles.filterNot { v -> v.id == deleteConfirmVehicleId })
                    if (selectedVehicleId == deleteConfirmVehicleId) {
                        onSelectVehicle(null)
                    }
                    deleteConfirmVehicleId = null
                },
                onDismiss = { deleteConfirmVehicleId = null }
            )
        }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            TechCard(title = if (editingId == null) "Add Vehicle" else "Edit Vehicle") {
                SelectionDropdown(
                    title = "Make",
                    selectedText = selectedMake,
                    options = knownManufacturers.map { it to it },
                    onSelect = { selected -> if (selected != null) selectedMake = selected },
                    includeNoneOption = false
                )
                if (selectedMake == "Other") {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customMake,
                        onValueChange = { customMake = it },
                        label = { Text("Custom Make") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("vehicle_custom_make")
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Model") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vehicle_model"),
                    placeholder = { Text("e.g., Model 3, i4, Leaf") }
                )
                Spacer(Modifier.height(8.dp))
                NumberField(
                    label = "Battery Capacity (kWh)",
                    value = battery,
                    onValueChange = { battery = it },
                    example = "75",
                    modifier = Modifier.testTag("vehicle_battery")
                )
                NumberField(
                    label = "Default Charge Target (%)",
                    value = target,
                    onValueChange = { target = it },
                    example = "80",
                    modifier = Modifier.testTag("vehicle_target")
                )
                NumberField(
                    label = "Estimated Full Range ($rangeUnitLabel)",
                    value = range,
                    onValueChange = { range = it },
                    example = if (distanceUnit == DistanceUnit.MI) "250" else "400",
                    modifier = Modifier.testTag("vehicle_range")
                )
                Spacer(Modifier.height(8.dp))
                SelectionDropdown(
                    title = "History Color",
                    selectedText = colorLabel(selectedChartColorHex),
                    options = vehicleChartColorOptions.map { option -> option.hex to option.label },
                    onSelect = { selected -> if (selected != null) selectedChartColorHex = selected },
                    includeNoneOption = false
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(parseHexColor(selectedChartColorHex), CircleShape)
                    )
                    Text(
                        text = "Used for history charts and session cards",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (validationMessage != null) {
                    Text(
                        validationMessage,
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val makeValue = if (selectedMake == "Other") customMake.trim() else selectedMake.trim()
                        val batteryValue = battery.toDoubleOrNull() ?: return@Button
                        val targetValue = target.toIntOrNull()?.coerceIn(1, 100) ?: return@Button
                        val rangeKmValue =
                            range.toDoubleOrNull()?.selectedToKm(distanceUnit) ?: return@Button
                        if (makeValue.isBlank() || model.isBlank()) return@Button

                        if (editingId == null) {
                            onVehiclesChanged(
                                vehicles + VehicleProfile(
                                    make = makeValue,
                                    model = model.trim(),
                                    batteryCapacityKwh = batteryValue,
                                    defaultTargetPercent = targetValue,
                                    estimatedRangeKm = rangeKmValue,
                                    chartColorHex = selectedChartColorHex
                                )
                            )
                        } else {
                            onVehiclesChanged(
                                vehicles.map {
                                    if (it.id == editingId) {
                                        it.copy(
                                            make = makeValue,
                                            model = model.trim(),
                                            batteryCapacityKwh = batteryValue,
                                            defaultTargetPercent = targetValue,
                                            estimatedRangeKm = rangeKmValue,
                                            chartColorHex = selectedChartColorHex
                                        )
                                    } else {
                                        it
                                    }
                                }
                            )
                        }
                        resetForm()
                    }, enabled = validationMessage == null, modifier = Modifier.testTag("vehicle_save")) {
                        Text(if (editingId == null) "Save Vehicle" else "Update Vehicle")
                    }
                    if (editingId != null) {
                        Button(
                            onClick = { resetForm() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }

        if (vehicles.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No Vehicles",
                    message = "Create a vehicle profile to save battery specs and estimated range. You can add multiple vehicles for quick calculator reference and track different charge profiles."
                )
            }
        } else {
            items(vehicles, key = { it.id }) { vehicle ->
                val vehicleColor = parseHexColor(vehicle.chartColorHex)
                TechCard(
                    title = "${vehicle.make} ${vehicle.model}",
                    titleColor = vehicleColor,
                    accentColor = vehicleColor
                ) {
                    Text("Battery: ${format1(vehicle.batteryCapacityKwh)} kWh")
                    Text("Default Target: ${vehicle.defaultTargetPercent}%")
                    Text("Range @100%: ${format1(vehicle.estimatedRangeKm.kmToSelected(distanceUnit))} $rangeUnitLabel")
                    Text("History Color: ${colorLabel(vehicle.chartColorHex)}")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(onClick = { onSelectVehicle(vehicle.id) }) {
                            Text(if (selectedVehicleId == vehicle.id) "Selected" else "Use")
                        }
                        Button(onClick = {
                            onVehiclesChanged(vehicles.map { it.copy(isDefault = it.id == vehicle.id) })
                            onSelectVehicle(vehicle.id)
                        }) {
                            Text(if (vehicle.isDefault) "Default" else "Set Default")
                        }
                        Button(onClick = {
                            editingId = vehicle.id
                            if (knownManufacturers.contains(vehicle.make)) {
                                selectedMake = vehicle.make
                                customMake = ""
                            } else {
                                selectedMake = "Other"
                                customMake = vehicle.make
                            }
                            model = vehicle.model
                            battery = vehicle.batteryCapacityKwh.toString()
                            target = vehicle.defaultTargetPercent.toString()
                            range = vehicle.estimatedRangeKm.kmToSelected(distanceUnit).toString()
                            selectedChartColorHex = vehicle.chartColorHex
                        }) {
                            Text("Edit")
                        }
                        Button(
                            onClick = { deleteConfirmVehicleId = vehicle.id },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}

