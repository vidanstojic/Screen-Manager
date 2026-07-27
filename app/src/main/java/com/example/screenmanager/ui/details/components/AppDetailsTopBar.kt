package com.example.screenmanager.ui.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.screenmanager.model.MockAppUsage
import com.example.screenmanager.ui.theme.PurpleAccent

@Composable
fun AppDetailsTopBar(app: MockAppUsage, onBack: () -> Unit) {
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
                .background(app.iconColor),
            contentAlignment = Alignment.Center
        ) {
            Text(app.iconText, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
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