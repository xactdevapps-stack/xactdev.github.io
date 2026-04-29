package com.evchargecalc.app.storage

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.withTransaction
import com.evchargecalc.app.model.ChargeSession
import com.evchargecalc.app.model.ChargerProfile
import com.evchargecalc.app.model.DistanceUnit
import com.evchargecalc.app.model.ThemeMode
import com.evchargecalc.app.model.VehicleProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class AppStorage(context: Context) {
    private val appContext = context.applicationContext
    private val db = AppDatabase.getInstance(appContext)
    private val prefs = appContext.settingsDataStore

    suspend fun loadVehicles(): List<VehicleProfile> {
        return runCatching { db.vehicleDao().getAll() }.getOrDefault(emptyList())
    }

    suspend fun saveVehicles(vehicles: List<VehicleProfile>) {
        db.withTransaction {
            db.vehicleDao().clearAll()
            db.vehicleDao().insertAll(vehicles)
        }
    }

    suspend fun loadChargers(): List<ChargerProfile> {
        return runCatching { db.chargerDao().getAll() }.getOrDefault(emptyList())
    }

    suspend fun saveChargers(chargers: List<ChargerProfile>) {
        db.withTransaction {
            db.chargerDao().clearAll()
            db.chargerDao().insertAll(chargers)
        }
    }

    suspend fun loadThemeMode(): ThemeMode {
        return runCatching {
            ThemeMode.valueOf(
                prefs.data.map { it[PreferencesKeys.theme] ?: ThemeMode.DARK.name }.first()
            )
        }.getOrDefault(ThemeMode.DARK)
    }

    suspend fun saveThemeMode(mode: ThemeMode) {
        prefs.edit { it[PreferencesKeys.theme] = mode.name }
    }

    suspend fun loadDistanceUnit(): DistanceUnit {
        return runCatching {
            DistanceUnit.valueOf(
                prefs.data.map { it[PreferencesKeys.distanceUnit] ?: DistanceUnit.KM.name }.first()
            )
        }.getOrDefault(DistanceUnit.KM)
    }

    suspend fun saveDistanceUnit(unit: DistanceUnit) {
        prefs.edit { it[PreferencesKeys.distanceUnit] = unit.name }
    }

    suspend fun loadCurrencyCode(): String {
        return prefs.data.map { it[PreferencesKeys.currencyCode] ?: "GBP" }.first()
    }

    suspend fun saveCurrencyCode(code: String) {
        prefs.edit { it[PreferencesKeys.currencyCode] = code }
    }

    suspend fun loadChargeSessions(): List<ChargeSession> {
        return runCatching { db.chargeSessionDao().getAll() }.getOrDefault(emptyList())
    }

    suspend fun saveChargeSessions(sessions: List<ChargeSession>) {
        db.withTransaction {
            db.chargeSessionDao().clearAll()
            db.chargeSessionDao().insertAll(sessions)
        }
    }

    suspend fun addChargeSession(session: ChargeSession) {
        db.chargeSessionDao().insert(session)
    }

    suspend fun clearAll() {
        db.withTransaction {
            db.vehicleDao().clearAll()
            db.chargerDao().clearAll()
            db.chargeSessionDao().clearAll()
        }
        prefs.edit { it.clear() }
    }

    private object PreferencesKeys {
        val theme = stringPreferencesKey("theme")
        val distanceUnit = stringPreferencesKey("distance_unit")
        val currencyCode = stringPreferencesKey("currency_code")
    }
}

private val Context.settingsDataStore by preferencesDataStore(name = "ev_charge_calc_settings")
