package com.evchargecalc.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import com.evchargecalc.app.model.DistanceUnit
import com.evchargecalc.app.model.VehicleProfile
import com.evchargecalc.app.model.kmToSelected
import com.evchargecalc.app.model.knownManufacturers
import com.evchargecalc.app.model.selectedToKm
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
    val rangeUnitLabel = if (distanceUnit == DistanceUnit.MI) "mi" else "km"

    fun resetForm() {
        editingId = null
        selectedMake = "Tesla"
        customMake = ""
        model = ""
        battery = ""
        target = "80"
        range = ""
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
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Model") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                NumberField(
                    label = "Battery Capacity (kWh)",
                    value = battery,
                    onValueChange = { battery = it }
                )
                NumberField(
                    label = "Default Charge Target (%)",
                    value = target,
                    onValueChange = { target = it }
                )
                NumberField(
                    label = "Estimated Full Range ($rangeUnitLabel)",
                    value = range,
                    onValueChange = { range = it }
                )
                Spacer(Modifier.height(8.dp))
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
                                    estimatedRangeKm = rangeKmValue
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
                                            estimatedRangeKm = rangeKmValue
                                        )
                                    } else {
                                        it
                                    }
                                }
                            )
                        }
                        resetForm()
                    }) {
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

        items(vehicles, key = { it.id }) { vehicle ->
            TechCard(title = "${vehicle.make} ${vehicle.model}") {
                Text("Battery: ${format1(vehicle.batteryCapacityKwh)} kWh")
                Text("Default Target: ${vehicle.defaultTargetPercent}%")
                Text("Range @100%: ${format1(vehicle.estimatedRangeKm.kmToSelected(distanceUnit))} $rangeUnitLabel")
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
                    }) {
                        Text("Edit")
                    }
                    Button(
                        onClick = {
                            onVehiclesChanged(vehicles.filterNot { it.id == vehicle.id })
                            if (selectedVehicleId == vehicle.id) {
                                onSelectVehicle(null)
                            }
                        },
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
