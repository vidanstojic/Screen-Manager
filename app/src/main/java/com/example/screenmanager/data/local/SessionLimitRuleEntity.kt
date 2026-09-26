package com.example.screenmanager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.screenmanager.domain.rules.SessionState
import com.example.screenmanager.model.SessionLimitRule

/** Konfiguracija interval moda (M/N/K) — TRS 2.4. */
@Entity(tableName = "session_limit_rules")
data class SessionLimitRuleEntity(
    @PrimaryKey val id: String,
    val name: String,
    val selectedAppIds: String, // comma-separated package names
    val sessionLengthMinutes: Int,
    val maxSessions: Int,
    val cooldownMinutes: Int,
    val isEnabled: Boolean
)

fun SessionLimitRuleEntity.toModel() = SessionLimitRule(
    id = id,
    name = name,
    selectedAppIds = selectedAppIds.split(",").filter { it.isNotEmpty() },
    sessionLengthMinutes = sessionLengthMinutes,
    maxSessions = maxSessions,
    cooldownMinutes = cooldownMinutes,
    isEnabled = isEnabled
)

fun SessionLimitRule.toEntity() = SessionLimitRuleEntity(
    id = id,
    name = name,
    selectedAppIds = selectedAppIds.joinToString(","),
    sessionLengthMinutes = sessionLengthMinutes,
    maxSessions = maxSessions,
    cooldownMinutes = cooldownMinutes,
    isEnabled = isEnabled
)

/** Runtime stanje interval moda, preživljava restart servisa/procesa. */
@Entity(tableName = "session_limit_state")
data class SessionLimitStateEntity(
    @PrimaryKey val ruleId: String,
    val epochDay: Long,
    val sessionsUsed: Int,
    val sessionStartedAt: Long?,
    val activeMs: Long,
    val lastSeenAt: Long?,
    val frozenUntil: Long
)

fun SessionLimitStateEntity.toDomain() = SessionState(
    ruleId = ruleId,
    epochDay = epochDay,
    sessionsUsed = sessionsUsed,
    sessionStartedAt = sessionStartedAt,
    activeMs = activeMs,
    lastSeenAt = lastSeenAt,
    frozenUntil = frozenUntil
)

fun SessionState.toEntity() = SessionLimitStateEntity(
    ruleId = ruleId,
    epochDay = epochDay,
    sessionsUsed = sessionsUsed,
    sessionStartedAt = sessionStartedAt,
    activeMs = activeMs,
    lastSeenAt = lastSeenAt,
    frozenUntil = frozenUntil
)
