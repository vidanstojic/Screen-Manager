package com.example.screenmanager.domain.rules

/**
 * Zaključavanje limita na vreme (predlog testera; TODO-002): dok traje, postojeća
 * pravila ne mogu da se isključe, menjaju ni brišu, a emergency pauza ne može da se
 * pokrene. Uključivanje pravila i dodavanje novih je dozvoljeno — to samo pooštrava.
 *
 * Pomeranje sata unapred ne skida zaključavanje: u istom paljenju telefona važi i
 * protekli `elapsedRealtime`; posle restarta ostaje samo zidni sat.
 *
 * @property lockedAtWall `System.currentTimeMillis()` u trenutku zaključavanja
 * @property lockedAtElapsed `SystemClock.elapsedRealtime()` u trenutku zaključavanja
 */
data class LimitsLock(
    val lockedAtWall: Long,
    val lockedAtElapsed: Long,
    val durationMs: Long
) {
    /** Kraj po zidnom satu (za prikaz). */
    val endsAt: Long get() = lockedAtWall + durationMs

    fun isActive(nowWall: Long, nowElapsed: Long): Boolean {
        val sameBoot = nowElapsed >= lockedAtElapsed
        val byElapsed = sameBoot && nowElapsed - lockedAtElapsed < durationMs
        return byElapsed || nowWall < endsAt
    }

    /** Kraj za prikaz: kasniji od dva merenja (sat pomeren unapred ne skraćuje prikaz). */
    fun effectiveEnd(nowWall: Long, nowElapsed: Long): Long {
        if (nowElapsed < lockedAtElapsed) return endsAt
        val remainingByElapsed = durationMs - (nowElapsed - lockedAtElapsed)
        return maxOf(endsAt, nowWall + remainingByElapsed)
    }

    companion object {
        const val HOUR_MS = 60 * 60_000L
        const val DAY_MS = 24 * HOUR_MS

        /** Novo zaključavanje; aktivno se samo produžava, nikad ne skraćuje. */
        fun lockOrExtend(current: LimitsLock?, nowWall: Long, nowElapsed: Long, durationMs: Long): LimitsLock {
            val next = LimitsLock(nowWall, nowElapsed, durationMs)
            if (current == null || !current.isActive(nowWall, nowElapsed)) return next
            return if (next.effectiveEnd(nowWall, nowElapsed) > current.effectiveEnd(nowWall, nowElapsed)) next else current
        }

        /**
         * Da li izmena pravila prolazi: bez aktivnog zaključavanja uvek; inače samo novo
         * pravilo ili isto pravilo koje se samo uključuje ([enabledCopy] = postojeće sa isEnabled = true).
         */
        fun <T> allowsSave(locked: Boolean, existing: T?, updated: T, enabledCopy: (T) -> T): Boolean =
            !locked || existing == null || updated == enabledCopy(existing)
    }
}
