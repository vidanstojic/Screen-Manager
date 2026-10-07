package com.example.screenmanager.ui.feature.alarms

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.screenmanager.ui.theme.AppTheme
import java.util.Locale

/**
 * "Točak" za izbor broja (sati ili minuti): lista koja se zaustavlja na
 * stavci, a izabrana je ona u sredini.
 *
 * Napomena: visina (180dp), padding (65dp) i veličine slova su međusobno
 * usklađeni da srednja stavka bude tačno u centru — menjati ih zajedno.
 */
@Composable
fun WheelPicker(
    count: Int,
    initialIndex: Int,
    onValueSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    twoDigits: Boolean = true
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val currentOnValueSelected by rememberUpdatedState(onValueSelected)

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val centerItem = listState.firstVisibleItemIndex
            if (centerItem < count) currentOnValueSelected(centerItem)
        }
    }

    Box(
        modifier = modifier
            .width(80.dp)
            .height(180.dp),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 65.dp)
        ) {
            items(count) { index ->
                val isCenter = index == listState.firstVisibleItemIndex
                Text(
                    text = if (twoDigits) String.format(Locale.US, "%02d", index) else index.toString(),
                    fontSize = if (isCenter) 48.sp else 32.sp,
                    color = if (isCenter) AppTheme.colors.textPrimary else AppTheme.colors.textMuted.copy(alpha = 0.5f),
                    fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                )
            }
        }
    }
}
