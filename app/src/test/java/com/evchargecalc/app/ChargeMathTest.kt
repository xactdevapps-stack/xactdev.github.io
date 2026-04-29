package com.evchargecalc.app

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
}
