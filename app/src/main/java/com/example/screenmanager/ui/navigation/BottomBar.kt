package com.example.screenmanager.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.screenmanager.ui.components.CardDivider
import com.example.screenmanager.ui.theme.AppTheme

private data class TabItem(
    val tab: Screen.Tab,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

/** Redosled i izgled tabova u donjoj navigaciji. */
private val TabItems = listOf(
    TabItem(Screen.Home, "Home", Icons.Outlined.Home, Icons.Rounded.Home),
    TabItem(Screen.Stats, "Stats", Icons.Outlined.BarChart, Icons.Rounded.BarChart),
    TabItem(Screen.Limits, "Limits", Icons.Outlined.Shield, Icons.Rounded.Shield),
    TabItem(Screen.Alarms, "Alarms", Icons.Outlined.Alarm, Icons.Rounded.Alarm)
)

@Composable
fun AppBottomBar(
    selected: Screen.Tab,
    onSelect: (Screen.Tab) -> Unit
) {
    Column {
        CardDivider()
        NavigationBar(
            containerColor = AppTheme.colors.background,
            contentColor = AppTheme.colors.textSecondary,
            tonalElevation = 0.dp
        ) {
            TabItems.forEach { item ->
                val isSelected = item.tab == selected
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onSelect(item.tab) },
                    icon = {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.icon,
                            contentDescription = null
                        )
                    },
                    label = { Text(text = item.label, style = MaterialTheme.typography.labelMedium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AppTheme.colors.accent,
                        selectedTextColor = AppTheme.colors.textPrimary,
                        indicatorColor = AppTheme.colors.accentSoft,
                        unselectedIconColor = AppTheme.colors.textMuted,
                        unselectedTextColor = AppTheme.colors.textMuted
                    )
                )
            }
        }
    }
}
