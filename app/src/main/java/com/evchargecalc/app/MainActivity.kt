package com.evchargecalc.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.evchargecalc.app.storage.AppStorage
import com.evchargecalc.app.ui.screens.AppScreen
import com.evchargecalc.app.ui.theme.EvTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val storage = remember { AppStorage(this) }
            var vehicles by remember { mutableStateOf(storage.loadVehicles()) }
            var chargers by remember { mutableStateOf(storage.loadChargers()) }
            var themeMode by remember { mutableStateOf(storage.loadThemeMode()) }
            var distanceUnit by remember { mutableStateOf(storage.loadDistanceUnit()) }
            var currencyCode by remember { mutableStateOf(storage.loadCurrencyCode()) }
            var chargeSessions by remember { mutableStateOf(storage.loadChargeSessions()) }

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
                            storage.saveVehicles(it)
                        },
                        onChargersChanged = {
                            chargers = it
                            storage.saveChargers(it)
                        },
                        onThemeModeChanged = {
                            themeMode = it
                            storage.saveThemeMode(it)
                        },
                        onDistanceUnitChanged = {
                            distanceUnit = it
                            storage.saveDistanceUnit(it)
                        },
                        onCurrencyCodeChanged = {
                            currencyCode = it
                            storage.saveCurrencyCode(it)
                        },
                        onChargeSaved = { session ->
                            chargeSessions = (listOf(session) + chargeSessions).sortedByDescending { it.timestampMs }
                            storage.saveChargeSessions(chargeSessions)
                        }
                    )
                }
            }
        }
    }
}
