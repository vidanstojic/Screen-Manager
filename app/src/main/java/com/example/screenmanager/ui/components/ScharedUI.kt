package com.example.screenmanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
            .background(Color(0xFF47444E))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MainDestination.bottomItems.forEach { item ->
            val active = selected == item
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(if (active) Color(0xFF6A5E82) else Color.Transparent)
                    .clickable { onSelected(item) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = item.icon,
                    color = if (active) PurpleAccent else Color(0xFFE8E4EE),
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 22.sp
                )
                Text(
                    text = item.navLabel,
                    color = Color.White,
                    fontSize = 10.sp,
                    lineHeight = 11.sp,
                    maxLines = 2
                )
            }
        }
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