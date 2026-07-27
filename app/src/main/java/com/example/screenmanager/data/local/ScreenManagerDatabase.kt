package com.example.screenmanager.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [AppUsageLog::class, BlockConfig::class, AppInternalState::class],
    version = 1,
    exportSchema = false
)
abstract class ScreenManagerDatabase : RoomDatabase() {
    abstract fun appUsageLogDao(): AppUsageLogDao
    abstract fun blockConfigDao(): BlockConfigDao
    abstract fun appInternalStateDao(): AppInternalStateDao

    companion object {
        @Volatile
        private var instance: ScreenManagerDatabase? = null

        fun getInstance(context: Context): ScreenManagerDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ScreenManagerDatabase::class.java,
                    "screen_manager.db"
                ).build().also { instance = it }
            }
        }
    }
}
