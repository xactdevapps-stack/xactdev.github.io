package com.evchargecalc.app.storage

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.evchargecalc.app.model.ChargeSession
import com.evchargecalc.app.model.ChargerProfile
import com.evchargecalc.app.model.VehicleProfile

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicle_profiles")
    suspend fun getAll(): List<VehicleProfile>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<VehicleProfile>)

    @Query("DELETE FROM vehicle_profiles")
    suspend fun clearAll()
}

@Dao
interface ChargerDao {
    @Query("SELECT * FROM charger_profiles")
    suspend fun getAll(): List<ChargerProfile>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ChargerProfile>)

    @Query("DELETE FROM charger_profiles")
    suspend fun clearAll()
}

@Dao
interface ChargeSessionDao {
    @Query("SELECT * FROM charge_sessions ORDER BY timestampMs DESC")
    suspend fun getAll(): List<ChargeSession>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ChargeSession)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ChargeSession>)

    @Query("DELETE FROM charge_sessions")
    suspend fun clearAll()
}

@Database(
    entities = [VehicleProfile::class, ChargerProfile::class, ChargeSession::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun chargerDao(): ChargerDao
    abstract fun chargeSessionDao(): ChargeSessionDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ev_charge_calc.db"
                ).build().also { instance = it }
            }
        }
    }
}