package com.example.screenmanager.ui.feature.appdetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.screenmanager.ui.common.formatDuration
import com.example.screenmanager.ui.components.AppCard
import com.example.screenmanager.ui.components.AppIcon
import com.example.screenmanager.ui.components.AppScreen
import com.example.screenmanager.ui.components.BarChart
import com.example.screenmanager.ui.components.BottomActionBar
import com.example.screenmanager.ui.components.CardDivider
import com.example.screenmanager.ui.components.ChevronIcon
import com.example.screenmanager.ui.components.DayStrip
import com.example.screenmanager.ui.components.IconBadge
import com.example.screenmanager.ui.components.ListRow
import com.example.screenmanager.ui.components.PrimaryButton
import com.example.screenmanager.ui.components.ScreenContentPadding
import com.example.screenmanager.ui.components.SectionHeader
import com.example.screenmanager.ui.components.SegmentedControl
import com.example.screenmanager.ui.feature.limits.NewLimitSheet
import com.example.screenmanager.ui.feature.limits.RuleTarget
import com.example.screenmanager.ui.model.UsageRange
import com.example.screenmanager.ui.theme.AppTheme
import com.example.screenmanager.ui.theme.Spacing

/** Stateful ulaz u ekran detalja aplikacije. */
@Composable
fun AppDetailsRoute(
    packageName: String,
    appName: String,
    onBack: () -> Unit,
    onEditRule: (RuleTarget) -> Unit,
    viewModel: AppDetailsViewModel = viewModel()
) {
    LaunchedEffect(packageName) { viewModel.selectPackage(packageName) }
    val state by viewModel.uiState.collectAsState()

    AppDetailsScreen(
        packageName = packageName,
        appName = appName,
        // ViewModel je deljen: dok ne stignu podaci za OVU aplikaciju, prikaži prazno stanje.
        state = if (state.packageName == packageName) state else AppDetailsUiState(packageName = packageName),
        onDaySelected = viewModel::selectDay,
        onBack = onBack,
        onEditRule = onEditRule
    )
}

/**
 * Detalji jedne aplikacije: potrošnja po satu (izabrani dan) ili po danu
 * (7 dana), kratka statistika i pravila koja važe za tu aplikaciju.
 */
@Composable
fun AppDetailsScreen(
    packageName: String,
    appName: String,
    state: AppDetailsUiState,
    onDaySelected: (Long) -> Unit,
    onBack: () -> Unit,
    onEditRule: (RuleTarget) -> Unit
) {
    var range by rememberSaveable { mutableStateOf(UsageRange.Day) }
    var showNewLimitSheet by rememberSaveable { mutableStateOf(false) }

    AppScreen(
        title = appName,
        onBack = onBack,
        bottomBar = {
            BottomActionBar {
                PrimaryButton(
                    text = "Limit this app",
                    icon = Icons.Rounded.Add,
                    onClick = { showNewLimitSheet = true },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = ScreenContentPadding,
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            item(key = "header") { AppHeader(packageName = packageName, appName = appName) }
            item(key = "range") {
                SegmentedControl(
                    options = listOf(UsageRange.Day, UsageRange.Week),
                    selected = range,
                    onSelected = { range = it },
                    label = { it.label }
                )
            }
            if (range == UsageRange.Day) {
                item(key = "days") {
                    DayStrip(
                        days = state.days,
                        selectedDayStart = state.selectedDayStart,
                        onDaySelected = onDaySelected,
                        modifier = Modifier.animateItem()
                    )
                }
            }
            item(key = "stats") { StatTiles(state = state, modifier = Modifier.animateItem()) }
            item(key = "chart") { AppUsageChart(state = state, range = range, modifier = Modifier.animateItem()) }
            item(key = "rules") {
                AppliedRulesSection(
                    rules = state.rules,
                    onEditRule = onEditRule,
                    modifier = Modifier.animateItem()
                )
            }
        }
    }

    if (showNewLimitSheet) {
        NewLimitSheet(
            packageName = packageName,
            appName = appName,
            onDismiss = { showNewLimitSheet = false },
            onPick = { target ->
                showNewLimitSheet = false
                onEditRule(target)
            }
        )
    }
}

@Composable
private fun AppHeader(packageName: String, appName: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AppIcon(packageName = packageName, label = appName, size = 56.dp)
        Spacer(Modifier.width(Spacing.lg))
        Column {
            Text(
                text = appName,
                style = MaterialTheme.typography.headlineSmall,
                color = AppTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = packageName,
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Tri broja: vreme izabranog dana, broj otvaranja tog dana, 7-dnevni prosek. */
@Composable
private fun StatTiles(state: AppDetailsUiState, modifier: Modifier = Modifier) {
    val selectedDay = state.days.firstOrNull { it.dayStart == state.selectedDayStart }
    val dayLabel = when {
        selectedDay == null || selectedDay.isToday -> "Today"
        else -> "${selectedDay.weekday} ${selectedDay.dayOfMonth}"
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        StatTile(label = dayLabel, value = formatDuration(state.hourlyMs.sum()), modifier = Modifier.weight(1f))
        StatTile(label = "Opened", value = "${state.sessionCount}×", modifier = Modifier.weight(1f))
        StatTile(label = "7-day avg", value = formatDuration(state.dailyMs.sum() / 7), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier, contentPadding = PaddingValues(horizontal = Spacing.md, vertical = 14.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = AppTheme.colors.textMuted,
            maxLines = 1
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = AppTheme.colors.textPrimary,
            maxLines = 1
        )
    }
}

@Composable
private fun AppUsageChart(state: AppDetailsUiState, range: UsageRange, modifier: Modifier = Modifier) {
    val isDay = range == UsageRange.Day
    val values = if (isDay) state.hourlyMs else state.dailyMs
    var selectedBar by rememberSaveable(range, state.selectedDayStart) { mutableStateOf<Int?>(null) }
    val selectedIndex = selectedBar?.takeIf { it in values.indices }

    AppCard(modifier = modifier) {
        Text(
            text = if (isDay) "BY HOUR" else "LAST 7 DAYS",
            style = MaterialTheme.typography.labelSmall,
            color = AppTheme.colors.textMuted
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = when {
                selectedIndex == null -> formatDuration(values.sum())
                isDay -> "%02d:00 · %s".format(selectedIndex, formatDuration(values[selectedIndex]))
                else -> {
                    val weekday = state.days.getOrNull(selectedIndex)?.weekday.orEmpty()
                    "$weekday · ${formatDuration(values[selectedIndex])}"
                }
            },
            style = MaterialTheme.typography.titleLarge,
            color = if (selectedIndex != null) AppTheme.colors.accent else AppTheme.colors.textPrimary
        )
        Spacer(Modifier.height(Spacing.lg))
        BarChart(
            valuesMs = values,
            slotLabels = if (isDay) {
                List(24) { hour -> if (hour % 6 == 0) "%02d".format(hour) else null }
            } else {
                state.days.map { it.weekday }
            },
            selectedIndex = selectedIndex,
            onSelect = { selectedBar = it },
            averageMs = if (isDay) null else values.sum() / 7
        )
    }
}

@Composable
private fun AppliedRulesSection(
    rules: List<AppliedRule>,
    onEditRule: (RuleTarget) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SectionHeader(title = "Limits")
        AppCard(contentPadding = PaddingValues(0.dp)) {
            if (rules.isEmpty()) {
                Text(
                    text = "No limits apply to this app yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTheme.colors.textSecondary,
                    modifier = Modifier.padding(Spacing.lg)
                )
            } else {
                rules.forEachIndexed { index, rule ->
                    if (index > 0) CardDivider()
                    ListRow(
                        title = if (rule.enabled) rule.title else "${rule.title} (off)",
                        subtitle = rule.summary,
                        onClick = { onEditRule(rule.target) },
                        leading = {
                            IconBadge(
                                icon = rule.kind.icon,
                                tint = if (rule.enabled) AppTheme.colors.accent else AppTheme.colors.textMuted
                            )
                        },
                        trailing = { ChevronIcon() }
                    )
                }
            }
        }
    }
}
