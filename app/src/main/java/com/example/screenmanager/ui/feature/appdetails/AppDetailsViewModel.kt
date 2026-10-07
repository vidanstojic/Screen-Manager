package com.example.screenmanager.ui.feature.appdetails

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.screenmanager.domain.ServiceLocator
import com.example.screenmanager.domain.TimeBuckets
import com.example.screenmanager.domain.rules.RulesSnapshot
import com.example.screenmanager.ui.feature.limits.RuleKind
import com.example.screenmanager.ui.feature.limits.RuleTarget
import com.example.screenmanager.ui.feature.limits.label
import com.example.screenmanager.ui.feature.limits.summary
import com.example.screenmanager.ui.model.DayOption
import com.example.screenmanager.ui.model.lastSevenDays
import com.example.screenmanager.ui.model.toDailyMs
import com.example.screenmanager.ui.model.toHourlyMs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/** Pravilo koje važi za prikazanu aplikaciju (red u kartici "Limits"). */
data class AppliedRule(
    val kind: RuleKind,
    val target: RuleTarget,
    val title: String,
    val summary: String,
    val enabled: Boolean
)

data class AppDetailsUiState(
    /** Aplikacija na koju se podaci odnose — ekran ignoriše stanje druge aplikacije. */
    val packageName: String = "",
    val days: List<DayOption> = emptyList(),
    val selectedDayStart: Long = 0L,
    /** Sati 0..23 izabranog dana, u ms. */
    val hourlyMs: List<Long> = List(24) { 0L },
    /** Poslednjih 7 dana, u ms. */
    val dailyMs: List<Long> = List(7) { 0L },
    /** Broj otvaranja aplikacije izabranog dana. */
    val sessionCount: Int = 0,
    val rules: List<AppliedRule> = emptyList()
)

/**
 * ViewModel ekrana detalja jedne aplikacije: potrošnja baš te aplikacije
 * (po satu / po danu / broj sesija) i pravila koja je pokrivaju.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppDetailsViewModel(application: Application) : AndroidViewModel(application) {
    private val usageRepository = ServiceLocator.usageStatsRepository(application)
    private val settings = ServiceLocator.settingsRepository(application)
    private val installedApps = ServiceLocator.installedApps(application)

    private val packageName = MutableStateFlow<String?>(null)
    private val selectedDay = MutableStateFlow(TimeBuckets.startOfToday())

    private data class Query(val packageName: String?, val dayStart: Long)

    private val query = combine(packageName, selectedDay, ::Query)

    private val hourly = query.flatMapLatest { q ->
        if (q.packageName == null) flowOf(emptyList())
        else usageRepository.observeHourlyUsageForApp(q.packageName, q.dayStart)
    }

    private val daily = packageName.flatMapLatest { pkg ->
        if (pkg == null) flowOf(emptyList()) else usageRepository.observeDailyUsageForApp(pkg)
    }

    private val sessions = query.flatMapLatest { q ->
        if (q.packageName == null) flowOf(0)
        else usageRepository.observeSessionCountForApp(q.packageName, q.dayStart)
    }

    val uiState: StateFlow<AppDetailsUiState> =
        combine(query, hourly, daily, sessions, settings.observeRulesSnapshot()) { q, hourly, daily, sessions, rules ->
            AppDetailsUiState(
                packageName = q.packageName.orEmpty(),
                days = lastSevenDays(),
                selectedDayStart = q.dayStart,
                hourlyMs = hourly.toHourlyMs(),
                dailyMs = daily.toDailyMs(7),
                sessionCount = sessions,
                rules = q.packageName?.let { rules.rulesFor(it) }.orEmpty()
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppDetailsUiState())

    /** Zove se po ulasku na ekran; ista instanca služi za bilo koju aplikaciju. */
    fun selectPackage(newPackageName: String) {
        if (packageName.value == newPackageName) return
        selectedDay.value = TimeBuckets.startOfToday()
        packageName.value = newPackageName
    }

    fun selectDay(dayStart: Long) {
        selectedDay.value = dayStart
    }

    /** Sva pravila (bilo koje vrste) koja pokrivaju aplikaciju [pkg]. */
    private fun RulesSnapshot.rulesFor(pkg: String): List<AppliedRule> = buildList {
        val labelFor = installedApps::label
        appLimits.filter { pkg in it.selectedAppIds }.forEach { rule ->
            add(
                AppliedRule(
                    kind = RuleKind.DailyLimit,
                    target = RuleTarget.AppLimit(rule.id),
                    title = rule.name,
                    summary = rule.summary(labelFor),
                    enabled = rule.isEnabled
                )
            )
        }
        sessionLimits.filter { pkg in it.selectedAppIds }.forEach { rule ->
            add(
                AppliedRule(
                    kind = RuleKind.SessionLimit,
                    target = RuleTarget.SessionLimit(rule.id),
                    title = rule.name,
                    summary = rule.summary(labelFor),
                    enabled = rule.isEnabled
                )
            )
        }
        schedules.filter { pkg in it.selectedAppIds }.forEach { rule ->
            add(
                AppliedRule(
                    kind = RuleKind.Schedule,
                    target = RuleTarget.Schedule(rule.id),
                    title = rule.name,
                    summary = rule.summary(labelFor),
                    enabled = rule.isEnabled
                )
            )
        }
        shortVideo?.takeIf { pkg in it.selectedAppIds }?.let { config ->
            add(
                AppliedRule(
                    kind = RuleKind.Shorts,
                    target = RuleTarget.Shorts(),
                    title = "Shorts & Reels · ${config.mode.label}",
                    summary = config.summary(),
                    enabled = config.isEnabled
                )
            )
        }
        wakeUp?.takeIf { pkg in it.selectedAppIds }?.let { config ->
            add(
                AppliedRule(
                    kind = RuleKind.MorningLock,
                    target = RuleTarget.MorningLock,
                    title = "Morning lock",
                    summary = config.summary(labelFor),
                    enabled = config.isEnabled
                )
            )
        }
    }
}
