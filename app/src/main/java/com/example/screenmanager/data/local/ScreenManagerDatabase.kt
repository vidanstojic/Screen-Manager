package com.example.screenmanager.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * v5 (refaktor Phase 1):
 * - vraćen unique indeks na app_usage_log (paket + start) — B1
 * - nova tabela usage_hourly (rollup-ovi, TRS 2.1)
 * - session_limit_rules / session_limit_state (interval mod, TRS 2.4)
 * - emergency_sessions: activeUntilMillis umesto isActive + labela
 * - uklonjena block_config tabela (zamenjena korisničkim pravilima)
 *
 * Migracija je destruktivna (projekat je u razvoju): pravila se ponovo
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
    version = 5,
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

        fun getInstance(context: Context): ScreenManagerDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ScreenManagerDatabase::class.java,
                    "screen_manager.db"
                ).fallbackToDestructiveMigration(dropAllTables = true)
                    .build().also { instance = it }
            }
        }
    }
}
