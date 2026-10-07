package com.example.screenmanager.ui.feature.limits

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Koje pravilo forma ([com.example.screenmanager.ui.feature.limits.editor.RuleEditorRoute])
 * treba da otvori.
 *
 * - `ruleId == null` → novo pravilo te vrste.
 * - `presetPackage` → aplikacija koja je unapred izabrana (dolazak sa ekrana
 *   detalja aplikacije: "Limit this app").
 *
 * Shorts/Reels i Morning lock su po jedno globalno pravilo, pa nemaju `ruleId`.
 */
sealed interface RuleTarget : Parcelable {

    @Parcelize
    data class AppLimit(val ruleId: String? = null, val presetPackage: String? = null) : RuleTarget

    @Parcelize
    data class SessionLimit(val ruleId: String? = null, val presetPackage: String? = null) : RuleTarget

    @Parcelize
    data class Schedule(val ruleId: String? = null, val presetPackage: String? = null) : RuleTarget

    @Parcelize
    data class Shorts(val presetPackage: String? = null) : RuleTarget

    @Parcelize
    data object MorningLock : RuleTarget
}
