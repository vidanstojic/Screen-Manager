package com.example.screenmanager.ui.limits

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.screenmanager.ui.components.BottomNavBar
import com.example.screenmanager.ui.components.InfoBanner
import com.example.screenmanager.ui.components.StatusChip
import com.example.screenmanager.ui.theme.DetailBackground
import com.example.screenmanager.ui.theme.PurpleAccent

/**
 * Glavni pregled limit funkcija aplikacije.
 *
 * Do ovog ekrana se dolazi iz donje navigacije i on služi kao ulaz u
 * app limits, shorts/reels, scheduled blocking, wake-up blocking i emergency.
 */
@Composable
fun UsageLimitsScreen(
    selectedDestination: MainDestination,
    onDestinationSelected: (MainDestination) -> Unit,
    onAddLimit: () -> Unit,
    onScheduledBlockClick: () -> Unit = {}
) {
    Scaffold(
        containerColor = DetailBackground,
        bottomBar = {
            BottomNavBar(
                selected = selectedDestination,
                onSelected = onDestinationSelected
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddLimit,
                containerColor = PurpleAccent,
                contentColor = Color(0xFF1F1B29)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add limit")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                onClick = onScheduledBlockClick // Povezan klik
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
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = Color(0xFF25313A))
                Text(description, fontSize = 13.sp, color = Color(0xFF66747E), modifier = Modifier.padding(top = 4.dp))
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF2EAFE)),
                contentAlignment = Alignment.Center
            ) {
                Text(">", color = PurpleAccent, fontWeight = FontWeight.Bold)
            }
        }
    }
}