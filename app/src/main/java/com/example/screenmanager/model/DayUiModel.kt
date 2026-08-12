package com.example.screenmanager.model

/**
 * UI model za jedan dan u "poslednjih 7 dana" prozoru.
 *
 * timestamp je početak tog dana (ponoć), koristi se i za poređenje
 * (selektovan dan) i za upit ka repository-ju.
 */
data class DayUiModel(
    val timestamp: Long,
    val shortLabel: String,
    val dateLabel: String,
    val displayLabel: String
)
