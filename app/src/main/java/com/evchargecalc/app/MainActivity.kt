package com.evchargecalc.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.evchargecalc.app.model.DEFAULT_VEHICLE_CHART_COLOR
import com.evchargecalc.app.storage.AppStorage
import com.evchargecalc.app.ui.screens.AppScreen
import com.evchargecalc.app.ui.theme.EvTheme
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val storage = remember { AppStorage(this) }
            val scope = rememberCoroutineScope()
            var vehicles by remember { mutableStateOf<List<com.evchargecalc.app.model.VehicleProfile>>(emptyList()) }
            var chargers by remember { mutableStateOf<List<com.evchargecalc.app.model.ChargerProfile>>(emptyList()) }
            var themeMode by remember { mutableStateOf(com.evchargecalc.app.model.ThemeMode.DARK) }
            var appAccentHex by remember { mutableStateOf(DEFAULT_VEHICLE_CHART_COLOR) }
            var distanceUnit by remember { mutableStateOf(com.evchargecalc.app.model.DistanceUnit.KM) }
            var currencyCode by remember { mutableStateOf("GBP") }
            var chargeSessions by remember { mutableStateOf<List<com.evchargecalc.app.model.ChargeSession>>(emptyList()) }

            LaunchedEffect(storage) {
                vehicles = storage.loadVehicles()
                chargers = storage.loadChargers()
                themeMode = storage.loadThemeMode()
                appAccentHex = storage.loadAppAccentHex()
                distanceUnit = storage.loadDistanceUnit()
                currencyCode = storage.loadCurrencyCode()
                chargeSessions = storage.loadChargeSessions()
            }

            EvTheme(mode = themeMode, accentHex = appAccentHex) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppScreen(
                        vehicles = vehicles,
                        chargers = chargers,
                        chargeSessions = chargeSessions,
                        themeMode = themeMode,
                        appAccentHex = appAccentHex,
                        distanceUnit = distanceUnit,
                        currencyCode = currencyCode,
                        onVehiclesChanged = {
                            vehicles = it
                            scope.launch { storage.saveVehicles(it) }
                        },
                        onChargersChanged = {
                            chargers = it
                            scope.launch { storage.saveChargers(it) }
                        },
                        onThemeModeChanged = {
                            themeMode = it
                            scope.launch { storage.saveThemeMode(it) }
                        },
                        onAppAccentHexChanged = {
                            appAccentHex = it
                            scope.launch { storage.saveAppAccentHex(it) }
                        },
                        onDistanceUnitChanged = {
                            distanceUnit = it
                            scope.launch { storage.saveDistanceUnit(it) }
                        },
                        onCurrencyCodeChanged = {
                            currencyCode = it
                            scope.launch { storage.saveCurrencyCode(it) }
                        },
                        onChargeSaved = { session ->
                            chargeSessions = (listOf(session) + chargeSessions).sortedByDescending { it.timestampMs }
                            scope.launch { storage.addChargeSession(session) }
                        },
                        onDuplicateSession = { session ->
                            chargeSessions = (listOf(session) + chargeSessions).sortedByDescending { it.timestampMs }
                            scope.launch { storage.addChargeSession(session) }
                        },
                        onImportSessions = { imported ->
                            fun parseVehicleName(name: String): Pair<String, String> {
                                val parts = name.trim().split(" ").filter { it.isNotBlank() }
                                return when {
                                    parts.isEmpty() -> "Imported" to "Vehicle"
                                    parts.size == 1 -> parts.first() to "EV"
                                    else -> parts.first() to parts.drop(1).joinToString(" ")
                                }
                            }

                            val importedByVehicleId = imported.groupBy { it.vehicleId }
                            val existingVehicleIds = vehicles.map { it.id }.toSet()
                            val newVehicles = importedByVehicleId
                                .filterKeys { it !in existingVehicleIds }
                                .map { (vehicleId, sessionsForVehicle) ->
                                    val displayName = sessionsForVehicle.first().vehicleName
                                    val (make, model) = parseVehicleName(displayName)

                                    val maxEnergy = sessionsForVehicle.maxOfOrNull { it.energyKwh } ?: 40.0
                                    val inferredBattery = (maxEnergy * 1.25).coerceIn(35.0, 120.0)

                                    val totalDistance = sessionsForVehicle.mapNotNull { it.distanceDrivenKm }.sum()
                                    val totalEnergy = sessionsForVehicle.sumOf { it.energyKwh }
                                    val kmPerKwh = if (totalDistance > 0.0 && totalEnergy > 0.0) {
                                        totalDistance / totalEnergy
                                    } else {
                                        5.5
                                    }
                                    val inferredRange = (inferredBattery * kmPerKwh).coerceIn(180.0, 700.0)

                                    com.evchargecalc.app.model.VehicleProfile(
                                        id = vehicleId,
                                        make = make,
                                        model = model,
                                        batteryCapacityKwh = ((inferredBattery * 10.0).roundToInt() / 10.0),
                                        defaultTargetPercent = 80,
                                        estimatedRangeKm = inferredRange,
                                        chartColorHex = DEFAULT_VEHICLE_CHART_COLOR,
                                        isDefault = false
                                    )
                                }

                            val importedByChargerId = imported.groupBy { it.chargerId }
                            val existingChargerIds = chargers.map { it.id }.toSet()
                            val newChargers = importedByChargerId
                                .filterKeys { it !in existingChargerIds }
                                .map { (chargerId, sessionsForCharger) ->
                                    val first = sessionsForCharger.first()
                                    val validRateSamples = sessionsForCharger
                                        .filter { it.timeHours > 0.0 }
                                        .map { it.energyKwh / it.timeHours }
                                    val inferredRate = if (validRateSamples.isNotEmpty()) {
                                        validRateSamples.average().coerceIn(3.0, 350.0)
                                    } else {
                                        7.4
                                    }

                                    val validPriceSamples = sessionsForCharger
                                        .filter { it.energyKwh > 0.0 }
                                        .map { it.costAmount / it.energyKwh }
                                    val inferredPrice = if (validPriceSamples.isNotEmpty()) {
                                        validPriceSamples.average().coerceIn(0.0, 2.5)
                                    } else {
                                        0.30
                                    }

                                    val inferredNetwork = sessionsForCharger
                                        .map { it.chargerNetwork.trim() }
                                        .filter { it.isNotBlank() }
                                        .groupingBy { it }
                                        .eachCount()
                                        .maxByOrNull { it.value }
                                        ?.key
                                        .orEmpty()

                                    com.evchargecalc.app.model.ChargerProfile(
                                        id = chargerId,
                                        name = first.chargerName,
                                        location = first.chargerLocation,
                                        networkName = inferredNetwork,
                                        chargeRateKw = ((inferredRate * 10.0).roundToInt() / 10.0),
                                        pricePerKwh = ((inferredPrice * 100.0).roundToInt() / 100.0),
                                        latitude = null,
                                        longitude = null,
                                        isDefault = false
                                    )
                                }

                            if (newVehicles.isNotEmpty()) {
                                vehicles = vehicles + newVehicles
                                scope.launch { storage.saveVehicles(vehicles) }
                            }

                            if (newChargers.isNotEmpty()) {
                                chargers = chargers + newChargers
                                scope.launch { storage.saveChargers(chargers) }
                            }

                            val merged = (chargeSessions + imported)
                                .associateBy { it.id }
                                .values
                                .sortedByDescending { it.timestampMs }
                            chargeSessions = merged
                            scope.launch { storage.saveChargeSessions(merged) }
                        },
                        onUpdateSession = { updated ->
                            chargeSessions = chargeSessions
                                .map { existing -> if (existing.id == updated.id) updated else existing }
                                .sortedByDescending { it.timestampMs }
                            scope.launch { storage.upsertChargeSession(updated) }
                        },
                        onDeleteSession = { deleted ->
                            chargeSessions = chargeSessions.filterNot { it.id == deleted.id }
                            scope.launch { storage.deleteChargeSession(deleted.id) }
                        },
                        onClearAllSessions = {
                            chargeSessions = emptyList()
                            scope.launch { storage.saveChargeSessions(emptyList()) }
                        }
                    )
                }
            }
        }
    }
}
