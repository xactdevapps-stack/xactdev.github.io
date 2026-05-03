package com.evchargecalc.app

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.evchargecalc.app.model.ChargeSession
import com.evchargecalc.app.model.ChargerProfile
import com.evchargecalc.app.model.VehicleProfile
import com.evchargecalc.app.storage.AppDatabase
import kotlin.math.max
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppStressInstrumentationTest {
    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .fallbackToDestructiveMigration()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun stressTestLargeMultiYearDatasetAndCoreFlows() = runBlocking {
        val random = Random(42)
        val vehicles = (1..10).map { idx ->
            VehicleProfile(
                make = "Make$idx",
                model = "Model$idx",
                batteryCapacityKwh = 45.0 + idx,
                defaultTargetPercent = 80,
                estimatedRangeKm = 280.0 + idx * 12,
                isDefault = idx == 1
            )
        }

        val chargers = (1..12).map { idx ->
            ChargerProfile(
                name = "Charger $idx",
                location = "Location $idx",
                networkName = if (idx % 2 == 0) "PublicNet" else "HomeNet",
                chargeRateKw = 7.2 + idx,
                pricePerKwh = 0.20 + (idx * 0.01),
                isDefault = idx == 1
            )
        }

        val startMs = System.currentTimeMillis() - FIVE_YEARS_MS
        val endMs = System.currentTimeMillis()

        val generated = mutableListOf<ChargeSession>()
        val generationStart = System.nanoTime()
        vehicles.forEachIndexed { vehicleIndex, vehicle ->
            repeat(1000) { i ->
                val charger = chargers[(i + vehicleIndex) % chargers.size]
                val from = random.nextInt(10, 70)
                val to = random.nextInt(max(from + 5, 20), 96)
                val energy = vehicle.batteryCapacityKwh * ((to - from) / 100.0)
                val timestamp = random.nextLong(startMs, endMs)
                val timeHours = energy / charger.chargeRateKw
                val cost = energy * charger.pricePerKwh

                generated += ChargeSession(
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
                    sessionTag = SESSION_TAGS[(i + vehicleIndex) % SESSION_TAGS.size],
                    notes = if (i % 10 == 0) "night session" else "",
                    distanceDrivenKm = random.nextDouble(8.0, 200.0)
                )
            }
            report("generated vehicle ${vehicleIndex + 1}/10")
        }
        val generationMs = elapsedMs(generationStart)

        val memBeforeInsert = usedMemoryMb()
        val insertStart = System.nanoTime()
        db.vehicleDao().insertAll(vehicles)
        db.chargerDao().insertAll(chargers)
        db.chargeSessionDao().insertAll(generated)
        val insertMs = elapsedMs(insertStart)

        val readStart = System.nanoTime()
        val storedSessions = db.chargeSessionDao().getAll()
        val readMs = elapsedMs(readStart)
        val memAfterRead = usedMemoryMb()

        assertEquals("Expected 10 vehicles", 10, db.vehicleDao().getAll().size)
        assertEquals("Expected 12 chargers", 12, db.chargerDao().getAll().size)
        assertEquals("Expected 10,000 sessions", 10_000, storedSessions.size)

        val updateCandidate = storedSessions.first()
        val updated = updateCandidate.copy(timestampMs = updateCandidate.timestampMs - DAY_MS)
        db.chargeSessionDao().insert(updated)
        val updatedFromDb = db.chargeSessionDao().getAll().first { it.id == updated.id }
        assertEquals("Updated timestamp should persist", updated.timestampMs, updatedFromDb.timestampMs)

        val duplicate = updateCandidate.copy(
            id = java.util.UUID.randomUUID().toString(),
            timestampMs = updateCandidate.timestampMs + DAY_MS
        )
        db.chargeSessionDao().insert(duplicate)
        assertEquals("Duplicate insertion should increase count", 10_001, db.chargeSessionDao().getAll().size)

        db.chargeSessionDao().deleteById(duplicate.id)
        assertEquals("Delete-by-id should remove duplicated record", 10_000, db.chargeSessionDao().getAll().size)

        val yearAgo = System.currentTimeMillis() - 365L * DAY_MS
        val recentSessions = storedSessions.filter { it.timestampMs >= yearAgo }
        val byNetwork = storedSessions.groupBy { it.chargerNetwork }
        val byTag = storedSessions.groupBy { it.sessionTag }
        val totalEnergy = storedSessions.sumOf { it.energyKwh }
        val totalCost = storedSessions.sumOf { it.costAmount }

        assertTrue("Expected sessions in last year", recentSessions.isNotEmpty())
        assertTrue("Expected multiple network buckets", byNetwork.size >= 2)
        assertTrue("Expected multiple tag buckets", byTag.size >= 3)
        assertTrue("Total energy should be positive", totalEnergy > 0.0)
        assertTrue("Total cost should be positive", totalCost > 0.0)

        val warnings = mutableListOf<String>()
        if (insertMs > 12_000) warnings += "Insert time exceeded 12s: ${insertMs}ms"
        if (readMs > 4_000) warnings += "Read time exceeded 4s: ${readMs}ms"
        if (memAfterRead - memBeforeInsert > 300) warnings += "Memory delta exceeded 300MB: ${memAfterRead - memBeforeInsert}MB"

        report("generation_ms=$generationMs")
        report("insert_ms=$insertMs")
        report("read_ms=$readMs")
        report("mem_before_insert_mb=$memBeforeInsert")
        report("mem_after_read_mb=$memAfterRead")
        report("recent_year_sessions=${recentSessions.size}")
        report("network_buckets=${byNetwork.size}")
        report("tag_buckets=${byTag.size}")
        report("total_energy_kwh=${"%.2f".format(totalEnergy)}")
        report("total_cost=${"%.2f".format(totalCost)}")

        if (warnings.isEmpty()) {
            report("warnings=none")
        } else {
            warnings.forEach { report("warning=$it") }
        }
    }

    private fun elapsedMs(startNano: Long): Long = (System.nanoTime() - startNano) / 1_000_000

    private fun usedMemoryMb(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
    }

    private fun report(message: String) {
        Log.i("STRESS_REPORT", message)
        println("STRESS_REPORT: $message")
    }

    companion object {
        private const val DAY_MS = 24L * 60L * 60L * 1000L
        private const val FIVE_YEARS_MS = 5L * 365L * DAY_MS
        private val SESSION_TAGS = listOf("Home", "Public", "Work", "Rapid", "Other")
    }
}
