package com.evchargecalc.app.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class AppTab { CALCULATE, VEHICLES, CHARGERS, HISTORY, SETTINGS }
enum class ThemeMode { SYSTEM, DARK, LIGHT }
enum class DistanceUnit { KM, MI }
enum class PeriodFilter { ALL, LAST_YEAR, LAST_MONTH, LAST_WEEK }
enum class ChartMetric { COST, ENERGY, TIME }

val chargeSessionTagOptions = listOf(
    "Home",
    "Public",
    "Work",
    "Rapid",
    "Other"
)

private const val KM_PER_MILE = 1.609344

fun Double.kmToSelected(unit: DistanceUnit): Double =
    if (unit == DistanceUnit.MI) this / KM_PER_MILE else this

fun Double.selectedToKm(unit: DistanceUnit): Double =
    if (unit == DistanceUnit.MI) this * KM_PER_MILE else this

@Entity(tableName = "charge_sessions", indices = [Index("timestampMs"), Index("currencyCode")])
data class ChargeSession(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val timestampMs: Long = System.currentTimeMillis(),
    val vehicleId: String,
    val vehicleName: String,
    val chargerId: String,
    val chargerName: String,
    val chargerLocation: String,
    val chargerNetwork: String = "",
    val energyKwh: Double,
    val timeHours: Double,
    val costAmount: Double,
    val currencyCode: String,
    val sessionTag: String = "Home",
    val notes: String = "",
    val distanceDrivenKm: Double? = null
)

val knownManufacturers = listOf(
    "Audi",
    "BMW",
    "BYD",
    "Chevrolet",
    "Citroen",
    "Cupra",
    "Dacia",
    "Fiat",
    "Ford",
    "Genesis",
    "GMC",
    "Honda",
    "Hyundai",
    "Jaguar",
    "Jeep",
    "Kia",
    "Lexus",
    "Lucid",
    "Mazda",
    "Mercedes-Benz",
    "MG",
    "Mini",
    "NIO",
    "Nissan",
    "Peugeot",
    "Polestar",
    "Porsche",
    "Renault",
    "Rivian",
    "Skoda",
    "Smart",
    "Subaru",
    "Tesla",
    "Toyota",
    "Vauxhall",
    "Volkswagen",
    "Volvo",
    "XPeng",
    "Other"
)

const val DEFAULT_VEHICLE_CHART_COLOR = "#9FFF5E"

data class VehicleChartColorOption(
    val label: String,
    val hex: String
)

val vehicleChartColorOptions = listOf(
    VehicleChartColorOption("Matrix Green", DEFAULT_VEHICLE_CHART_COLOR),
    VehicleChartColorOption("Electric Teal", "#4BEA89"),
    VehicleChartColorOption("Grid Blue", "#3DB2FF"),
    VehicleChartColorOption("Fast Amber", "#FFB020"),
    VehicleChartColorOption("Charge Red", "#FF6B6B"),
    VehicleChartColorOption("Neon Violet", "#C77DFF"),
    VehicleChartColorOption("Solar Yellow", "#FFD166"),
    VehicleChartColorOption("Aqua Mint", "#06D6A0")
)

@Entity(tableName = "vehicle_profiles")
data class VehicleProfile(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val make: String,
    val model: String,
    val batteryCapacityKwh: Double,
    val defaultTargetPercent: Int,
    val estimatedRangeKm: Double,
    val chartColorHex: String = DEFAULT_VEHICLE_CHART_COLOR,
    val isDefault: Boolean = false
)

@Entity(tableName = "charger_profiles")
data class ChargerProfile(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val location: String,
    val networkName: String = "",
    val chargeRateKw: Double,
    val pricePerKwh: Double,
    val latitude: Double? = null,  // For OpenStreetMap integration
    val longitude: Double? = null, // For OpenStreetMap integration
    val isDefault: Boolean = false
)
