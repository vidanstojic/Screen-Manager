package com.example.screenmanager.ui.feature.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.screenmanager.ui.AppViewModel
import com.example.screenmanager.ui.common.OnResume
import com.example.screenmanager.ui.common.formatDuration
import com.example.screenmanager.ui.components.AppCard
import com.example.screenmanager.ui.components.AppScreen
import com.example.screenmanager.ui.components.AppUsageRow
import com.example.screenmanager.ui.components.BarChart
import com.example.screenmanager.ui.components.CardDivider
import com.example.screenmanager.ui.components.DayStrip
import com.example.screenmanager.ui.components.EmptyState
import com.example.screenmanager.ui.components.PrimaryButton
import com.example.screenmanager.ui.components.ScreenContentPadding
import com.example.screenmanager.ui.components.SectionHeader
import com.example.screenmanager.ui.components.SegmentedControl
import com.example.screenmanager.ui.model.AppUsageItem
import com.example.screenmanager.ui.model.UsageRange
import com.example.screenmanager.ui.theme.AppTheme
import com.example.screenmanager.ui.theme.Spacing

/** Stateful ulaz u Stats tab. */
@Composable
fun StatsRoute(
    onBack: () -> Unit,
    onOpenApp: (AppUsageItem) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: StatsViewModel = viewModel(),
    appViewModel: AppViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val permissions by appViewModel.permissionState.collectAsState()
    OnResume(viewModel::onResume)

    StatsScreen(
        state = state,
        hasUsageAccess = permissions.hasUsageAccess,
        onBack = onBack,
        onRangeSelected = viewModel::selectRange,
        onDaySelected = viewModel::selectDay,
        onOpenApp = onOpenApp,
        onOpenSettings = onOpenSettings
    )
}

/**
 * Stats tab: izbor perioda (dan / nedelja / mesec), grafik i lista
 * aplikacija za taj period.
 */
@Composable
fun StatsScreen(
    state: StatsUiState,
    hasUsageAccess: Boolean,
    onBack: () -> Unit,
    onRangeSelected: (UsageRange) -> Unit,
    onDaySelected: (Long) -> Unit,
    onOpenApp: (AppUsageItem) -> Unit,
    onOpenSettings: () -> Unit
) {
    AppScreen(title = "Screen time", onBack = onBack, largeTitle = true, hasBottomNav = true) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = ScreenContentPadding,
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            item(key = "range") {
                SegmentedControl(
                    options = UsageRange.entries,
                    selected = state.range,
                    onSelected = onRangeSelected,
                    label = { it.label }
                )
            }
            if (state.range == UsageRange.Day) {
                item(key = "days") {
                    DayStrip(
                        days = state.days,
                        selectedDayStart = state.selectedDayStart,
                        onDaySelected = onDaySelected,
                        modifier = Modifier.animateItem()
                    )
                }
            }
            item(key = "chart") {
                UsageChartCard(state = state, modifier = Modifier.animateItem())
            }
            item(key = "apps") {
                AppListSection(
                    apps = state.apps,
                    hasUsageAccess = hasUsageAccess,
                    onOpenApp = onOpenApp,
                    onOpenSettings = onOpenSettings,
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

@Composable
private fun AppListSection(
    apps: List<AppUsageItem>,
    hasUsageAccess: Boolean,
    onOpenApp: (AppUsageItem) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SectionHeader(title = "Apps")
        AppCard(contentPadding = PaddingValues(0.dp)) {
            when {
                !hasUsageAccess -> EmptyState(
                    icon = Icons.Rounded.Lock,
                    title = "Usage access needed",
                    message = "Allow usage access so Focus Flow can measure your screen time.",
                    action = { PrimaryButton(text = "Open settings", onClick = onOpenSettings) }
                )

                apps.isEmpty() -> EmptyState(
                    icon = Icons.Rounded.BarChart,
                    title = "Nothing recorded",
                    message = "No app usage was recorded for this period."
                )

                else -> {
                    val maxDuration = apps.first().durationMs
                    apps.forEachIndexed { index, app ->
                        if (index > 0) CardDivider()
                        AppUsageRow(app = app, maxDurationMs = maxDuration, onClick = { onOpenApp(app) })
                    }
                }
            }
        }
    }
}

/** Ukupno vreme perioda + stubičasti grafik; dodir stubića prikazuje njegovu vrednost. */
@Composable
private fun UsageChartCard(state: StatsUiState, modifier: Modifier = Modifier) {
    // Izbor stubića važi samo za trenutno prikazan period.
    var selectedBar by rememberSaveable(state.range, state.selectedDayStart) { mutableStateOf<Int?>(null) }
    val selectedIndex = selectedBar?.takeIf { it in state.chartValuesMs.indices }

    AppCard(modifier = modifier) {
        Text(
            text = state.periodTitle.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = AppTheme.colors.textMuted
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = formatDuration(state.totalMs),
            style = MaterialTheme.typography.displaySmall,
            color = AppTheme.colors.textPrimary
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = when {
                selectedIndex != null ->
                    "${state.chartSlotTitles[selectedIndex]} · ${formatDuration(state.chartValuesMs[selectedIndex])}"
                state.dailyAverageMs != null -> "Daily average ${formatDuration(state.dailyAverageMs)}"
                else -> "Tap a bar to see that hour"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (selectedIndex != null) AppTheme.colors.accent else AppTheme.colors.textSecondary
        )
        Spacer(Modifier.height(Spacing.lg))
        BarChart(
            valuesMs = state.chartValuesMs,
            slotLabels = state.chartSlotLabels,
            selectedIndex = selectedIndex,
            onSelect = { selectedBar = it },
            averageMs = state.dailyAverageMs
        )
    }
}
