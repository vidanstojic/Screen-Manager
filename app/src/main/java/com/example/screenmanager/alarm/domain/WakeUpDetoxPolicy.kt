package com.example.screenmanager.alarm.domain

import com.example.screenmanager.model.AlarmRule

/**
 * Aktivna wake-up detox blokada posle gašenja alarma.
 *
 * @property packages aplikacije koje su blokirane (sprovodi FocusMonitorService/RulesEngine).
 * @property domains sajtovi koji su blokirani u browserima (sprovodi WebsiteGuard).
 */
data class AlarmDetoxLock(
    val until: Long,
    val packages: Set<String>,
    val domains: Set<String>,
    val label: String
) {
    fun isActiveAt(now: Long): Boolean = until > now
}

/** Pravila wake-up detox-a bez Android zavisnosti. */
object WakeUpDetoxPolicy {

    /**
     * Blokada koju pali gašenje [alarm]-a, ili null ako detox nije uključen
     * ili nema šta da blokira. Postojeća blokada se ne skraćuje: kraj je
     * kasniji od dva, a liste se spajaju (dva alarma zaredom).
     */
    fun lockFor(alarm: AlarmRule, now: Long, existing: AlarmDetoxLock?): AlarmDetoxLock? {
        val detox = alarm.detox
        if (!detox.enabled || (detox.apps.isEmpty() && detox.sites.isEmpty())) return null
        val fresh = AlarmDetoxLock(
            until = now + detox.minutes.coerceIn(MIN_MINUTES, MAX_MINUTES) * 60_000L,
            packages = detox.apps.toSet(),
            domains = detox.sites.mapNotNull(SiteRules::normalize).toSet(),
            label = alarm.label.ifBlank { "Alarm" }
        )
        val current = existing?.takeIf { it.isActiveAt(now) } ?: return fresh
        return AlarmDetoxLock(
            until = maxOf(current.until, fresh.until),
            packages = current.packages + fresh.packages,
            domains = current.domains + fresh.domains,
            label = fresh.label
        )
    }

    const val MIN_MINUTES = 5
    const val MAX_MINUTES = 240
}

/**
 * Domeni za blokadu sajtova. Korisnik može da upiše bilo šta od
 * "Instagram.com" do "https://www.instagram.com/reels/" — čuva se samo domen.
 * Blokira se domen i svi njegovi poddomeni (m.youtube.com za youtube.com).
 */
object SiteRules {
    private val VALID_HOST = Regex("^[a-z0-9-]+(\\.[a-z0-9-]+)+$")

    /** "https://www.Instagram.com/reels" → "instagram.com"; neispravan unos → null. */
    fun normalize(input: String): String? {
        val host = hostOf(input) ?: return null
        return host.takeIf { VALID_HOST.matches(it) }
    }

    /**
     * Domen iz teksta adresne trake (sa ili bez šeme, putanje, porta...).
     * Vodeći "www." i "m." se uklanjaju.
     */
    fun hostOf(text: String): String? {
        var value = text.trim().lowercase()
        if (value.isEmpty() || value.contains(' ')) return null
        value = value.substringAfter("://")
        value = value.substringBefore('/').substringBefore('?').substringBefore('#').substringBefore(':')
        value = value.substringAfterLast('@')
        while (value.startsWith("www.") || value.startsWith("m.")) value = value.substringAfter('.')
        return value.takeIf { it.contains('.') }
    }

    fun isBlocked(host: String, domains: Set<String>): Boolean =
        domains.any { domain -> host == domain || host.endsWith(".$domain") }
}
