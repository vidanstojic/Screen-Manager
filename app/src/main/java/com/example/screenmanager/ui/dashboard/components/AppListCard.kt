package com.example.screenmanager.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.screenmanager.model.AppUsageSummary
import com.example.screenmanager.ui.theme.GlassTheme

@Composable
fun AppListCard(
    apps: List<AppUsageSummary>,
    onAppClick: (AppUsageSummary) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(GlassTheme.colors.surface)
            .border(
                1.dp,
                GlassTheme.colors.borderGradient,
                RoundedCornerShape(18.dp)
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(GlassTheme.colors.surfaceElevated)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(GlassTheme.colors.accentPrimary),
                contentAlignment = Alignment.Center
            ) {
                Text("Apps", color = GlassTheme.colors.textPrimary, fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Categories", color = GlassTheme.colors.textSecondary, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        apps.forEachIndexed { index, app ->
            AppUsageRow(
                app = app,
                maxMinutes = apps.maxOfOrNull { it.minutes } ?: 1,
                onClick = { onAppClick(app) }
            )
            if (index < apps.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .padding(start = 52.dp)
                        .background(GlassTheme.colors.borderEnd)
                )
            }
        }
    }
}