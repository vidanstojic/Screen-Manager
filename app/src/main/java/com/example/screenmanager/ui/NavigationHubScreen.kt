package com.example.screenmanager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * "Hub" stranica - zamena za stari BottomNavBar. Stilski identična
 * FocusFlowHomeScreen-u (isti gradient, glow orbovi, glass kartice),
 * sadrži po jednu karticu za svaku bivšu navbar destinaciju.
 */
@Composable
fun NavigationHubScreen(
    onUsageStatsClick: () -> Unit,
    onUsageLimitsClick: () -> Unit,
    onGeneralUsageClick: () -> Unit,
    onGeneralSettingsClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B0A1F), Color(0xFF1A1233), Color(0xFF0E0B22))
                )
            )
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val maxWidth = this.maxWidth

            HubGlowOrb(
                colors = listOf(Color(0xFFB13BFF), Color(0xFF6C4CE0)),
                size = maxWidth * 0.55f,
                alignment = Alignment.TopEnd,
                offsetX = maxWidth * 0.1f,
                offsetY = (-50).dp
            )
            HubGlowOrb(
                colors = listOf(Color(0xFF3BC8FF), Color(0xFF4C6CE0)),
                size = maxWidth * 0.4f,
                alignment = Alignment.CenterStart,
                offsetX = -(maxWidth * 0.15f),
                offsetY = 40.dp
            )
            HubGlowOrb(
                colors = listOf(Color(0xFFFF5CA8), Color(0xFFB13BFF)),
                size = maxWidth * 0.35f,
                alignment = Alignment.BottomEnd,
                offsetX = maxWidth * 0.08f,
                offsetY = (-90).dp
            )
        }

        Scaffold(containerColor = Color.Transparent) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 24.dp, bottom = 24.dp)
            ) {
                Text(
                    text = "Menu",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "What would you like to check?",
                    fontSize = 14.sp,
                    color = Color(0xFFA9A3C4)
                )

                Spacer(Modifier.height(24.dp))

                HubActionCard(
                    icon = Icons.Filled.BarChart,
                    iconBackground = Brush.linearGradient(listOf(Color(0xFF3BC8FF), Color(0xFF4C6CE0))),
                    title = "Usage Stats",
                    description = "Pregled dnevnog i nedeljnog korišćenja aplikacija.",
                    badgeText = "DAILY OVERVIEW",
                    badgeColor = Color(0xFF3BC8FF),
                    onClick = onUsageStatsClick
                )

                Spacer(Modifier.height(16.dp))

                HubActionCard(
                    icon = Icons.Filled.Shield,
                    iconBackground = Brush.linearGradient(listOf(Color(0xFF3BFFA0), Color(0xFF1FBF7A))),
                    title = "Usage Limits",
                    description = "Postavi i upravljaj limitima za aplikacije.",
                    badgeText = "STAY ON TRACK",
                    badgeColor = Color(0xFF3BFFA0),
                    onClick = onUsageLimitsClick
                )

                Spacer(Modifier.height(16.dp))

                HubActionCard(
                    icon = Icons.Filled.Timeline,
                    iconBackground = Brush.linearGradient(listOf(Color(0xFFB13BFF), Color(0xFF6C4CE0))),
                    title = "General Usage",
                    description = "Ukupni obrasci korišćenja i trendovi.",
                    badgeText = "TRENDS & PATTERNS",
                    badgeColor = Color(0xFFB13BFF),
                    onClick = onGeneralUsageClick
                )

                Spacer(Modifier.height(16.dp))

                HubActionCard(
                    icon = Icons.Filled.Tune,
                    iconBackground = Brush.linearGradient(listOf(Color(0xFFFF9A5C), Color(0xFFFF5C8A))),
                    title = "Settings",
                    description = "Globalna podešavanja aplikacije.",
                    badgeText = "CUSTOMIZE",
                    badgeColor = Color(0xFFFF9A5C),
                    onClick = onGeneralSettingsClick
                )
            }
        }
    }
}

@Composable
private fun BoxScope.HubGlowOrb(
    colors: List<Color>,
    size: Dp,
    alignment: Alignment,
    offsetX: Dp,
    offsetY: Dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .align(alignment)
            .offset(x = offsetX, y = offsetY)
            .blur(90.dp)
            .clip(CircleShape)
            .background(Brush.radialGradient(colors.map { it.copy(alpha = 0.45f) }))
    )
}

@Composable
private fun HubActionCard(
    icon: ImageVector,
    iconBackground: Brush,
    title: String,
    description: String,
    badgeText: String,
    badgeColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(
                1.dp,
                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.05f))),
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = Color.White)
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(description, fontSize = 13.sp, color = Color(0xFFA9A3C4))
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(badgeColor.copy(alpha = 0.18f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(badgeText, fontSize = 11.sp, color = badgeColor, fontWeight = FontWeight.Medium)
            }
        }

        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF6C647F))
    }
}