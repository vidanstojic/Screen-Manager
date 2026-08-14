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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.screenmanager.ui.theme.GlassBackground
import com.example.screenmanager.ui.theme.GlassTheme

@Composable
fun FocusFlowHomeScreen(
    onAppDetoxClick: () -> Unit,
    onSmartAlarmsClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onStatsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    // Koristimo tvoj helper iz teme za centralizovanu pozadinu
    GlassBackground {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val maxWidth = this.maxWidth

            GlowOrb(
                colors = GlassTheme.colors.glowPrimary,
                size = maxWidth * 0.55f,
                alignment = Alignment.TopEnd,
                offsetX = maxWidth * 0.1f,
                offsetY = (-50).dp
            )
            GlowOrb(
                colors = GlassTheme.colors.glowSecondary,
                size = maxWidth * 0.4f,
                alignment = Alignment.CenterStart,
                offsetX = -(maxWidth * 0.15f),
                offsetY = 40.dp
            )
            GlowOrb(
                colors = GlassTheme.colors.glowTertiary,
                size = maxWidth * 0.35f,
                alignment = Alignment.BottomEnd,
                offsetX = maxWidth * 0.08f,
                offsetY = (-90).dp
            )
        }

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                FocusFlowBottomNav(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 16.dp, top = 8.dp)
                        .navigationBarsPadding(),
                    onHomeClick = onHomeClick,
                    onStatsClick = onStatsClick,
                    onSettingsClick = onSettingsClick,
                    onProfileClick = onProfileClick
                )
            },
            contentWindowInsets = WindowInsets.systemBars
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 24.dp, bottom = 24.dp)
            ) {
                HomeHeader()

                Spacer(Modifier.height(24.dp))

                ScreenTimeCard()

                Spacer(Modifier.height(16.dp))

                GlassActionCard(
                    icon = Icons.Filled.CheckCircle,
                    iconBackground = Brush.linearGradient(
                        listOf(GlassTheme.colors.success, GlassTheme.colors.success.copy(alpha = 0.6f))
                    ),
                    title = "App Detox",
                    description = "Schedule limits and block distracting apps to reclaim your focus.",
                    badgeText = "3 SESSIONS ACTIVE",
                    badgeColor = GlassTheme.colors.success,
                    onClick = onAppDetoxClick
                )

                Spacer(Modifier.height(16.dp))

                GlassActionCard(
                    icon = Icons.Filled.Notifications,
                    iconBackground = Brush.linearGradient(
                        listOf(GlassTheme.colors.warning, GlassTheme.colors.warning.copy(alpha = 0.6f))
                    ),
                    title = "Smart Alarms",
                    description = "Set intelligent wake-up calls and focus reminders with custom sounds.",
                    badgeText = "NEXT: 07:30 AM",
                    badgeColor = GlassTheme.colors.warning,
                    onClick = onSmartAlarmsClick
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Quick Insights",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GlassTheme.colors.textPrimary
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InsightGlassCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Star,
                        iconTint = GlassTheme.colors.accentPrimary,
                        label = "FOCUS SCORE",
                        value = "82%"
                    )
                    InsightGlassCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Info,
                        iconTint = GlassTheme.colors.info,
                        label = "SLEEP QUALITY",
                        value = "Good"
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.GlowOrb(
    colors: List<Color>,
    size: androidx.compose.ui.unit.Dp,
    alignment: Alignment,
    offsetX: androidx.compose.ui.unit.Dp,
    offsetY: androidx.compose.ui.unit.Dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .align(alignment)
            .offset(x = offsetX, y = offsetY)
            .blur(90.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(colors.map { it.copy(alpha = 0.45f) })
            )
    )
}

@Composable
private fun HomeHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "FocusFlow",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = GlassTheme.colors.textPrimary
            )
            Text(
                text = "Ready to focus?",
                fontSize = 14.sp,
                color = GlassTheme.colors.textSecondary
            )
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(GlassTheme.colors.accentGradient)
                .border(1.dp, GlassTheme.colors.borderStart, CircleShape)
        )
    }
}

@Composable
private fun ScreenTimeCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        GlassTheme.colors.accentSecondary.copy(alpha = 0.55f),
                        GlassTheme.colors.accentPrimary.copy(alpha = 0.35f)
                    )
                )
            )
            .border(1.dp, GlassTheme.colors.borderGradient, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = "TOTAL SCREEN TIME TODAY",
                fontSize = 11.sp,
                color = GlassTheme.colors.textPrimary.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "4h 12m",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = GlassTheme.colors.textPrimary
            )
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(GlassTheme.colors.surfaceElevated)
                    .border(1.dp, GlassTheme.colors.borderStart, RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "↘ 12% less than yesterday",
                    fontSize = 12.sp,
                    color = GlassTheme.colors.textPrimary
                )
            }
        }
    }
}

@Composable
private fun GlassActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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

        Icon(
            Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = GlassTheme.colors.textMuted
        )
    }
}

@Composable
private fun InsightGlassCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    value: String
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(GlassTheme.colors.surface)
            .border(1.dp, GlassTheme.colors.borderGradient, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Icon(icon, contentDescription = label, tint = iconTint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(8.dp))
        Text(label, fontSize = 10.sp, color = GlassTheme.colors.textSecondary)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GlassTheme.colors.textPrimary)
    }
}

@Composable
private fun FocusFlowBottomNav(
    modifier: Modifier = Modifier,
    onHomeClick: () -> Unit,
    onStatsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(GlassTheme.colors.surfaceElevated)
            .border(1.dp, GlassTheme.colors.borderGradient, RoundedCornerShape(28.dp))
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavIcon(Icons.Filled.Home, "Home", onHomeClick)
        NavIcon(Icons.Filled.List, "Stats", onStatsClick)
        NavIcon(Icons.Filled.Settings, "Settings", onSettingsClick)
        NavIcon(Icons.Filled.Person, "Profile", onProfileClick)
    }
}

@Composable
private fun NavIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Icon(icon, contentDescription = label, tint = GlassTheme.colors.textPrimary, modifier = Modifier.size(20.dp))
    }
}