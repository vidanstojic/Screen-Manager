package com.example.screenmanager.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v5 (refaktor Phase 1):
 * - vraćen unique indeks na app_usage_log (paket + start) — B1
 * - nova tabela usage_hourly (rollup-ovi, TRS 2.1)
 * - session_limit_rules / session_limit_state (interval mod, TRS 2.4)
 * - emergency_sessions: activeUntilMillis umesto isActive + labela
 * - uklonjena block_config tabela (zamenjena korisničkim pravilima)
 *
 * v6: short_video_configs dobija `mode` (BLOCKED/BUDGET/SESSIONS) i parametre
 * interval moda za Shorts/Reels — PRAVA migracija ([MIGRATION_5_6]), pa se
 * korisnička pravila i istorija čuvaju.
 *
 * Migracija sa verzija < 5 je destruktivna (projekat je u razvoju): pravila se ponovo
 * seed-uju u [com.example.screenmanager.ScreenManagerApplication], a
 * istorija potrošnje se ponovo povlači iz UsageStats (do 7 dana unazad).
 */
@Database(
    entities = [
        AppUsageLog::class,
        UsageHourlyEntity::class,
        AppInternalState::class,
        AlarmEntity::class,
        AppLimitRuleEntity::class,
        ShortVideoConfigEntity::class,
        ScheduleRuleEntity::class,
        WakeUpConfigEntity::class,
        EmergencySessionConfigEntity::class,
        SessionLimitRuleEntity::class,
        SessionLimitStateEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class ScreenManagerDatabase : RoomDatabase() {
    abstract fun appUsageLogDao(): AppUsageLogDao
    abstract fun usageHourlyDao(): UsageHourlyDao
    abstract fun appInternalStateDao(): AppInternalStateDao
    abstract fun alarmDao(): AlarmDao
    abstract fun appLimitRuleDao(): AppLimitRuleDao
    abstract fun shortVideoConfigDao(): ShortVideoConfigDao
    abstract fun scheduleRuleDao(): ScheduleRuleDao
    abstract fun wakeUpConfigDao(): WakeUpConfigDao
    abstract fun emergencySessionConfigDao(): EmergencySessionConfigDao
    abstract fun sessionLimitDao(): SessionLimitDao

    companion object {
        @Volatile
        private var instance: ScreenManagerDatabase? = null

        /** Mora da odgovara @ColumnInfo(defaultValue) u [ShortVideoConfigEntity]. */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `short_video_configs` ADD COLUMN `mode` TEXT NOT NULL DEFAULT 'BUDGET'")
                db.execSQL("ALTER TABLE `short_video_configs` ADD COLUMN `sessionLengthMinutes` INTEGER NOT NULL DEFAULT 5")
                db.execSQL("ALTER TABLE `short_video_configs` ADD COLUMN `maxSessions` INTEGER NOT NULL DEFAULT 3")
                db.execSQL("ALTER TABLE `short_video_configs` ADD COLUMN `cooldownMinutes` INTEGER NOT NULL DEFAULT 30")
            }
        }

        fun getInstance(context: Context): ScreenManagerDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ScreenManagerDatabase::class.java,
                    "screen_manager.db"
                ).addMigrations(MIGRATION_5_6)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build().also { instance = it }
            }
        }
    }
}
