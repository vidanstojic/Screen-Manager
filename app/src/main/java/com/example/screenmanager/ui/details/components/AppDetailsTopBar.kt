package com.example.screenmanager.ui.details.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.screenmanager.domain.AppIconLoader
import com.example.screenmanager.model.AppUsageSummary
import com.example.screenmanager.ui.theme.GlassTheme

@Composable
fun AppDetailsTopBar(app: AppUsageSummary, onBack: () -> Unit) {
    val context = LocalContext.current
    var icon by remember(app.packageName) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(app.packageName) {
        icon = AppIconLoader.getIcon(context, app.packageName)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(GlassTheme.colors.surface)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Back", tint = GlassTheme.colors.textPrimary)
        }
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(GlassTheme.colors.surfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            val currentIcon = icon
            if (currentIcon != null) {
                Image(
                    bitmap = currentIcon,
                    contentDescription = app.name,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Text(
                    text = app.name.take(1).uppercase(),
                    color = GlassTheme.colors.textPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
            }
        }
        Text(
            text = app.name,
            color = GlassTheme.colors.textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp)
        )
        Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = GlassTheme.colors.textSecondary)
    }
}

@Composable
fun AppDetailsTabs() {
    Row(modifier = Modifier.fillMaxWidth()) {
        DetailTab("Stats", active = true, modifier = Modifier.weight(1f))
        DetailTab("Settings", active = false, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun DetailTab(label: String, active: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.height(44.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = if (active) GlassTheme.colors.accentPrimary else GlassTheme.colors.textSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (active) 3.dp else 1.dp)
                .background(if (active) GlassTheme.colors.accentPrimary else GlassTheme.colors.borderEnd)
        )
    }
}