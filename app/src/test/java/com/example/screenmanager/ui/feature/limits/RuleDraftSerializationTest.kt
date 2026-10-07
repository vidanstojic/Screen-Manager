package com.example.screenmanager.ui.feature.limits

import com.example.screenmanager.model.AppLimitRule
import com.example.screenmanager.model.ScheduleRule
import com.example.screenmanager.model.SessionLimitRule
import com.example.screenmanager.model.ShortVideoConfig
import com.example.screenmanager.model.ShortsMode
import com.example.screenmanager.model.WakeUpConfig
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Forme za pravila čuvaju radnu kopiju preko `rememberSaveable`, što radi
 * samo ako je ceo objekat (uključujući liste i skupove u njemu) Serializable.
 * Ovaj test pravi pravila na isti način kao baza i forme i proverava da
 * prežive serijalizaciju — u suprotnom bi aplikacija pala pri rotaciji ekrana.
 */
class RuleDraftSerializationTest {

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> roundTrip(value: T): T {
        val bytes = ByteArrayOutputStream().also { ObjectOutputStream(it).use { out -> out.writeObject(value) } }
        return ObjectInputStream(ByteArrayInputStream(bytes.toByteArray())).use { it.readObject() } as T
    }

    // Isti izrazi kojima entiteti iz baze prave liste i skupove.
    private fun appsFromDb(csv: String): List<String> = csv.split(",").filter { it.isNotEmpty() }
    private fun daysFromDb(csv: String): Set<DayOfWeek> =
        csv.split(",").mapNotNull { runCatching { DayOfWeek.valueOf(it) }.getOrNull() }.toSet()

    @Test
    fun `app limit drafts survive serialization`() {
        val fromDb = AppLimitRule("1", "Social", appsFromDb("a,b"), 60, 0)
        val newWithPreset = AppLimitRule("2", "New", listOfNotNull("a"), 60, 0)
        val newEmpty = AppLimitRule("3", "New", listOfNotNull<String>(null), 60, 0)
        val edited = fromDb.copy(selectedAppIds = setOf("a", "c").toList())
        listOf(fromDb, newWithPreset, newEmpty, edited).forEach { assertEquals(it, roundTrip(it)) }
    }

    @Test
    fun `session limit drafts survive serialization`() {
        val rule = SessionLimitRule("1", "IG", appsFromDb("a"), 5, 5, 15)
        assertEquals(rule, roundTrip(rule))
        assertEquals(rule.copy(selectedAppIds = emptyList()), roundTrip(rule.copy(selectedAppIds = emptyList())))
    }

    @Test
    fun `schedule drafts survive serialization for every kind of day set`() {
        val base = ScheduleRule("1", "Night", LocalTime.of(22, 0), LocalTime.of(7, 0), daysFromDb("MONDAY,FRIDAY"), appsFromDb("a,b"))
        val variants = listOf(
            base,
            base.copy(daysOfWeek = daysFromDb("")),          // prazan skup
            base.copy(daysOfWeek = daysFromDb("SUNDAY")),    // jedan dan
            base.copy(daysOfWeek = base.daysOfWeek + DayOfWeek.SUNDAY),
            base.copy(daysOfWeek = base.daysOfWeek - DayOfWeek.MONDAY),
            base.copy(daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY))
        )
        variants.forEach { assertEquals(it, roundTrip(it)) }
    }

    @Test
    fun `global rule drafts survive serialization`() {
        val shorts = ShortVideoConfig(mode = ShortsMode.SESSIONS)
        assertEquals(shorts, roundTrip(shorts))
        val toggled = shorts.copy(selectedAppIds = (shorts.selectedAppIds - ShortsApps.YOUTUBE).distinct())
        assertEquals(toggled, roundTrip(toggled))

        val morning = WakeUpConfig(selectedAppIds = appsFromDb("a,b,c"))
        assertEquals(morning, roundTrip(morning))
    }
}
