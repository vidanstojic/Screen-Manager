package com.example.screenmanager.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class UsageHourlyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(rows: List<UsageHourlyEntity>)

    @Query("DELETE FROM usage_hourly WHERE epochDay >= :fromEpochDay")
    abstract suspend fun deleteFromDay(fromEpochDay: Long)

    @Query("DELETE FROM usage_hourly WHERE epochDay < :beforeEpochDay")
    abstract suspend fun deleteOlderThan(beforeEpochDay: Long)

    /** Atomsko preračunavanje: stari rollup-ovi od [fromEpochDay] se zamenjuju novim. */
    @Transaction
    open suspend fun replaceFromDay(fromEpochDay: Long, rows: List<UsageHourlyEntity>) {
        deleteFromDay(fromEpochDay)
        if (rows.isNotEmpty()) insertAll(rows)
    }

    // --- Sve aplikacije zbirno ---

    @Query(
        """
        SELECT hour, SUM(durationMs) AS durationMs
        FROM usage_hourly
        WHERE epochDay = :epochDay
        GROUP BY hour
        ORDER BY hour ASC
        """
    )
    abstract fun observeHourly(epochDay: Long): Flow<List<HourlyUsage>>

    @Query(
        """
        SELECT CAST(epochDay - :fromEpochDay AS INTEGER) AS day, SUM(durationMs) AS durationMs
        FROM usage_hourly
        WHERE epochDay BETWEEN :fromEpochDay AND :toEpochDay
        GROUP BY epochDay
        ORDER BY epochDay ASC
        """
    )
    abstract fun observeDaily(fromEpochDay: Long, toEpochDay: Long): Flow<List<DailyUsage>>

    @Query(
        """
        SELECT packageName, SUM(durationMs) AS totalDurationMs
        FROM usage_hourly
        WHERE epochDay BETWEEN :fromEpochDay AND :toEpochDay
        GROUP BY packageName
        ORDER BY totalDurationMs DESC
        """
    )
    abstract fun observeTotals(fromEpochDay: Long, toEpochDay: Long): Flow<List<UsageSummary>>

    @Query(
        """
        SELECT packageName, SUM(durationMs) AS totalDurationMs
        FROM usage_hourly
        WHERE epochDay = :epochDay
        GROUP BY packageName
        """
    )
    abstract suspend fun totalsForDay(epochDay: Long): List<UsageSummary>

    // --- Jedna aplikacija ---

    @Query(
        """
        SELECT hour, SUM(durationMs) AS durationMs
        FROM usage_hourly
        WHERE epochDay = :epochDay AND packageName = :packageName
        GROUP BY hour
        ORDER BY hour ASC
        """
    )
    abstract fun observeHourlyForApp(epochDay: Long, packageName: String): Flow<List<HourlyUsage>>

    @Query(
        """
        SELECT CAST(epochDay - :fromEpochDay AS INTEGER) AS day, SUM(durationMs) AS durationMs
        FROM usage_hourly
        WHERE epochDay BETWEEN :fromEpochDay AND :toEpochDay AND packageName = :packageName
        GROUP BY epochDay
        ORDER BY epochDay ASC
        """
    )
    abstract fun observeDailyForApp(fromEpochDay: Long, toEpochDay: Long, packageName: String): Flow<List<DailyUsage>>

    @Query(
        """
        SELECT COALESCE(SUM(sessionStarts), 0)
        FROM usage_hourly
        WHERE epochDay = :epochDay AND packageName = :packageName
        """
    )
    abstract fun observeSessionCountForApp(epochDay: Long, packageName: String): Flow<Int>
}
