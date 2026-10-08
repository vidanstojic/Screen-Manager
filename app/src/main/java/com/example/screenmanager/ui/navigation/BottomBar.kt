package com.example.screenmanager.ui.navigation

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.screenmanager.ui.components.FloatingBarBottomMargin
import com.example.screenmanager.ui.components.FloatingBarHeight
import com.example.screenmanager.ui.theme.AppTheme
import com.example.screenmanager.ui.theme.GlowAlpha
import com.example.screenmanager.ui.theme.Spacing
import com.example.screenmanager.ui.theme.glow

private data class TabItem(
    val tab: Screen.Tab,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

/** Redosled i izgled tabova Screen Manager-a (alarmi su zaseban deo aplikacije). */
private val TabItems = listOf(
    TabItem(Screen.Overview, "Overview", Icons.Outlined.Home, Icons.Rounded.Home),
    TabItem(Screen.Stats, "Stats", Icons.Outlined.BarChart, Icons.Rounded.BarChart),
    TabItem(Screen.Limits, "Limits", Icons.Outlined.Shield, Icons.Rounded.Shield)
)

/** Tamna, poluprovidna podloga trake — sadržaj ispod se nazire, ali ne smeta čitanju. */
private val BarFill = Color(0xFF15132A).copy(alpha = 0.82f)

/**
 * Donja navigacija Screen Manager-a: staklena "pilula" koja pluta iznad
 * sadržaja, sa akcentnim indikatorom koji klizi do izabranog taba.
 */
@Composable
fun AppBottomBar(
    selected: Screen.Tab,
    onSelect: (Screen.Tab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val view = LocalView.current
    val selectedIndex = TabItems.indexOfFirst { it.tab == selected }.coerceAtLeast(0)

    BoxWithConstraints(
        modifier = modifier
            .navigationBarsPadding()
            .padding(start = Spacing.screen, end = Spacing.screen, bottom = FloatingBarBottomMargin)
            .fillMaxWidth()
            .height(FloatingBarHeight)
            .glow(Color.Black.copy(alpha = 0.55f), blurRadius = 24.dp, offsetY = 10.dp)
            .clip(CircleShape)
            .background(BarFill)
            .border(1.dp, colors.glassBorderBrush, CircleShape)
            .padding(6.dp)
    ) {
        val itemWidth = maxWidth / TabItems.size
        val indicatorOffset by animateDpAsState(
            targetValue = itemWidth * selectedIndex,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
            label = "navIndicator"
        )
        Spacer(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(itemWidth)
                .fillMaxHeight()
                .glow(colors.accentStart.copy(alpha = GlowAlpha))
                .clip(CircleShape)
                .background(colors.accentBrush)
        )
        Row(modifier = Modifier.fillMaxSize()) {
            TabItems.forEach { item ->
                val isSelected = item.tab == selected
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) colors.onAccent else colors.textMuted,
                    label = "navItemColor"
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab
                        ) {
                            if (!isSelected) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onSelect(item.tab)
                        },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(text = item.label, style = MaterialTheme.typography.labelLarge, color = contentColor, maxLines = 1)
                }
            }
        }
    }
}
