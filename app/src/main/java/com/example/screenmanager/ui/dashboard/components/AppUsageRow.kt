package com.example.screenmanager.ui.dashboard.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun AppUsageRow(
    app: AppUsageSummary,
    maxMinutes: Int,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var icon by remember(app.packageName) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(app.packageName) {
        icon = AppIconLoader.getIcon(context, app.packageName)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
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
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = app.name,
                    color = GlassTheme.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${app.minutes}m",
                    color = GlassTheme.colors.textSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .border(1.dp, GlassTheme.colors.borderEnd, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(">", color = GlassTheme.colors.textPrimary, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(GlassTheme.colors.surfaceElevated)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((app.minutes / maxMinutes.toFloat()).coerceIn(0.04f, 1f))
                        .height(7.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(GlassTheme.colors.accentPrimary)
                )
            }
        }
    }
}