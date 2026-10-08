package com.example.screenmanager.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.screenmanager.ui.theme.AppTheme
import com.example.screenmanager.ui.theme.Spacing

/**
 * Koliko mesta na dnu sadržaja zauzima plutajuća donja traka (0 kad je nema).
 * Postavlja ga [AppScreen] sa `hasBottomNav = true`.
 */
val LocalBottomBarClearance = compositionLocalOf { 0.dp }

/** Visina plutajuće donje trake i njen razmak od dna ekrana (ui/navigation/BottomBar.kt). */
val FloatingBarHeight = 64.dp
val FloatingBarBottomMargin = 16.dp

/**
 * Margine skrolujućeg sadržaja ekrana (LazyColumn `contentPadding`). Na
 * tabovima dno sadržaja može da se odskroluje iznad plutajuće trake.
 */
@Composable
fun screenContentPadding(): PaddingValues = PaddingValues(
    start = Spacing.screen,
    end = Spacing.screen,
    top = Spacing.sm,
    bottom = Spacing.xl + LocalBottomBarClearance.current
)

/**
 * Kostur svakog ekrana: naslovna traka, sadržaj i (opciono) fiksirana donja
 * traka sa akcijama. Ekran je PROVIDAN — aurora pozadinu crta koren UI-a.
 *
 * @param onBack ako nije null, u naslovu je strelica nazad.
 * @param largeTitle veliki naslov — za "glavne" ekrane (početni ekran, tabovi,
 *        ulaz u modul); mali naslov za ekrane otvorene preko njih (detalji, forme).
 * @param hasBottomNav preko dna ekrana pluta donja navigacija: sadržaj ide do
 *        dna (vidi se kroz traku), a [screenContentPadding] ostavlja mesto da se
 *        poslednja stavka odskroluje iznad nje. Kad trake nema, ekran sam ostavlja
 *        mesto za sistemsku traku i tastaturu.
 */
@Composable
fun AppScreen(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    largeTitle: Boolean = onBack == null,
    hasBottomNav: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .then(if (hasBottomNav) Modifier else Modifier.navigationBarsPadding().imePadding())
    ) {
        TopBar(title = title, onBack = onBack, largeTitle = largeTitle, actions = actions)
        val clearance = if (hasBottomNav) {
            FloatingBarHeight + FloatingBarBottomMargin + Spacing.sm +
                WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        } else {
            0.dp
        }
        CompositionLocalProvider(LocalBottomBarClearance provides clearance) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                content = content
            )
        }
        bottomBar?.invoke()
    }
}

@Composable
private fun TopBar(
    title: String,
    onBack: (() -> Unit)?,
    largeTitle: Boolean,
    actions: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(
                // Dugme "nazad" ima svoju marginu, pa je traka sa njim malo uvučenija.
                start = if (onBack != null) Spacing.lg else Spacing.screen,
                end = Spacing.screen,
                top = if (largeTitle) Spacing.lg else Spacing.md,
                bottom = Spacing.sm
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            CircleIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                onClick = onBack
            )
            Spacer(Modifier.width(Spacing.md))
        }
        Text(
            text = title,
            style = if (largeTitle) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
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
