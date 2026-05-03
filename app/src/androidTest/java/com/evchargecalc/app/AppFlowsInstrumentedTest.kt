package com.evchargecalc.app

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.evchargecalc.app.model.ChargeSession
import com.evchargecalc.app.model.ChargerProfile
import com.evchargecalc.app.model.DEFAULT_VEHICLE_CHART_COLOR
import com.evchargecalc.app.model.DistanceUnit
import com.evchargecalc.app.model.ThemeMode
import com.evchargecalc.app.model.VehicleProfile
import com.evchargecalc.app.ui.screens.CalculatorScreen
import com.evchargecalc.app.ui.screens.HistoryScreen
import com.evchargecalc.app.ui.screens.SettingsScreen
import com.evchargecalc.app.ui.screens.VehiclesScreen
import com.evchargecalc.app.ui.theme.EvTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppFlowsInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun ensureDeviceAwakeAndComposeHostResumed() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val device = UiDevice.getInstance(instrumentation)

        if (!device.isScreenOn) {
            device.wakeUp()
        }
        device.executeShellCommand("wm dismiss-keyguard")

        @Suppress("DEPRECATION")
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.waitForIdle()
    }

    @Test
    fun settingsCurrencySearchChangesSelection() {
        composeRule.setContent {
            var currencyCode by remember { mutableStateOf("GBP") }
            EvTheme(mode = ThemeMode.DARK, accentHex = DEFAULT_VEHICLE_CHART_COLOR) {
                SettingsScreen(
                    themeMode = ThemeMode.DARK,
                    appAccentHex = DEFAULT_VEHICLE_CHART_COLOR,
                    distanceUnit = DistanceUnit.KM,
                    currencyCode = currencyCode,
                    onThemeModeChanged = {},
                    onAppAccentHexChanged = {},
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
            EvTheme(mode = ThemeMode.DARK, accentHex = DEFAULT_VEHICLE_CHART_COLOR) {
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
            EvTheme(mode = ThemeMode.DARK, accentHex = DEFAULT_VEHICLE_CHART_COLOR) {
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
                    HistoryScreen(
                        vehicles = listOf(vehicle),
                        chargers = listOf(charger),
                        chargeSessions = sessions,
                        distanceUnit = DistanceUnit.KM,
                        currencyCode = "GBP"
                    )
                }
            }
        }

        composeRule.onNodeWithTag("calculator_list").performScrollToNode(hasText("Save Charge Session"))
        composeRule.onNodeWithText("Save Charge Session").performClick()
        composeRule.onNodeWithText("Save").performClick()
        composeRule.onNodeWithText("Sessions: 1").assertIsDisplayed()
    }
}
