package com.example.screenmanager.ui.feature.alarms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.screenmanager.ui.theme.AppTheme
import com.example.screenmanager.ui.theme.Spacing
import java.util.Locale

/** Naslov grupe podešavanja u formi alarma ("SOUND", "WAKE-UP MISSION"...) + sadržaj. */
@Composable
fun EditorSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = title.uppercase(Locale.ENGLISH),
            style = MaterialTheme.typography.labelSmall,
            color = AppTheme.colors.textMuted,
            modifier = Modifier.padding(start = Spacing.xs)
        )
        content()
    }
}

/** Objašnjenje ispod kartice u formi. */
@Composable
fun EditorHint(text: String, color: androidx.compose.ui.graphics.Color = AppTheme.colors.textSecondary) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        modifier = Modifier.padding(horizontal = Spacing.xs)
    )
}
