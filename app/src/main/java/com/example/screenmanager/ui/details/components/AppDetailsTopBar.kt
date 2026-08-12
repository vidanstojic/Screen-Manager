package com.example.screenmanager.ui.details.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.screenmanager.domain.AppIconLoader
import com.example.screenmanager.model.AppUsageSummary
import com.example.screenmanager.ui.theme.PurpleAccent

/**
 * Gornja traka ekrana detalja sa povratkom, ikoncom aplikacije i menijem.
 */
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
        Text(
            text = "<",
            color = Color(0xFFD7D3E4),
            fontSize = 28.sp,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .clickable(onClick = onBack),
        )
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.08f)),
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
                // Fallback dok se ikonica učitava ili ako nije pronađena
                Text(
                    text = app.name.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
            }
        }
        Text(
            text = app.name,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp)
        )
        Text("⋮", color = Color.White, fontSize = 28.sp)
    }
}

/**
 * Tab sekcija u detaljima koja vizuelno razlikuje Stats i Settings.
 */
@Composable
fun AppDetailsTabs() {
    Row(modifier = Modifier.fillMaxWidth()) {
        DetailTab("Stats", active = true, modifier = Modifier.weight(1f))
        DetailTab("Settings", active = false, modifier = Modifier.weight(1f))
    }
}

/**
 * Jedan tab u detaljima, koristi se samo za vizuelno označavanje aktivnog taba.
 */
@Composable
private fun DetailTab(label: String, active: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.height(44.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = if (active) PurpleAccent else Color(0xFFD4CFDD),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (active) 3.dp else 1.dp)
                .background(if (active) PurpleAccent else Color(0xFF353140))
        )
    }
}