package com.example.screenmanager.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        AppUsageLog::class,
        BlockConfig::class,
        AppInternalState::class,
        AlarmEntity::class,
        AppLimitRuleEntity::class,
        ShortVideoConfigEntity::class,
        ScheduleRuleEntity::class,
        WakeUpConfigEntity::class,
        EmergencySessionConfigEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(LocalDateTimeConverter::class)
abstract class ScreenManagerDatabase : RoomDatabase() {
    abstract fun appUsageLogDao(): AppUsageLogDao
    abstract fun blockConfigDao(): BlockConfigDao
    abstract fun appInternalStateDao(): AppInternalStateDao
    abstract fun alarmDao(): AlarmDao
    abstract fun appLimitRuleDao(): AppLimitRuleDao
    abstract fun shortVideoConfigDao(): ShortVideoConfigDao
    abstract fun scheduleRuleDao(): ScheduleRuleDao
    abstract fun wakeUpConfigDao(): WakeUpConfigDao
    abstract fun emergencySessionConfigDao(): EmergencySessionConfigDao

    companion object {
        @Volatile
        private var instance: ScreenManagerDatabase? = null

        fun getInstance(context: Context): ScreenManagerDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ScreenManagerDatabase::class.java,
                    "screen_manager.db"
                ).fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
        }
    }
}
