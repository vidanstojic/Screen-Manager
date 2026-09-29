package com.example.screenmanager.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Jutarnja blokada (TRS 2.5).
 *
 * Heuristika: ekran je bio ugašen najmanje [inactivityHours], korisnik je
 * posle paljenja aktivan duže od [triggerDelayMinutes] → aplikacije iz
 * [selectedAppIds] su blokirane [blockDurationMinutes] minuta.
 * Isti lockout može da okine i Smart Alarm (vidi WakeUpLockoutTrigger).
 */
data class WakeUpConfig(
    val inactivityHours: Int = 7,
    val triggerDelayMinutes: Int = 2,
    val blockDurationMinutes: Int = 30,
    val selectedAppIds: List<String> = emptyList(),
    val isEnabled: Boolean = true
)

/**
 * Kako se tretira short-form sadržaj (Shorts/Reels) — ostatak aplikacije
 * (obični YouTube video, Instagram poruke/feed) radi normalno u svim modovima.
 */
enum class ShortsMode {
    /** Shorts/Reels su potpuno zabranjeni: svaki ulazak odmah dobija BACK. */
    BLOCKED,

    /** Dnevni budžet; po isteku kazna za celu aplikaciju + Shorts zaključan do kraja dana. */
    BUDGET,

    /** Interval mod samo za Shorts/Reels: M min po sesiji, N sesija dnevno, K min pauze. */
    SESSIONS
}

/**
 * Shorts/Reels pravilo (TRS 2.3). Jedno globalno pravilo koje važi za svaku
 * aplikaciju iz [selectedAppIds]; stanje (budžet, sesije) se vodi PO APLIKACIJI.
 *
 * - [ShortsMode.BUDGET]: [maxReelsWatchMinutes] je dnevni budžet po aplikaciji;
 *   posle njega cela aplikacija je blokirana [fullAppBlockMinutes] minuta, a
 *   Shorts/Reels do kraja dana.
 * - [ShortsMode.SESSIONS]: [sessionLengthMinutes] / [maxSessions] / [cooldownMinutes]
 *   (ista semantika kao [SessionLimitRule], ali meri se samo vreme u Shorts/Reels).
 * - [ShortsMode.BLOCKED]: bez dozvoljenog vremena.
 */
data class ShortVideoConfig(
    val maxReelsWatchMinutes: Int = 15,
    val fullAppBlockMinutes: Int = 60,
    val selectedAppIds: List<String> = listOf("com.instagram.android", "com.google.android.youtube"),
    val isEnabled: Boolean = true,
    val mode: ShortsMode = ShortsMode.BUDGET,
    val sessionLengthMinutes: Int = 5,
    val maxSessions: Int = 3,
    val cooldownMinutes: Int = 30
)

data class ScheduleRule(
    val id: String,
    val name: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val daysOfWeek: Set<DayOfWeek>,
    val selectedAppIds: List<String> = emptyList(),
    val isEnabled: Boolean = true
)

/**
 * Dnevni limit za GRUPU aplikacija: zbir potrošnje svih [selectedAppIds]
 * se poredi sa [dailyLimitMinutes]. Po prekoračenju grupa je blokirana do
 * ponoći. [blockDurationMinutes] je zadržan radi kompatibilnosti UI-a, ali
 * ga engine trenutno ne koristi (vidi izveštaj refaktora).
 */
data class AppLimitRule(
    val id: String,
    val name: String,
    val selectedAppIds: List<String>,
    val dailyLimitMinutes: Int,
    val blockDurationMinutes: Int,
    val isEnabled: Boolean = true,
    val description: String = ""
)

/**
 * Interval mod (TRS 2.4) za grupu aplikacija.
 *
 * - [sessionLengthMinutes] (M): maksimalno aktivno vreme jedne sesije.
 * - [maxSessions] (N): broj sesija dnevno; posle N-te sesije grupa je
 *   zaključana do ponoći (hard lockout).
 * - [cooldownMinutes] (K): obavezna pauza posle svake završene sesije
 *   (istekla M ili korisnik napustio aplikaciju).
 */
data class SessionLimitRule(
    val id: String,
    val name: String,
    val selectedAppIds: List<String>,
    val sessionLengthMinutes: Int,
    val maxSessions: Int,
    val cooldownMinutes: Int,
    val isEnabled: Boolean = true
)

/**
 * Emergency sesija — privremeno gasi SVA pravila.
 *
 * Aktivnost se izvodi iz apsolutnog trenutka [activeUntilMillis], pa istek
 * radi ispravno i preko ponoći (ranije se poredila "HH:mm" labela).
 */
data class EmergencySessionConfig(
    val defaultDurationMinutes: Int = 15,
    val activeUntilMillis: Long? = null,
    val manualEndEnabled: Boolean = true
) {
    fun isActiveAt(now: Long): Boolean = activeUntilMillis != null && activeUntilMillis > now

    /** Pogodno za UI; engine uvek koristi [isActiveAt] sa eksplicitnim `now`. */
    val isActive: Boolean
        get() = isActiveAt(System.currentTimeMillis())

    val activeUntilLabel: String?
        get() = activeUntilMillis
            ?.takeIf { isActive }
            ?.let { LABEL_FORMAT.format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())) }

    fun activated(now: Long, durationMinutes: Int = defaultDurationMinutes): EmergencySessionConfig =
        copy(activeUntilMillis = now + durationMinutes * 60_000L)

    fun ended(): EmergencySessionConfig = copy(activeUntilMillis = null)

    private companion object {
        val LABEL_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}

data class AppOption(
    val id: String,
    val name: String,
    val category: String,
    val icon: String
)
