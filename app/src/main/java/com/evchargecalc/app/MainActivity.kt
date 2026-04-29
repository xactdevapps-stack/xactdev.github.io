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
import com.evchargecalc.app.storage.AppStorage
import com.evchargecalc.app.ui.screens.AppScreen
import com.evchargecalc.app.ui.theme.EvTheme
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
            var distanceUnit by remember { mutableStateOf(com.evchargecalc.app.model.DistanceUnit.KM) }
            var currencyCode by remember { mutableStateOf("GBP") }
            var chargeSessions by remember { mutableStateOf<List<com.evchargecalc.app.model.ChargeSession>>(emptyList()) }

            LaunchedEffect(storage) {
                vehicles = storage.loadVehicles()
                chargers = storage.loadChargers()
                themeMode = storage.loadThemeMode()
                distanceUnit = storage.loadDistanceUnit()
                currencyCode = storage.loadCurrencyCode()
                chargeSessions = storage.loadChargeSessions()
            }

            EvTheme(mode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppScreen(
                        vehicles = vehicles,
                        chargers = chargers,
                        chargeSessions = chargeSessions,
                        themeMode = themeMode,
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
                            // Pre-select the vehicle and charger for quick re-calculation
                            val vehicle = vehicles.firstOrNull { it.id == session.vehicleId }
                            val charger = chargers.firstOrNull { it.id == session.chargerId }
                            if (vehicle != null && charger != null) {
                                // The UI will select these for the calculator
                            }
                        },
                        onChargeSessionsChanged = {
                            chargeSessions = it
                            scope.launch { storage.saveChargeSessions(it) }
                        }
                    )
                }
            }
        }
    }
}
