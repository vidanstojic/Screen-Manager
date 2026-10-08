package com.example.screenmanager.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

/**
 * Prepoznatljiva boja aplikacije (iz ikonice), ili `null` dok se ne izračuna
 * odnosno ako ikonica nema izraženu boju.
 */
@Composable
fun rememberAppAccent(packageName: String): Color? {
    val context = LocalContext.current
    val accent by produceState(initialValue = AppIconLoader.cachedAccent(packageName), key1 = packageName) {
        value = AppIconLoader.loadAccent(context, packageName)
    }
    return accent
}

/**
 * Ikonice više aplikacija naslagane jedna preko druge (npr. YouTube + Instagram
 * kod Shorts/Reels pravila, izabrane aplikacije u formi).
 *
 * @param dimmed priguši ikonice (npr. pravilo je isključeno).
 */
@Composable
fun AppIconStack(
    packageNames: List<String>,
    labelFor: (String) -> String,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    overlap: Float = 0.32f,
    dimmed: Boolean = false
) {
    val step = size * (1f - overlap)
    Box(modifier = modifier.alpha(if (dimmed) 0.4f else 1f)) {
        packageNames.forEachIndexed { index, id ->
            AppIcon(
                packageName = id,
                label = labelFor(id),
                size = size,
                modifier = Modifier.offset(x = step * index)
            )
        }
        // Zauzima širinu naslaganih ikonica (offset ne utiče na layout).
        Spacer(Modifier.width(size + step * (packageNames.size - 1).coerceAtLeast(0)))
    }
}
