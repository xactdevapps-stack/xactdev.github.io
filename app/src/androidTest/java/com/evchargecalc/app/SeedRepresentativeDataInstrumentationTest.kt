package com.evchargecalc.app

import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.evchargecalc.app.model.ChargeSession
import com.evchargecalc.app.model.ChargerProfile
import com.evchargecalc.app.model.VehicleProfile
import com.evchargecalc.app.storage.AppStorage
import java.util.UUID
import kotlin.math.max
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SeedRepresentativeDataInstrumentationTest {

    @Test
    fun seedRepresentativeDatasetIntoAppStorage() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val storage = AppStorage(context)

        val vehicles = listOf(
            VehicleProfile(make = "Tesla", model = "Model 3", batteryCapacityKwh = 75.0, defaultTargetPercent = 80, estimatedRangeKm = 510.0, isDefault = true),
            VehicleProfile(make = "Nissan", model = "Leaf", batteryCapacityKwh = 40.0, defaultTargetPercent = 80, estimatedRangeKm = 260.0),
            VehicleProfile(make = "Hyundai", model = "Kona Electric", batteryCapacityKwh = 64.0, defaultTargetPercent = 80, estimatedRangeKm = 430.0),
            VehicleProfile(make = "Kia", model = "EV6", batteryCapacityKwh = 77.4, defaultTargetPercent = 80, estimatedRangeKm = 500.0),
            VehicleProfile(make = "BMW", model = "i4", batteryCapacityKwh = 83.9, defaultTargetPercent = 80, estimatedRangeKm = 560.0)
        )

        val chargers = listOf(
            ChargerProfile(name = "Home Charger", location = "Home", networkName = "", chargeRateKw = 7.4, pricePerKwh = 0.28, isDefault = true),
            ChargerProfile(name = "Workplace Bay", location = "Office", networkName = "", chargeRateKw = 11.0, pricePerKwh = 0.22),
            ChargerProfile(name = "City Rapid Hub", location = "City Center", networkName = "IONITY", chargeRateKw = 120.0, pricePerKwh = 0.69),
            ChargerProfile(name = "Supermarket Point", location = "Retail Park", networkName = "Pod Point", chargeRateKw = 22.0, pricePerKwh = 0.45)
        )

        val random = Random(20260503)
        val startMs = System.currentTimeMillis() - (365L * 2L * DAY_MS)
        val endMs = System.currentTimeMillis()
        val sessionTags = listOf("Home", "Work", "Public", "Rapid", "Other")

        val sessions = mutableListOf<ChargeSession>()
        vehicles.forEachIndexed { vehicleIndex, vehicle ->
            repeat(36) { monthIdx ->
                val sessionsThisMonth = 2 + random.nextInt(0, 3)
                repeat(sessionsThisMonth) { idx ->
                    val charger = chargers[(vehicleIndex + monthIdx + idx) % chargers.size]
                    val fromPct = 10 + random.nextInt(0, 50)
                    val toPct = max(fromPct + 10, fromPct + 20 + random.nextInt(0, 30)).coerceAtMost(95)
                    val delta = (toPct - fromPct) / 100.0

                    val energy = vehicle.batteryCapacityKwh * delta
                    val timestamp = random.nextLong(startMs, endMs)
                    val timeHours = energy / charger.chargeRateKw
                    val cost = energy * charger.pricePerKwh
                    val distanceKm = (energy / 0.18) * (0.75 + random.nextDouble() * 0.4)

                    sessions += ChargeSession(
                        id = UUID.randomUUID().toString(),
                        timestampMs = timestamp,
                        vehicleId = vehicle.id,
                        vehicleName = "${vehicle.make} ${vehicle.model}",
                        chargerId = charger.id,
                        chargerName = charger.name,
                        chargerLocation = charger.location,
                        chargerNetwork = charger.networkName,
                        energyKwh = energy,
                        timeHours = timeHours,
                        costAmount = cost,
                        currencyCode = "GBP",
                        sessionTag = sessionTags[(vehicleIndex + idx) % sessionTags.size],
                        notes = if (idx % 4 == 0) "Routine charge" else "",
                        distanceDrivenKm = distanceKm
                    )
                }
            }
        }

            storage.saveVehicles(vehicles)
            storage.saveChargers(chargers)
            storage.saveChargeSessions(sessions.sortedByDescending { it.timestampMs })

            Log.i("SEED_REPORT", "vehicles=${vehicles.size}")
            Log.i("SEED_REPORT", "chargers=${chargers.size}")
            Log.i("SEED_REPORT", "sessions=${sessions.size}")
        }
    }

    companion object {
        private const val DAY_MS = 24L * 60L * 60L * 1000L
    }
}
