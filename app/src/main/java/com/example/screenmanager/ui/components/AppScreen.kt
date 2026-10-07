package com.example.screenmanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.screenmanager.ui.theme.AppTheme
import com.example.screenmanager.ui.theme.Spacing

/** Margine skrolujućeg sadržaja ekrana (LazyColumn `contentPadding`). */
val ScreenContentPadding = PaddingValues(
    start = Spacing.screen,
    end = Spacing.screen,
    top = Spacing.sm,
    bottom = Spacing.xl
)

/**
 * Kostur svakog ekrana: pozadina, naslovna traka, sadržaj i (opciono)
 * fiksirana donja traka sa akcijama.
 *
 * - [onBack] == null → ekran je TAB (veliki naslov; ispod njega je donja
 *   navigacija koja sama pokriva sistemsku traku).
 * - [onBack] != null → ekran je otvoren PREKO taba (strelica nazad; sam
 *   ostavlja mesto za sistemsku traku i tastaturu).
 */
@Composable
fun AppScreen(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val isTab = onBack == null
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .then(if (isTab) Modifier else Modifier.navigationBarsPadding().imePadding())
    ) {
        if (onBack == null) {
            TabTopBar(title = title, actions = actions)
        } else {
            DetailTopBar(title = title, onBack = onBack, actions = actions)
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            content = content
        )
        bottomBar?.invoke()
    }
}

@Composable
private fun TabTopBar(title: String, actions: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = Spacing.screen)
            .padding(top = Spacing.lg, bottom = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = AppTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )
    }
}

@Composable
private fun DetailTopBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = Spacing.lg)
            .padding(top = Spacing.md, bottom = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircleIconButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "Back",
            onClick = onBack
        )
        Spacer(Modifier.width(Spacing.md))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = AppTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )
    }
}

/**
 * Fiksirana traka pri dnu ekrana za glavne akcije forme (Save, Delete...).
 * Prosleđuje se kao `bottomBar` u [AppScreen].
 */
@Composable
fun BottomActionBar(content: @Composable RowScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        CardDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screen, vertical = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/** Prazan prostor na dnu liste da poslednja stavka ne ostane ispod FAB dugmeta. */
@Composable
fun FabSpacer() {
    Spacer(Modifier.height(72.dp))
}
