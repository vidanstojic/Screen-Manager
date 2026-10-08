package com.example.screenmanager.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * Broj koji se pri PRVOM prikazu "odbrojava" od 0 do [target]; kasnije
 * promene vrednosti klize sa stare na novu. Stanje "već odbrojano" se pamti,
 * pa se odbrojavanje ne ponavlja pri povratku na ekran.
 */
@Composable
fun rememberCountUp(target: Long, durationMillis: Int = 900): Long {
    var played by rememberSaveable { mutableStateOf(false) }
    val animatable = remember { Animatable(if (played) target.toFloat() else 0f) }
    LaunchedEffect(target) {
        animatable.animateTo(target.toFloat(), tween(durationMillis, easing = FastOutSlowInEasing))
        played = true
    }
    return animatable.value.toLong()
}

/** Isto kao [rememberCountUp], za udeo 0..1 (npr. prsten dnevnog cilja). */
@Composable
fun rememberCountUp(target: Float, durationMillis: Int = 900): Float {
    var played by rememberSaveable { mutableStateOf(false) }
    val animatable = remember { Animatable(if (played) target else 0f) }
    LaunchedEffect(target) {
        animatable.animateTo(target, tween(durationMillis, easing = FastOutSlowInEasing))
        played = true
    }
    return animatable.value
}
