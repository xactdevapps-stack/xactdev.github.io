package com.evchargecalc.app

import com.evchargecalc.app.model.DistanceUnit
import com.evchargecalc.app.model.kmToSelected
import com.evchargecalc.app.model.selectedToKm
import org.junit.Assert.assertEquals
import org.junit.Test

class ChargeMathTest {
    @Test
    fun computesChargeHoursFromPowerAndBatterySize() {
        val batteryKwh = 60.0
        val chargerKw = 7.4
        val hours = batteryKwh / chargerKw

        assertEquals(8.108, hours, 0.001)
    }

    @Test
    fun convertsKilometersToMiles() {
        assertEquals(62.137, 100.0.kmToSelected(DistanceUnit.MI), 0.001)
    }

    @Test
    fun convertsMilesToKilometers() {
        assertEquals(160.934, 100.0.selectedToKm(DistanceUnit.MI), 0.001)
    }

    @Test
    fun computesEnergyForChargeWindow() {
        val batteryKwh = 75.0
        val fromPercent = 20.0
        val toPercent = 80.0

        val energy = batteryKwh * ((toPercent - fromPercent) / 100.0)

        assertEquals(45.0, energy, 0.001)
    }
}
