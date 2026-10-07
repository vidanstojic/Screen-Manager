package com.example.screenmanager.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.screenmanager.ui.common.AppIconLoader
import com.example.screenmanager.ui.theme.AppTheme

/**
 * Ikonica instalirane aplikacije po packageName-u. Dok se ne učita (ili ako
 * aplikacija više nije instalirana) prikazuje prvo slovo imena.
 */
@Composable
fun AppIcon(
    packageName: String,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val context = LocalContext.current
    val icon by produceState<ImageBitmap?>(
        initialValue = AppIconLoader.cached(packageName),
        key1 = packageName
    ) {
        value = AppIconLoader.load(context, packageName)
    }

    val bitmap = icon
    if (bitmap != null) {
        // Ikonica već ima svoj oblik (maska launcher-a), pa ide bez podloge.
        Image(bitmap = bitmap, contentDescription = null, modifier = modifier.size(size))
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(size * 0.28f))
                .background(AppTheme.colors.surfaceRaised),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = AppTheme.colors.textSecondary
            )
        }
    }
}
