package com.example.screenmanager.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Trenutno vreme koje se osvežava na [intervalMs] — za prikaze koji zavise
 * od "sada" (odbrojavanje, "aktivno do 14:35", "alarm za 7h").
 */
@Composable
fun rememberNow(intervalMs: Long = 30_000L): State<Long> =
    produceState(initialValue = System.currentTimeMillis(), key1 = intervalMs) {
        while (true) {
            delay(intervalMs)
            value = System.currentTimeMillis()
        }
    }

/** Isto, kao Flow — za ViewModel-e čije stanje zavisi od trenutnog vremena. */
fun tickerFlow(intervalMs: Long = 60_000L): Flow<Long> = flow {
    while (true) {
        emit(System.currentTimeMillis())
        delay(intervalMs)
    }
}
