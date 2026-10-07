package com.example.screenmanager.ui.feature.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.screenmanager.data.local.UsageSummary
import com.example.screenmanager.domain.ServiceLocator
import com.example.screenmanager.domain.TimeBuckets
import com.example.screenmanager.ui.model.AppUsageItem
import com.example.screenmanager.ui.model.DayOption
import com.example.screenmanager.ui.model.UsageRange
import com.example.screenmanager.ui.model.lastSevenDays
import com.example.screenmanager.ui.model.toDailyMs
import com.example.screenmanager.ui.model.toHourlyMs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Sve što Stats ekran prikazuje za izabrani period. */
data class StatsUiState(
    val range: UsageRange = UsageRange.Day,
    val days: List<DayOption> = emptyList(),
    val selectedDayStart: Long = 0L,
    /** Naslov perioda: "Today", "Tue, 6 Oct", "Last 7 days", "October". */
    val periodTitle: String = "",
    /** Vrednost svakog stubića grafika (sat ili dan), u ms. */
    val chartValuesMs: List<Long> = emptyList(),
    /** Natpis ispod stubića; null = bez natpisa. */
    val chartSlotLabels: List<String?> = emptyList(),
    /** Pun opis stubića za prikaz kad ga korisnik dodirne: "14:00–15:00", "Tue 6". */
    val chartSlotTitles: List<String> = emptyList(),
    val totalMs: Long = 0L,
    /** Prosek po danu (samo Week/Month). */
    val dailyAverageMs: Long? = null,
    val apps: List<AppUsageItem> = emptyList()
)

/**
 * ViewModel Stats ekrana. Sve dolazi iz satnih rollup-ova (usage_hourly);
 * sync radi FocusMonitorService i [com.example.screenmanager.ui.AppViewModel].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModel(application: Application) : AndroidViewModel(application) {
    private val usageRepository = ServiceLocator.usageStatsRepository(application)
    private val installedApps = ServiceLocator.installedApps(application)

    private val range = MutableStateFlow(UsageRange.Day)
    private val today = MutableStateFlow(TimeBuckets.startOfToday())
    private val selectedDay = MutableStateFlow(today.value)

    /** Šta se trenutno prikazuje; promena bilo čega ponovo pokreće upite. */
    private data class Query(val range: UsageRange, val dayStart: Long, val today: Long)

    private val query = combine(range, selectedDay, today, ::Query)

    private val chartValues: Flow<List<Long>> = query.flatMapLatest { q ->
        when (q.range) {
            UsageRange.Day -> usageRepository.observeHourlyUsageForDay(q.dayStart).map { it.toHourlyMs() }
            UsageRange.Week -> usageRepository.observeWeeklyDailyBreakdown().map { it.toDailyMs(7) }
            UsageRange.Month -> usageRepository.observeMonthlyDailyBreakdown()
                .map { it.toDailyMs(q.today.toLocalDate().lengthOfMonth()) }
        }
    }

    private val apps: Flow<List<AppUsageItem>> = query.flatMapLatest { q ->
        when (q.range) {
            UsageRange.Day -> usageRepository.observeTotalsForDay(q.dayStart)
            UsageRange.Week -> usageRepository.observeRollingWeekTotals()
            UsageRange.Month -> usageRepository.observeMonthlyTotals()
        }
    }.map { it.toAppUsageItems() }.flowOn(Dispatchers.Default)

    val uiState: StateFlow<StatsUiState> = combine(query, chartValues, apps) { q, values, apps ->
        val days = lastSevenDays(q.today)
        val todayDate = q.today.toLocalDate()
        val total = values.sum()
        StatsUiState(
            range = q.range,
            days = days,
            selectedDayStart = q.dayStart,
            periodTitle = periodTitle(q, todayDate),
            chartValuesMs = values,
            chartSlotLabels = slotLabels(q.range, days, values.size),
            chartSlotTitles = slotTitles(q.range, days, todayDate, values.size),
            totalMs = total,
            dailyAverageMs = when (q.range) {
                UsageRange.Day -> null
                UsageRange.Week -> total / 7
                // Prosek samo preko dana koji su do sada prošli u mesecu.
                UsageRange.Month -> total / todayDate.dayOfMonth
            },
            apps = apps
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    fun selectRange(newRange: UsageRange) {
        range.value = newRange
    }

    fun selectDay(dayStart: Long) {
        selectedDay.value = dayStart
    }

    /** Posle ponoći "danas" više nije isti dan — pomeri prozor (ViewModel živi danima). */
    fun onResume() {
        val newToday = TimeBuckets.startOfToday()
        val oldToday = today.value
        if (newToday == oldToday) return
        if (selectedDay.value == oldToday || selectedDay.value < newToday - 6 * TimeBuckets.DAY_MS) {
            selectedDay.value = newToday
        }
        today.value = newToday
    }

    private fun List<UsageSummary>.toAppUsageItems(): List<AppUsageItem> =
        filter { it.totalDurationMs > 0 }
            .map { AppUsageItem(it.packageName, installedApps.label(it.packageName), it.totalDurationMs) }
            .sortedByDescending { it.durationMs }

    private fun periodTitle(q: Query, today: LocalDate): String = when (q.range) {
        UsageRange.Day -> {
            val date = q.dayStart.toLocalDate()
            when (date) {
                today -> "Today"
                today.minusDays(1) -> "Yesterday"
                else -> "${date.dayOfWeek.shortName()}, ${date.dayOfMonth} ${date.month.shortName()}"
            }
        }
        UsageRange.Week -> "Last 7 days"
        UsageRange.Month -> today.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
    }

    private fun slotLabels(range: UsageRange, days: List<DayOption>, count: Int): List<String?> = when (range) {
        UsageRange.Day -> List(count) { hour -> if (hour % 6 == 0) "%02d".format(hour) else null }
        UsageRange.Week -> days.map { it.weekday }
        UsageRange.Month -> List(count) { index -> if (index % 7 == 0) (index + 1).toString() else null }
    }

    private fun slotTitles(range: UsageRange, days: List<DayOption>, today: LocalDate, count: Int): List<String> =
        when (range) {
            UsageRange.Day -> List(count) { hour -> "%02d:00–%02d:00".format(hour, hour + 1) }
            UsageRange.Week -> days.map { "${it.weekday} ${it.dayOfMonth}" }
            UsageRange.Month -> List(count) { index -> "${today.month.shortName()} ${index + 1}" }
        }

    private fun Long.toLocalDate(): LocalDate = LocalDate.ofEpochDay(TimeBuckets.epochDay(this))
    private fun java.time.DayOfWeek.shortName(): String = getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
    private fun java.time.Month.shortName(): String = getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
}
