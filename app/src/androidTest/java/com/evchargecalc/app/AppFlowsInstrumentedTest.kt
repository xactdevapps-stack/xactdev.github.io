package com.evchargecalc.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.evchargecalc.app.model.ChargeSession
import com.evchargecalc.app.model.ChargerProfile
import com.evchargecalc.app.model.DistanceUnit
import com.evchargecalc.app.model.ThemeMode
import com.evchargecalc.app.model.VehicleProfile
import com.evchargecalc.app.ui.screens.CalculatorScreen
import com.evchargecalc.app.ui.screens.HistoryScreen
import com.evchargecalc.app.ui.screens.SettingsScreen
import com.evchargecalc.app.ui.screens.VehiclesScreen
import com.evchargecalc.app.ui.theme.EvTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppFlowsInstrumentedTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settingsCurrencySearchChangesSelection() {
        composeRule.setContent {
            var currencyCode by remember { mutableStateOf("GBP") }
            EvTheme(mode = ThemeMode.DARK) {
                SettingsScreen(
                    themeMode = ThemeMode.DARK,
                    distanceUnit = DistanceUnit.KM,
                    currencyCode = currencyCode,
                    onThemeModeChanged = {},
                    onDistanceUnitChanged = {},
                    onCurrencyCodeChanged = { currencyCode = it }
                )
            }
        }

        composeRule.onNodeWithText("Change Currency").performClick()
        composeRule.onNodeWithText("Search currencies").performTextInput("USD")
        composeRule.onNodeWithText("USD -", substring = true).performClick()
        composeRule.onNodeWithText("USD -", substring = true).assertIsDisplayed()
    }

    @Test
    fun vehicleValidationDisablesSaveUntilValid() {
        composeRule.setContent {
            var vehicles by remember { mutableStateOf(emptyList<VehicleProfile>()) }
            EvTheme(mode = ThemeMode.DARK) {
                VehiclesScreen(
                    vehicles = vehicles,
                    distanceUnit = DistanceUnit.KM,
                    selectedVehicleId = null,
                    onSelectVehicle = {},
                    onVehiclesChanged = { vehicles = it }
                )
            }
        }

        composeRule.onNodeWithTag("vehicle_save").assertIsNotEnabled()
        composeRule.onNodeWithTag("vehicle_model").performTextInput("Model 3")
        composeRule.onNodeWithTag("vehicle_battery").performTextInput("75")
        composeRule.onNodeWithTag("vehicle_range").performTextInput("500")
        composeRule.onNodeWithTag("vehicle_save").assertIsEnabled()
    }

    @Test
    fun savedChargeAppearsInHistory() {
        val vehicle = VehicleProfile(
            make = "Tesla",
            model = "Model 3",
            batteryCapacityKwh = 75.0,
            defaultTargetPercent = 80,
            estimatedRangeKm = 500.0,
            isDefault = true
        )
        val charger = ChargerProfile(
            name = "Home",
            location = "Garage",
            chargeRateKw = 7.4,
            pricePerKwh = 0.30,
            isDefault = true
        )

        composeRule.setContent {
            var sessions by remember { mutableStateOf(emptyList<ChargeSession>()) }
            EvTheme(mode = ThemeMode.DARK) {
                if (sessions.isEmpty()) {
                    CalculatorScreen(
                        vehicles = listOf(vehicle),
                        chargers = listOf(charger),
                        distanceUnit = DistanceUnit.KM,
                        currencyCode = "GBP",
                        selectedVehicleId = vehicle.id,
                        selectedChargerId = charger.id,
                        onSelectedVehicleChanged = {},
                        onSelectedChargerChanged = {},
                        onSaveCharge = { sessions = sessions + it }
                    )
                } else {
                    HistoryScreen(chargeSessions = sessions, currencyCode = "GBP")
                }
            }
        }

        composeRule.onNodeWithText("Save Charge Session").performClick()
        composeRule.onNodeWithText("Sessions: 1").assertIsDisplayed()
    }
}
