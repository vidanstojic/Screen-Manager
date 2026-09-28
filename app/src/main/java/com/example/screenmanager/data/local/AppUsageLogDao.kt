package com.example.screenmanager.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AppUsageLogDao {
    /** REPLACE: ista sesija (paket + start) se prepisuje novijim krajem. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(logs: List<AppUsageLog>)

    /** Sve sesije koje se preklapaju sa [fromTimestamp, toTimestamp). */
    @Query(
        """
        SELECT * FROM app_usage_log
        WHERE endTimeStamp > :fromTimestamp AND startTimeStamp < :toTimestamp
        ORDER BY startTimeStamp ASC
        """
    )
    suspend fun sessionsOverlapping(fromTimestamp: Long, toTimestamp: Long): List<AppUsageLog>

    /** Briše sesije koje počinju od [fromTimestamp] — koristi se pre punog backfill-a. */
    @Query("DELETE FROM app_usage_log WHERE startTimeStamp >= :fromTimestamp")
    suspend fun deleteStartingFrom(fromTimestamp: Long)

    @Query("SELECT COUNT(*) FROM app_usage_log")
    suspend fun count(): Int

    @Query("DELETE FROM app_usage_log WHERE endTimeStamp < :beforeTimestamp")
    suspend fun deleteOlderThan(beforeTimestamp: Long)
}
