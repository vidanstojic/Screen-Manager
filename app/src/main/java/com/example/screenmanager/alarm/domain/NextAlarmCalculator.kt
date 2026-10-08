package com.example.screenmanager.alarm.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Računa sledeći trenutak zvonjave alarma. Čista funkcija (vreme i zona
 * dolaze spolja) → testira se bez uređaja.
 *
 * Pravila:
 * - bez dana ponavljanja: prvi budući trenutak `hour:minute` (danas ili sutra);
 * - sa danima: najbliži izabrani dan čije `hour:minute` je posle `now`;
 * - prelazak na letnje računanje vremena: ako `hour:minute` ne postoji tog dana
 *   (npr. 02:30 u noći kad se sat pomera 02→03), alarm zvoni u prvom
 *   postojećem trenutku posle rupe (03:30 po `ZonedDateTime.of` pravilu);
 *   pri vraćanju sata (03→02) zvoni u PRVOM od dva ista trenutka.
 *
 * Sekunde i milisekunde su uvek 0.
 */
object NextAlarmCalculator {

    fun nextTrigger(
        hour: Int,
        minute: Int,
        repeatDays: Set<DayOfWeek>,
        now: Long,
        zone: ZoneId
    ): Long {
        val nowZoned = Instant.ofEpochMilli(now).atZone(zone)
        val time = LocalTime.of(hour, minute)
        val today = nowZoned.toLocalDate()
        // 8 dana pokriva "isti dan sledeće nedelje" kad je današnji termin prošao.
        for (offset in 0..7L) {
            val date = today.plusDays(offset)
            if (repeatDays.isNotEmpty() && date.dayOfWeek !in repeatDays) continue
            val candidate = ZonedDateTime.of(date, time, zone).withEarlierOffsetAtOverlap()
            val millis = candidate.toInstant().toEpochMilli()
            if (millis > now) return millis
        }
        // Nedostižno: sa bar jednim danom u nedelji petlja uvek nađe termin.
        error("No alarm time found for $hour:$minute on $repeatDays")
    }
}
