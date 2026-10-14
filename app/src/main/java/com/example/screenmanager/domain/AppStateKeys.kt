package com.example.screenmanager.domain

object AppStateKeys {
    /** Samo za prelaz sa stare heuristike: početna vrednost za [WAKE_DARK_SINCE]. */
    const val LAST_SCREEN_OFF_AT = "last_screen_off_at"
    const val WAKEUP_BLOCKED_UNTIL = "wakeup_blocked_until"

    /** Stanje detekcije buđenja (domain/wakeup/WakeUpDetector.WakeState). */
    const val WAKE_DARK_SINCE = "wake_dark_since"
    const val WAKE_SCREEN_ON_AT = "wake_screen_on_at"
    const val WAKE_CANDIDATE_AT = "wake_candidate_at"
    const val WAKE_CANDIDATE_ON_MS = "wake_candidate_on_ms"
    const val USAGE_SYNC_CURSOR = "usage_sync_cursor"

    /** 1 = primeri pravila su već upisani (jednom po instalaciji). */
    const val DEFAULT_RULES_SEEDED = "default_rules_seeded"

    /** Verzija poslednjeg uspešnog punog backfill-a istorije (vidi UsageStatsRepository). */
    const val USAGE_BACKFILL_VERSION = "usage_backfill_version"

    /** + packageName → kraj kazne za Shorts/Reels (cela aplikacija blokirana). */
    const val SHORTS_PENALTY_PREFIX = "shorts_penalty_until:"

    /** + ključ (`all` = zbir svih short-form aplikacija) → danas odgledano short-form vreme (ms). */
    const val SHORTS_WATCHED_PREFIX = "shorts_watched_ms:"

    /** + ključ → epochDay na koji se odnosi [SHORTS_WATCHED_PREFIX]. */
    const val SHORTS_WATCHED_DAY_PREFIX = "shorts_watched_day:"
}
