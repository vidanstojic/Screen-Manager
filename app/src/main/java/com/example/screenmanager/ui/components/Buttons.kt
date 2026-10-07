package com.example.screenmanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.screenmanager.ui.theme.AppTheme
import com.example.screenmanager.ui.theme.Spacing

private val ButtonPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)

/** Glavna akcija na ekranu (najviše jedna po ekranu): Save, Activate... */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 50.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = AppTheme.colors.accent,
            contentColor = AppTheme.colors.onAccent,
            disabledContainerColor = AppTheme.colors.surfaceRaised,
            disabledContentColor = AppTheme.colors.textMuted
        ),
        contentPadding = ButtonPadding
    ) {
        ButtonContent(text = text, icon = icon)
    }
}

/** Sporedna akcija pored glavne: Cancel, Choose apps... */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    contentColor: Color = AppTheme.colors.textPrimary
) {
    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 50.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = AppTheme.colors.surfaceRaised,
            contentColor = contentColor,
            disabledContainerColor = AppTheme.colors.surfaceRaised,
            disabledContentColor = AppTheme.colors.textMuted
        ),
        contentPadding = ButtonPadding
    ) {
        ButtonContent(text = text, icon = icon)
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
 * Okruglo dugme sa ikonicom (nazad, podešavanja, +/-).
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
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(AppTheme.colors.surfaceRaised)
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) AppTheme.colors.textPrimary else AppTheme.colors.textMuted,
                modifier = Modifier.size(20.dp)
            )
        }
        if (showBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 1.dp, y = (-1).dp)
                    .size(11.dp)
                    .clip(CircleShape)
                    .background(AppTheme.colors.background)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(AppTheme.colors.warning)
                )
            }
        }
    }
}

/** Plutajuće dugme za dodavanje nove stavke (alarm). */
@Composable
fun AddFab(
    contentDescription: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        containerColor = AppTheme.colors.accent,
        contentColor = AppTheme.colors.onAccent
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription)
    }
}
