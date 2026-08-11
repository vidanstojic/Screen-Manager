package com.example.screenmanager.domain

import java.util.Calendar
import java.util.concurrent.TimeUnit

object TimeBuckets {
    fun startOfToday(now: Long = System.currentTimeMillis()): Long {
        return Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun startOfWeek(now: Long = System.currentTimeMillis()): Long {
        return Calendar.getInstance().apply {
            timeInMillis = now
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun startOfMonth(now: Long = System.currentTimeMillis()): Long {
        return Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /**
     * Početak "klizećeg" prozora od poslednjih 7 dana (danas uključen),
     * normalizovano na ponoć — ne prati kalendarsku nedelju kao [startOfWeek].
     *
     * Koristi se za retenciju logova i za "poslednjih 7 dana" grafike
     * (weekly daily breakdown), jer korisnik uvek treba da vidi tačno
     * 7 tačaka, bez obzira koji je dan danas.
     */
    fun startOfRollingWeek(now: Long = System.currentTimeMillis()): Long {
        val todayStart = startOfToday(now)
        return todayStart - TimeUnit.DAYS.toMillis(6)
    }

    val sevenHoursMs: Long = TimeUnit.HOURS.toMillis(7)
    val wakeupEvaluationWindowMs: Long = TimeUnit.MINUTES.toMillis(2)
    val defaultWakeupBlockMs: Long = TimeUnit.MINUTES.toMillis(60)
    val shortsLimitMs: Long = TimeUnit.MINUTES.toMillis(5)
    val shortsPenaltyMs: Long = TimeUnit.MINUTES.toMillis(30)
    val shortsGraceMs: Long = TimeUnit.SECONDS.toMillis(10)
}