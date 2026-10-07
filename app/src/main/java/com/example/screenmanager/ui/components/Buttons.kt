package com.example.screenmanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.screenmanager.ui.theme.AppTheme
import com.example.screenmanager.ui.theme.GlowAlpha
import com.example.screenmanager.ui.theme.Spacing
import com.example.screenmanager.ui.theme.glass
import com.example.screenmanager.ui.theme.glow

private val ButtonPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)

// Zaobljenja za sjaj; moraju da prate MaterialTheme.shapes.medium / large (ui/theme/Dimens.kt).
private val ButtonCorner = 16.dp
private val FabCorner = 20.dp

/**
 * Glavna akcija na ekranu (najviše jedna po ekranu): Save, Activate...
 * Akcentni gradijent sa mekim sjajem; isključeno dugme je ravno i prigušeno.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    val colors = AppTheme.colors
    val shape = MaterialTheme.shapes.medium
    ButtonRow(
        onClick = onClick,
        enabled = enabled,
        contentColor = if (enabled) colors.onAccent else colors.textMuted,
        modifier = modifier
            .then(if (enabled) Modifier.glow(colors.accentStart.copy(alpha = GlowAlpha), ButtonCorner) else Modifier)
            .clip(shape)
            .background(if (enabled) colors.accentBrush else SolidColor(colors.surfaceRaised))
    ) {
        ButtonContent(text = text, icon = icon)
    }
}

/** Sporedna akcija pored glavne: Cancel, Choose apps... Staklena površina. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    contentColor: Color = AppTheme.colors.textPrimary
) {
    val colors = AppTheme.colors
    ButtonRow(
        onClick = onClick,
        enabled = enabled,
        contentColor = if (enabled) contentColor else colors.textMuted,
        modifier = modifier.glass(colors, MaterialTheme.shapes.medium)
    ) {
        ButtonContent(text = text, icon = icon)
    }
}

/** Zajednički kostur dugmadi: visina, klik, centriran sadržaj i boja sadržaja. */
@Composable
private fun ButtonRow(
    onClick: () -> Unit,
    enabled: Boolean,
    contentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Row(
            modifier = modifier
                .defaultMinSize(minHeight = 52.dp)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(ButtonPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
private fun ButtonContent(text: String, icon: ImageVector?) {
    if (icon != null) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(Spacing.sm))
    }
    Text(text = text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
}

/** Tekstualna akcija bez podloge (npr. "Delete" u crvenoj boji). */
@Composable
fun TextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = AppTheme.colors.accent,
    enabled: Boolean = true
) {
    TextButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) color else AppTheme.colors.textMuted
        )
    }
}

/**
 * Okruglo stakleno dugme sa ikonicom (nazad, podešavanja, +/-).
 * [showBadge] crta tačku upozorenja u uglu (npr. "nedostaju dozvole").
 */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showBadge: Boolean = false
) {
    val colors = AppTheme.colors
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .glass(colors, CircleShape)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) colors.textPrimary else colors.textMuted,
                modifier = Modifier.size(20.dp)
            )
        }
        if (showBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-8).dp, y = 8.dp)
                    .size(8.dp)
                    .glow(colors.warning.copy(alpha = 0.8f), blurRadius = 6.dp, offsetY = 0.dp)
                    .clip(CircleShape)
                    .background(colors.warning)
            )
        }
    }
}

/** Plutajuće dugme za dodavanje nove stavke (alarm): akcentni gradijent sa sjajem. */
@Composable
fun AddFab(
    contentDescription: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    Box(
        modifier = modifier
            .size(58.dp)
            .glow(colors.accentStart.copy(alpha = GlowAlpha + 0.1f), FabCorner, blurRadius = 20.dp)
            .clip(MaterialTheme.shapes.large)
            .background(colors.accentBrush)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = colors.onAccent)
    }
}
