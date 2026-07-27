package com.example.screenmanager.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppUsageLogDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(log: AppUsageLog)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(logs: List<AppUsageLog>)

    @Query(
        """
        SELECT packageName, SUM(durationMs) AS totalDurationMs
        FROM app_usage_log
        WHERE startTimeStamp >= :fromTimestamp
        GROUP BY packageName
        ORDER BY totalDurationMs DESC
        """
    )
    fun observeTotalsSince(fromTimestamp: Long): Flow<List<UsageSummary>>

    @Query(
        """
        SELECT CAST(((startTimeStamp - :dayStartTimestamp) / 3600000) AS INTEGER) AS hour,
               SUM(durationMs) AS durationMs
        FROM app_usage_log
        WHERE startTimeStamp >= :dayStartTimestamp
        GROUP BY hour
        ORDER BY hour ASC
        """
    )
    fun observeHourlyUsage(dayStartTimestamp: Long): Flow<List<HourlyUsage>>

    @Query("DELETE FROM app_usage_log WHERE endTimeStamp < :beforeTimestamp")
    suspend fun deleteOlderThan(beforeTimestamp: Long)
}
