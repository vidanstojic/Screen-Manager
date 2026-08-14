package com.example.screenmanager.ui.limits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.screenmanager.model.MainDestination
import com.example.screenmanager.ui.components.InfoBanner
import com.example.screenmanager.ui.components.StatusChip
import com.example.screenmanager.ui.theme.GlassBackground
import com.example.screenmanager.ui.theme.GlassTheme

/**
 * Glavni pregled limit funkcija aplikacije.
 */
@Composable
fun UsageLimitsScreen(
    selectedDestination: MainDestination,
    onDestinationSelected: (MainDestination) -> Unit,
    onAddLimit: () -> Unit,
    onScheduledBlockClick: () -> Unit = {}
) {
    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onAddLimit,
                    containerColor = GlassTheme.colors.accentPrimary,
                    contentColor = GlassTheme.colors.textPrimary,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add limit")
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                InfoBanner(
                    title = "Limit control center",
                    description = "Create app limits, shorts penalties, schedules, and wake-up blocks from one place."
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip(text = "App limits", isActive = true)
                    StatusChip(text = "Shorts/Reels", isActive = true)
                    StatusChip(text = "Schedules", isActive = true)
                }

                LimitTile(
                    title = "App limits",
                    description = "Block any app after the configured daily usage window."
                )
                LimitTile(
                    title = "Shorts and Reels",
                    description = "Add an extra penalty block after short-form content expires."
                )
                LimitTile(
                    title = "Scheduled blocking",
                    description = "Use time windows to enforce focus during work or sleep.",
                    onClick = onScheduledBlockClick
                )
                LimitTile(
                    title = "Wake-up blocking",
                    description = "Block selected apps after the phone has been inactive long enough."
                )
                LimitTile(
                    title = "Emergency sessions",
                    description = "Temporarily bypass every block when you need a safe exception."
                )
            }
        }
    }
}

/**
 * Jedna stavka na limit dashboardu koja objašnjava koju vrstu zaštite pokriva.
 */
@Composable
private fun LimitTile(
    title: String,
    description: String,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.dp, GlassTheme.colors.borderGradient, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = GlassTheme.colors.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = GlassTheme.colors.textPrimary)
                Text(
                    description,
                    fontSize = 13.sp,
                    color = GlassTheme.colors.textSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GlassTheme.colors.surfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Text(">", color = GlassTheme.colors.accentPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}