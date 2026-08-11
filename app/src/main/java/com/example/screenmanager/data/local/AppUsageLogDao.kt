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

    /**
     * Ukupno (sve aplikacije zbirno) po satu, ograničeno na jedan dan.
     * NAPOMENA: dodata je gornja granica (dayEndTimestamp) koja je pre
     * nedostajala — bez nje bi izbor prošlog dana pokupio i podatke
     * nakon njega.
     */
    @Query(
        """
        SELECT CAST(((startTimeStamp - :dayStartTimestamp) / 3600000) AS INTEGER) AS hour,
               SUM(durationMs) AS durationMs
        FROM app_usage_log
        WHERE startTimeStamp >= :dayStartTimestamp AND startTimeStamp < :dayEndTimestamp
        GROUP BY hour
        ORDER BY hour ASC
        """
    )
    fun observeHourlyUsage(dayStartTimestamp: Long, dayEndTimestamp: Long): Flow<List<HourlyUsage>>

    /**
     * Jedna aplikacija, po satu, ograničeno na jedan dan.
     * Koristi se kad korisnik uđe u detalje aplikacije za konkretan dan.
     */
    @Query(
        """
        SELECT CAST(((startTimeStamp - :dayStartTimestamp) / 3600000) AS INTEGER) AS hour,
               SUM(durationMs) AS durationMs
        FROM app_usage_log
        WHERE startTimeStamp >= :dayStartTimestamp
              AND startTimeStamp < :dayEndTimestamp
              AND packageName = :packageName
        GROUP BY hour
        ORDER BY hour ASC
        """
    )
    fun observeHourlyUsageForApp(
        dayStartTimestamp: Long,
        dayEndTimestamp: Long,
        packageName: String
    ): Flow<List<HourlyUsage>>

    /**
     * Ukupno (sve aplikacije zbirno) po danu, za glavni "Week" grafik.
     */
    @Query(
        """
        SELECT CAST(((startTimeStamp - :weekStartTimestamp) / 86400000) AS INTEGER) AS day,
               SUM(durationMs) AS durationMs
        FROM app_usage_log
        WHERE startTimeStamp >= :weekStartTimestamp
        GROUP BY day
        ORDER BY day ASC
        """
    )
    fun observeDailyTotalsAllApps(weekStartTimestamp: Long): Flow<List<DailyUsage>>

    /**
     * Jedna aplikacija, po danu — 7-dnevni trend za taj konkretan app.
     */
    @Query(
        """
        SELECT CAST(((startTimeStamp - :weekStartTimestamp) / 86400000) AS INTEGER) AS day,
               SUM(durationMs) AS durationMs
        FROM app_usage_log
        WHERE startTimeStamp >= :weekStartTimestamp AND packageName = :packageName
        GROUP BY day
        ORDER BY day ASC
        """
    )
    fun observeDailyUsageForApp(weekStartTimestamp: Long, packageName: String): Flow<List<DailyUsage>>

    /**
     * Briše logove starije od zadate granice. Za sada čuvamo samo
     * poslednjih 7 dana (vidi UsageStatsRepository.cleanupOldLogs).
     */
    @Query("DELETE FROM app_usage_log WHERE endTimeStamp < :beforeTimestamp")
    suspend fun deleteOlderThan(beforeTimestamp: Long)
}