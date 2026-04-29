package com.evchargecalc.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.evchargecalc.app.model.ChargerProfile
import com.evchargecalc.app.model.MapsUtils
import com.evchargecalc.app.model.formatCurrencyAmount
import com.evchargecalc.app.ui.components.ConfirmationDialog
import com.evchargecalc.app.ui.components.EmptyStateCard
import com.evchargecalc.app.ui.components.LocationPickerDialog
import com.evchargecalc.app.ui.components.NumberField
import com.evchargecalc.app.ui.components.TechCard
import com.evchargecalc.app.ui.components.format2

@Composable
fun ChargersScreen(
    chargers: List<ChargerProfile>,
    currencyCode: String,
    selectedChargerId: String?,
    onSelectCharger: (String?) -> Unit,
    onChargersChanged: (List<ChargerProfile>) -> Unit
) {
    val context = LocalContext.current
    var editingId by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var networkName by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var rate by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var showLocationPicker by remember { mutableStateOf(false) }
    var deleteConfirmChargerId by remember { mutableStateOf<String?>(null) }

    val validationMessage = when {
        name.isBlank() -> "Enter a charger name."
        location.isBlank() -> "Enter a charger location."
        rate.toDoubleOrNull() == null || rate.toDoubleOrNull()!! <= 0.0 -> "Enter a charge rate greater than zero."
        price.toDoubleOrNull() == null || price.toDoubleOrNull()!! < 0.0 -> "Enter a valid price per kWh."
        else -> null
    }

    fun resetForm() {
        editingId = null
        name = ""
        location = ""
        networkName = ""
        latitude = null
        longitude = null
        rate = ""
        price = ""
    }

    if (showLocationPicker) {
        LocationPickerDialog(
            currentLocation = location,
            currentLat = latitude,
            currentLng = longitude,
            onLocationSelected = { loc, lat, lng ->
                location = loc
                latitude = lat
                longitude = lng
                showLocationPicker = false
            },
            onDismiss = { showLocationPicker = false }
        )
    }

    if (deleteConfirmChargerId != null) {
        val chargerToDelete = chargers.firstOrNull { it.id == deleteConfirmChargerId }
        chargerToDelete?.let {
            ConfirmationDialog(
                title = "Delete Charger?",
                message = "Are you sure you want to delete '${it.name}'? This action cannot be undone.",
                confirmText = "Delete",
                onConfirm = {
                    onChargersChanged(chargers.filterNot { c -> c.id == deleteConfirmChargerId })
                    if (selectedChargerId == deleteConfirmChargerId) {
                        onSelectCharger(null)
                    }
                    deleteConfirmChargerId = null
                },
                onDismiss = { deleteConfirmChargerId = null }
            )
        }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            TechCard(title = if (editingId == null) "Add Charger" else "Edit Charger") {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g., Home, Work, Mall") }
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location") },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("e.g., Main St") }
                    )
                    Button(onClick = { showLocationPicker = true }) {
                        Text("Pick")
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = networkName,
                    onValueChange = { networkName = it },
                    label = { Text("Network (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g., Pod Point, Tesla") }
                )
                Spacer(Modifier.height(8.dp))
                NumberField(
                    label = "Charge Rate (kW)",
                    value = rate,
                    onValueChange = { rate = it },
                    example = "7.4"
                )
                NumberField(
                    label = "Price per kWh ($currencyCode)",
                    value = price,
                    onValueChange = { price = it },
                    example = "0.34"
                )
                Spacer(Modifier.height(8.dp))
                if (latitude != null && longitude != null) {
                    Text(
                        "GPS: ${"%.4f".format(latitude)}, ${"%.4f".format(longitude)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.height(8.dp))
                }
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
                        val rateValue = rate.toDoubleOrNull() ?: return@Button
                        val priceValue = price.toDoubleOrNull() ?: return@Button
                        if (name.isBlank() || location.isBlank()) return@Button

                        if (editingId == null) {
                            onChargersChanged(
                                chargers + ChargerProfile(
                                    name = name.trim(),
                                    location = location.trim(),
                                    networkName = networkName.trim(),
                                    chargeRateKw = rateValue,
                                    pricePerKwh = priceValue,
                                    latitude = latitude,
                                    longitude = longitude
                                )
                            )
                        } else {
                            onChargersChanged(
                                chargers.map {
                                    if (it.id == editingId) {
                                        it.copy(
                                            name = name.trim(),
                                            location = location.trim(),
                                            networkName = networkName.trim(),
                                            chargeRateKw = rateValue,
                                            pricePerKwh = priceValue,
                                            latitude = latitude,
                                            longitude = longitude
                                        )
                                    } else {
                                        it
                                    }
                                }
                            )
                        }
                        resetForm()
                    }, enabled = validationMessage == null) {
                        Text(if (editingId == null) "Save Charger" else "Update Charger")
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

        if (chargers.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No Chargers",
                    message = "Create a charger profile to save charging locations and rates. You can add multiple chargers (home, work, public stations, etc.) for quick calculator reference."
                )
            }
        } else {
            items(chargers, key = { it.id }) { charger ->
                TechCard(title = charger.name) {
                    Text("Location: ${charger.location}")
                    if (charger.networkName.isNotBlank()) {
                        Text("Network: ${charger.networkName}")
                    }
                    if (charger.latitude != null && charger.longitude != null) {
                        Text(
                            "GPS: ${"%.4f".format(charger.latitude)}, ${"%.4f".format(charger.longitude)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Text("Rate: ${format2(charger.chargeRateKw)} kW")
                    Text("Price: ${formatCurrencyAmount(charger.pricePerKwh, currencyCode)} / kWh")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(onClick = { onSelectCharger(charger.id) }) {
                            Text(if (selectedChargerId == charger.id) "Selected" else "Use")
                        }
                        Button(onClick = {
                            onChargersChanged(chargers.map { it.copy(isDefault = it.id == charger.id) })
                            onSelectCharger(charger.id)
                        }) {
                            Text(if (charger.isDefault) "Default" else "Set Default")
                        }
                        if (charger.latitude != null && charger.longitude != null) {
                            Button(onClick = {
                                MapsUtils.openDirectionsInMaps(
                                    context,
                                    charger.name,
                                    charger.latitude,
                                    charger.longitude,
                                    charger.location
                                )
                            }) {
                                Text("Maps")
                            }
                        }
                        Button(onClick = {
                            editingId = charger.id
                            name = charger.name
                            location = charger.location
                            networkName = charger.networkName
                            latitude = charger.latitude
                            longitude = charger.longitude
                            rate = charger.chargeRateKw.toString()
                            price = charger.pricePerKwh.toString()
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit charger"
                            )
                        }
                        Button(
                            onClick = { deleteConfirmChargerId = charger.id },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete charger"
                            )
                        }
                    }
                }
            }
        }
    }
}

