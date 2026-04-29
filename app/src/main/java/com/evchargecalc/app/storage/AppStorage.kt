package com.evchargecalc.app.storage

import android.content.Context
import com.evchargecalc.app.model.ChargeSession
import com.evchargecalc.app.model.ChargerProfile
import com.evchargecalc.app.model.DistanceUnit
import com.evchargecalc.app.model.ThemeMode
import com.evchargecalc.app.model.VehicleProfile
import org.json.JSONArray
import org.json.JSONObject

class AppStorage(context: Context) {
    private val prefs = context.getSharedPreferences("ev_charge_calc", Context.MODE_PRIVATE)

    fun loadVehicles(): List<VehicleProfile> {
        val raw = prefs.getString("vehicles", "[]") ?: "[]"
        val arr = JSONArray(raw)
        val items = mutableListOf<VehicleProfile>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            items.add(
                VehicleProfile(
                    id = obj.optString("id"),
                    make = obj.optString("make"),
                    model = obj.optString("model"),
                    batteryCapacityKwh = obj.optDouble("batteryCapacityKwh"),
                    defaultTargetPercent = obj.optInt("defaultTargetPercent"),
                    estimatedRangeKm = obj.optDouble("estimatedRangeKm"),
                    isDefault = obj.optBoolean("isDefault")
                )
            )
        }
        return items
    }

    fun saveVehicles(vehicles: List<VehicleProfile>) {
        val arr = JSONArray()
        vehicles.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("make", it.make)
                    .put("model", it.model)
                    .put("batteryCapacityKwh", it.batteryCapacityKwh)
                    .put("defaultTargetPercent", it.defaultTargetPercent)
                    .put("estimatedRangeKm", it.estimatedRangeKm)
                    .put("isDefault", it.isDefault)
            )
        }
        prefs.edit().putString("vehicles", arr.toString()).apply()
    }

    fun loadChargers(): List<ChargerProfile> {
        val raw = prefs.getString("chargers", "[]") ?: "[]"
        val arr = JSONArray(raw)
        val items = mutableListOf<ChargerProfile>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            items.add(
                ChargerProfile(
                    id = obj.optString("id"),
                    name = obj.optString("name"),
                    location = obj.optString("location"),
                    chargeRateKw = obj.optDouble("chargeRateKw"),
                    pricePerKwh = obj.optDouble("pricePerKwh"),
                    isDefault = obj.optBoolean("isDefault")
                )
            )
        }
        return items
    }

    fun saveChargers(chargers: List<ChargerProfile>) {
        val arr = JSONArray()
        chargers.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("name", it.name)
                    .put("location", it.location)
                    .put("chargeRateKw", it.chargeRateKw)
                    .put("pricePerKwh", it.pricePerKwh)
                    .put("isDefault", it.isDefault)
            )
        }
        prefs.edit().putString("chargers", arr.toString()).apply()
    }

    fun loadThemeMode(): ThemeMode {
        return runCatching {
            ThemeMode.valueOf(prefs.getString("theme", ThemeMode.DARK.name) ?: ThemeMode.DARK.name)
        }.getOrDefault(ThemeMode.DARK)
    }

    fun saveThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme", mode.name).apply()
    }

    fun loadDistanceUnit(): DistanceUnit {
        return runCatching {
            DistanceUnit.valueOf(
                prefs.getString("distance_unit", DistanceUnit.KM.name) ?: DistanceUnit.KM.name
            )
        }.getOrDefault(DistanceUnit.KM)
    }

    fun saveDistanceUnit(unit: DistanceUnit) {
        prefs.edit().putString("distance_unit", unit.name).apply()
    }

    fun loadCurrencyCode(): String {
        return prefs.getString("currency_code", "GBP") ?: "GBP"
    }

    fun saveCurrencyCode(code: String) {
        prefs.edit().putString("currency_code", code).apply()
    }

    fun loadChargeSessions(): List<ChargeSession> {
        val raw = prefs.getString("charge_sessions", "[]") ?: "[]"
        val arr = JSONArray(raw)
        val items = mutableListOf<ChargeSession>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            items.add(
                ChargeSession(
                    id = obj.optString("id"),
                    timestampMs = obj.optLong("timestampMs"),
                    vehicleId = obj.optString("vehicleId"),
                    vehicleName = obj.optString("vehicleName"),
                    chargerId = obj.optString("chargerId"),
                    chargerName = obj.optString("chargerName"),
                    chargerLocation = obj.optString("chargerLocation"),
                    energyKwh = obj.optDouble("energyKwh"),
                    timeHours = obj.optDouble("timeHours"),
                    costAmount = obj.optDouble("costAmount"),
                    currencyCode = obj.optString("currencyCode", "GBP")
                )
            )
        }
        return items.sortedByDescending { it.timestampMs }
    }

    fun saveChargeSessions(sessions: List<ChargeSession>) {
        val arr = JSONArray()
        sessions.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("timestampMs", it.timestampMs)
                    .put("vehicleId", it.vehicleId)
                    .put("vehicleName", it.vehicleName)
                    .put("chargerId", it.chargerId)
                    .put("chargerName", it.chargerName)
                    .put("chargerLocation", it.chargerLocation)
                    .put("energyKwh", it.energyKwh)
                    .put("timeHours", it.timeHours)
                    .put("costAmount", it.costAmount)
                    .put("currencyCode", it.currencyCode)
            )
        }
        prefs.edit().putString("charge_sessions", arr.toString()).apply()
    }
}
