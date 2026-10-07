package com.example.screenmanager.ui.feature.limits

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.screenmanager.ui.components.ChevronIcon
import com.example.screenmanager.ui.components.IconBadge
import com.example.screenmanager.ui.components.ListRow
import com.example.screenmanager.ui.theme.AppTheme
import com.example.screenmanager.ui.theme.Spacing

/**
 * Donji list "kako želiš da ograničiš ovu aplikaciju?" — nudi vrste pravila
 * koje imaju smisla za [packageName] i vraća odgovarajući [RuleTarget].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewLimitSheet(
    packageName: String,
    appName: String,
    onDismiss: () -> Unit,
    onPick: (RuleTarget) -> Unit
) {
    val options = buildList {
        add(RuleKind.DailyLimit to RuleTarget.AppLimit(presetPackage = packageName))
        add(RuleKind.SessionLimit to RuleTarget.SessionLimit(presetPackage = packageName))
        add(RuleKind.Schedule to RuleTarget.Schedule(presetPackage = packageName))
        if (packageName in ShortsApps.supported) {
            add(RuleKind.Shorts to RuleTarget.Shorts(presetPackage = packageName))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppTheme.colors.surface,
        contentColor = AppTheme.colors.textPrimary
    ) {
        Column(modifier = Modifier.navigationBarsPadding().padding(bottom = Spacing.lg)) {
            Text(
                text = "Limit $appName",
                style = MaterialTheme.typography.titleLarge,
                color = AppTheme.colors.textPrimary,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)
            )
            options.forEach { (kind, target) ->
                ListRow(
                    title = kind.title,
                    subtitle = kind.description,
                    onClick = { onPick(target) },
                    leading = { IconBadge(icon = kind.icon) },
                    trailing = { ChevronIcon() }
                )
            }
        }
    }
}
