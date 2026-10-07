package com.example.screenmanager.ui.feature.limits

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material.icons.rounded.Timelapse
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Vrste pravila koje aplikacija nudi — naziv, opis i ikonica na jednom
 * mestu, da se ista vrsta svuda zove i izgleda isto (Limits, Overview, izbor
 * novog pravila).
 */
enum class RuleKind(
    val title: String,
    val description: String,
    val icon: ImageVector
) {
    DailyLimit(
        title = "Daily limits",
        description = "Cap the total time per day. Apps stay blocked until midnight once it is used up.",
        icon = Icons.Rounded.HourglassBottom
    ),
    SessionLimit(
        title = "Session limits",
        description = "Allow only short sessions, with a break after each one.",
        icon = Icons.Rounded.Timelapse
    ),
    Schedule(
        title = "Schedules",
        description = "Block apps during set hours, like work or sleep.",
        icon = Icons.Rounded.CalendarMonth
    ),
    Shorts(
        title = "Shorts & Reels",
        description = "Limit only short videos. The rest of YouTube and Instagram keeps working.",
        icon = Icons.Rounded.SmartDisplay
    ),
    MorningLock(
        title = "Morning lock",
        description = "Keep distracting apps locked right after you wake up.",
        icon = Icons.Rounded.WbSunny
    )
}
