package com.example.screenmanager.utils

import java.util.Locale

fun formatHeadline(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return if (hours > 0) "$hours hours and $rest minutes" else "$rest minutes"
}

fun formatSentenceMinutes(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return if (hours > 0) "$hours hours and $rest minutes" else "$rest minutes"
}

fun formatCompactMinutes(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours > 0 && rest > 0 -> String.format(Locale.US, "%dh %dm", hours, rest)
        hours > 0 -> String.format(Locale.US, "%dh", hours)
        else -> String.format(Locale.US, "%dm", rest)
    }
}

fun formatDetailedDuration(minutes: Int): String {
    if (minutes <= 0) return "0s"
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours > 0 && rest > 0 -> "${hours}h  ${rest}m"
        hours > 0 -> "${hours}h"
        else -> "${rest}m"
    }
}