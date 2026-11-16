package com.example.screenmanager.ui.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.screenmanager.ui.components.AppCard
import com.example.screenmanager.ui.components.AppScreen
import com.example.screenmanager.ui.components.IconBadge
import com.example.screenmanager.ui.components.SectionHeader
import com.example.screenmanager.ui.components.screenContentPadding
import com.example.screenmanager.ui.theme.AppTheme
import com.example.screenmanager.ui.theme.Spacing

/** Jedna tema uputstva: ikonica, naslov i kratko objašnjenje. */
private class HelpTopic(val icon: ImageVector, val title: String, val text: String)

private val gettingStarted = listOf(
    HelpTopic(
        Icons.Rounded.Verified,
        "Allow the permissions",
        "Settings lists everything Focus Flow needs. Usage access measures screen time, display over other apps " +
            "shows the block screen, and the accessibility service finds Shorts and Reels. Unrestricted battery " +
            "keeps everything running in the background."
    ),
    HelpTopic(
        Icons.Rounded.Shield,
        "Xiaomi, Redmi and POCO phones",
        "These phones close apps in the background on their own. Turn on Autostart for Focus Flow, set Battery " +
            "saver to \"No restrictions\" and lock Focus Flow in recent apps. If the phone stops Focus Flow anyway, " +
            "Settings will tell you."
    )
)

private val limits = listOf(
    HelpTopic(
        Icons.Rounded.HourglassBottom,
        "Daily limits",
        "Pick one or more apps and a time per day. The apps share that time; when it runs out they show the " +
            "block screen until midnight."
    ),
    HelpTopic(
        Icons.Rounded.Timer,
        "Session limits",
        "Short sessions with breaks: for example 5 minutes at a time, at most 5 times a day, with a 15 minute " +
            "break after each session."
    ),
    HelpTopic(
        Icons.Rounded.CalendarMonth,
        "Schedules",
        "Block apps at set times, such as work hours or the night. The apps are blocked for the whole period."
    ),
    HelpTopic(
        Icons.Rounded.PlayCircle,
        "Shorts and Reels",
        "Blocks only the short video feed in YouTube and Instagram; normal videos, the feed and messages keep " +
            "working. Choose Block, a daily budget or sessions. Needs the accessibility service."
    ),
    HelpTopic(
        Icons.Rounded.WbSunny,
        "Morning lock",
        "After a night without using the phone, the chosen apps stay blocked for a while after you wake up."
    ),
    HelpTopic(
        Icons.Rounded.Block,
        "The block screen",
        "Shows why an app is blocked and when it opens again. \"Go to home screen\" closes the blocked app."
    )
)

private val control = listOf(
    HelpTopic(
        Icons.Rounded.Lock,
        "Lock limits",
        "Lock all limits at the bottom of Limits, or one limit with the lock button on its own screen, for as many " +
            "days and hours as you choose. Until then a locked limit can't be turned off, edited or deleted, and the " +
            "emergency pause is off. You can still add new limits."
    ),
    HelpTopic(
        Icons.Rounded.PauseCircle,
        "Emergency pause",
        "Pauses every limit for a few minutes when you really need your phone. It ends on its own."
    )
)

private val alarms = listOf(
    HelpTopic(
        Icons.Rounded.Alarm,
        "Smart alarms",
        "An alarm can ask for a mission before it turns off: solve math problems, type a sentence or walk a " +
            "number of steps. A wake-up check later asks you to confirm you are still awake."
    ),
    HelpTopic(
        Icons.Rounded.Shield,
        "Wake-up detox",
        "After an alarm, chosen apps and websites can stay blocked for a while, so the morning starts without " +
            "scrolling."
    )
)

/** "How Focus Flow works": kratko uputstvo za sve funkcije (predlog testera). Otvara se iz Settings. */
@Composable
fun HelpScreen(onBack: () -> Unit) {
    AppScreen(title = "How Focus Flow works", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(screenContentPadding()),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            HelpSection("Getting started", gettingStarted)
            HelpSection("Limits", limits)
            HelpSection("Staying in control", control)
            HelpSection("Alarms", alarms)
            Text(
                text = "All of this runs on your phone. Focus Flow has no account and sends nothing anywhere.",
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.textMuted,
                modifier = Modifier.padding(top = Spacing.md, start = Spacing.xs, end = Spacing.xs)
            )
        }
    }
}

@Composable
private fun HelpSection(title: String, topics: List<HelpTopic>) {
    SectionHeader(title = title, modifier = Modifier.padding(top = Spacing.md))
    topics.forEach { topic ->
        AppCard {
            Row(verticalAlignment = Alignment.Top) {
                IconBadge(icon = topic.icon)
                Spacer(Modifier.width(Spacing.md))
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(topic.title, style = MaterialTheme.typography.titleSmall, color = AppTheme.colors.textPrimary)
                    Text(topic.text, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textSecondary)
                }
            }
        }
    }
}
