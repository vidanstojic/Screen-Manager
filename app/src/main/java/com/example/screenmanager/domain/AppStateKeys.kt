package com.example.screenmanager.domain

object AppStateKeys {
    const val LAST_SCREEN_OFF_AT = "last_screen_off_at"
    const val LAST_SCREEN_ON_AT = "last_screen_on_at"
    const val WAKEUP_BLOCKED_UNTIL = "wakeup_blocked_until"
    const val LAST_USER_INTERACTION_AT = "last_user_interaction_at"
    const val LAST_USAGE_EVENT_SYNC_AT = "last_usage_event_sync_at"
    const val USAGE_SYNC_CURSOR = "usage_sync_cursor"

    /** Verzija poslednjeg uspešnog punog backfill-a istorije (vidi UsageStatsRepository). */
    const val USAGE_BACKFILL_VERSION = "usage_backfill_version"

    /** + packageName → kraj kazne za Shorts/Reels (cela aplikacija blokirana). */
    const val SHORTS_PENALTY_PREFIX = "shorts_penalty_until:"

    /** + packageName → danas odgledano short-form vreme (ms). */
    const val SHORTS_WATCHED_PREFIX = "shorts_watched_ms:"

    /** + packageName → epochDay na koji se odnosi [SHORTS_WATCHED_PREFIX]. */
    const val SHORTS_WATCHED_DAY_PREFIX = "shorts_watched_day:"
}
