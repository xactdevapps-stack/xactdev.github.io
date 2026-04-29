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
import com.evchargecalc.app.model.ChargerProfile
import com.evchargecalc.app.model.formatCurrencyAmount
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
    var editingId by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }

    fun resetForm() {
        editingId = null
        name = ""
        location = ""
        rate = ""
        price = ""
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            TechCard(title = if (editingId == null) "Add Charger" else "Edit Charger") {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                NumberField(
                    label = "Charge Rate (kW)",
                    value = rate,
                    onValueChange = { rate = it }
                )
                NumberField(
                    label = "Price per kWh ($currencyCode)",
                    value = price,
                    onValueChange = { price = it }
                )
                Spacer(Modifier.height(8.dp))
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
                                    chargeRateKw = rateValue,
                                    pricePerKwh = priceValue
                                )
                            )
                        } else {
                            onChargersChanged(
                                chargers.map {
                                    if (it.id == editingId) {
                                        it.copy(
                                            name = name.trim(),
                                            location = location.trim(),
                                            chargeRateKw = rateValue,
                                            pricePerKwh = priceValue
                                        )
                                    } else {
                                        it
                                    }
                                }
                            )
                        }
                        resetForm()
                    }) {
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

        items(chargers, key = { it.id }) { charger ->
            TechCard(title = charger.name) {
                Text("Location: ${charger.location}")
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
                    Button(onClick = {
                        editingId = charger.id
                        name = charger.name
                        location = charger.location
                        rate = charger.chargeRateKw.toString()
                        price = charger.pricePerKwh.toString()
                    }) {
                        Text("Edit")
                    }
                    Button(
                        onClick = {
                            onChargersChanged(chargers.filterNot { it.id == charger.id })
                            if (selectedChargerId == charger.id) {
                                onSelectCharger(null)
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
