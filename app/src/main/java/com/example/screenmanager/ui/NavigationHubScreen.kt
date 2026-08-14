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
import com.example.screenmanager.ui.theme.GlassBackground
import com.example.screenmanager.ui.theme.GlassTheme

@Composable
fun NavigationHubScreen(
    onUsageStatsClick: () -> Unit,
    onUsageLimitsClick: () -> Unit,
    onGeneralUsageClick: () -> Unit,
    onGeneralSettingsClick: () -> Unit
) {
    GlassBackground {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val maxWidth = this.maxWidth

            HubGlowOrb(
                colors = GlassTheme.colors.glowPrimary,
                size = maxWidth * 0.55f,
                alignment = Alignment.TopEnd,
                offsetX = maxWidth * 0.1f,
                offsetY = (-50).dp
            )
            HubGlowOrb(
                colors = GlassTheme.colors.glowSecondary,
                size = maxWidth * 0.4f,
                alignment = Alignment.CenterStart,
                offsetX = -(maxWidth * 0.15f),
                offsetY = 40.dp
            )
            HubGlowOrb(
                colors = GlassTheme.colors.glowTertiary,
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
                    color = GlassTheme.colors.textPrimary
                )
                Text(
                    text = "What would you like to check?",
                    fontSize = 14.sp,
                    color = GlassTheme.colors.textSecondary
                )

                Spacer(Modifier.height(24.dp))

                HubActionCard(
                    icon = Icons.Filled.BarChart,
                    iconBackground = Brush.linearGradient(
                        listOf(GlassTheme.colors.info, GlassTheme.colors.info.copy(alpha = 0.6f))
                    ),
                    title = "Usage Stats",
                    description = "Pregled dnevnog i nedeljnog korišćenja aplikacija.",
                    badgeText = "DAILY OVERVIEW",
                    badgeColor = GlassTheme.colors.info,
                    onClick = onUsageStatsClick
                )

                Spacer(Modifier.height(16.dp))

                HubActionCard(
                    icon = Icons.Filled.Shield,
                    iconBackground = Brush.linearGradient(
                        listOf(GlassTheme.colors.success, GlassTheme.colors.success.copy(alpha = 0.6f))
                    ),
                    title = "Usage Limits",
                    description = "Postavi i upravljaj limitima za aplikacije.",
                    badgeText = "STAY ON TRACK",
                    badgeColor = GlassTheme.colors.success,
                    onClick = onUsageLimitsClick
                )

                Spacer(Modifier.height(16.dp))

                HubActionCard(
                    icon = Icons.Filled.Timeline,
                    iconBackground = GlassTheme.colors.accentGradient,
                    title = "General Usage",
                    description = "Ukupni obrasci korišćenja i trendovi.",
                    badgeText = "TRENDS & PATTERNS",
                    badgeColor = GlassTheme.colors.accentPrimary,
                    onClick = onGeneralUsageClick
                )

                Spacer(Modifier.height(16.dp))

                HubActionCard(
                    icon = Icons.Filled.Tune,
                    iconBackground = Brush.linearGradient(
                        listOf(GlassTheme.colors.warning, GlassTheme.colors.warning.copy(alpha = 0.6f))
                    ),
                    title = "Settings",
                    description = "Globalna podešavanja aplikacije.",
                    badgeText = "CUSTOMIZE",
                    badgeColor = GlassTheme.colors.warning,
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
            .background(GlassTheme.colors.surface)
            .border(1.dp, GlassTheme.colors.borderGradient, RoundedCornerShape(20.dp))
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
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = GlassTheme.colors.textPrimary)
            Spacer(Modifier.height(4.dp))
            Text(description, fontSize = 13.sp, color = GlassTheme.colors.textSecondary)
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

        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = GlassTheme.colors.textMuted)
    }
}