package com.example.screenmanager.domain.rules

import com.example.screenmanager.model.ScheduleRule
import java.time.Instant
import java.time.ZoneId

/**
 * Vremenski prozori zakazanih blokada.
 *
 * Prozor koji prelazi ponoć (npr. 22:00–07:00, PON–ČET) pripada danu u kom
 * je POČEO: petak 02:00 je aktivan jer je prozor počeo u četvrtak, dok
 * ponedeljak 02:00 nije aktivan jer nedelja nije u listi.
 * Stari BlockingEngine je proveravao dan tekućeg trenutka, što je bilo
 * pogrešno za noćne prozore.
 */
object ScheduleWindows {

    /** Vraća kraj aktivnog prozora (epoch ms) ili null ako pravilo nije aktivno. */
    fun activeUntil(rule: ScheduleRule, now: Long, zone: ZoneId): Long? {
        if (!rule.isEnabled || rule.startTime == rule.endTime) return null
        val local = Instant.ofEpochMilli(now).atZone(zone)
        val today = local.toLocalDate()
        val time = local.toLocalTime()

        return if (rule.startTime.isBefore(rule.endTime)) {
            val inside = !time.isBefore(rule.startTime) && time.isBefore(rule.endTime)
            if (inside && today.dayOfWeek in rule.daysOfWeek) {
                today.atTime(rule.endTime).atZone(zone).toInstant().toEpochMilli()
            } else {
                null
            }
        } else {
            when {
                // Deo posle starta, prozor je počeo danas.
                !time.isBefore(rule.startTime) && today.dayOfWeek in rule.daysOfWeek ->
                    today.plusDays(1).atTime(rule.endTime).atZone(zone).toInstant().toEpochMilli()
                // Deo posle ponoći, prozor je počeo juče.
                time.isBefore(rule.endTime) && today.minusDays(1).dayOfWeek in rule.daysOfWeek ->
                    today.atTime(rule.endTime).atZone(zone).toInstant().toEpochMilli()
                else -> null
            }
        }
    }
}
