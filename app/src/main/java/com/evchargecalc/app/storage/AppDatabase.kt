package com.evchargecalc.app.storage

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun chargerDao(): ChargerDao
    abstract fun chargeSessionDao(): ChargeSessionDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE vehicle_profiles ADD COLUMN chartColorHex TEXT NOT NULL DEFAULT '#9FFF5E'"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE charger_profiles ADD COLUMN networkName TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "ALTER TABLE charge_sessions ADD COLUMN chargerNetwork TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "ALTER TABLE charge_sessions ADD COLUMN sessionTag TEXT NOT NULL DEFAULT 'Home'"
                )
                db.execSQL(
                    "ALTER TABLE charge_sessions ADD COLUMN notes TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "ALTER TABLE charge_sessions ADD COLUMN distanceDrivenKm REAL"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ev_charge_calc.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
        }
    }
}