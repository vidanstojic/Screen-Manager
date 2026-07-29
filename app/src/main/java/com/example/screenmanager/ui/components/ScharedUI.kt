package com.example.screenmanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.screenmanager.model.MainDestination
import com.example.screenmanager.ui.theme.DetailBackground
import com.example.screenmanager.ui.theme.PurpleAccent

@Composable
fun BottomNavBar(
    selected: MainDestination,
    onSelected: (MainDestination) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(Color(0xFF1F1B29))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MainDestination.bottomItems.forEach { item ->
            val active = selected == item
            val isAction = item == MainDestination.AddLimit
            Box(
                modifier = Modifier
                    .weight(if (isAction) 1.1f else 1f)
                    .height(if (isAction) 48.dp else 44.dp)
                    .clip(RoundedCornerShape(if (isAction) 16.dp else 14.dp))
                    .background(
                        when {
                            isAction -> PurpleAccent
                            active -> Color(0xFF3A334B)
                            else -> Color(0xFF2A2535)
                        }
                    )
                    .border(
                        1.dp,
                        if (active) PurpleAccent else Color(0xFF3A334B),
                        RoundedCornerShape(if (isAction) 16.dp else 14.dp)
                    )
                    .clickable { onSelected(item) }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = item.icon,
                        color = if (isAction) Color(0xFF1F1B29) else if (active) PurpleAccent else Color.White,
                        fontSize = if (isAction) 22.sp else 19.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 20.sp
                    )
                    Text(
                        text = item.navLabel,
                        color = if (isAction) Color(0xFF1F1B29) else Color(0xFFE8E4EE),
                        fontSize = 10.sp,
                        lineHeight = 11.sp,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE5E9EF), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            color = Color(0xFF24323C),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = Color(0xFF6A7883),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

@Composable
fun InfoBanner(
    title: String,
    description: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFEAF4FF))
            .border(1.dp, Color(0xFFD4E9FF), RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Text(title, color = Color(0xFF176FBA), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(description, color = Color(0xFF4E6477), fontSize = 13.sp, lineHeight = 18.sp)
        if (!actionLabel.isNullOrBlank() && onActionClick != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = actionLabel,
                color = Color(0xFF176FBA),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onActionClick)
            )
        }
    }
}

@Composable
fun ToggleChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) PurpleAccent else Color(0xFFF1F4F8))
            .border(1.dp, if (selected) PurpleAccent else Color(0xFFD5DCE3), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color(0xFF1F1B29) else Color(0xFF54616C),
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
    }
}

@Composable
fun StatusChip(
    text: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (isActive) Color(0xFFE6F5EC) else Color(0xFFF3F4F7))
            .border(
                1.dp,
                if (isActive) Color(0xFF76C893) else Color(0xFFDCE1E7),
                RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = if (isActive) Color(0xFF1E7A4E) else Color(0xFF64707A),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun GeneralPlaceholderScreen(
    destination: MainDestination,
    title: String,
    description: String,
    onDestinationSelected: (MainDestination) -> Unit
) {
    Scaffold(
        containerColor = DetailBackground,
        bottomBar = {
            BottomNavBar(
                selected = destination,
                onSelected = onDestinationSelected
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(22.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(
                text = description,
                color = Color(0xFFC4BEC9),
                fontSize = 15.sp,
                lineHeight = 22.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}
