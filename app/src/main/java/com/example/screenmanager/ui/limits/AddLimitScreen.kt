package com.example.screenmanager.ui.limits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.screenmanager.model.MockAppUsage
import com.example.screenmanager.ui.theme.DetailBackground
import com.example.screenmanager.ui.theme.PurpleAccent


@Composable
fun AddLimitScreen(
    selectedApp: MockAppUsage?,
    onBack: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    var limitName by remember { mutableStateOf("") }
    Scaffold(
        containerColor = DetailBackground,
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .background(Color(0xFF47444E))
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cancel",
                    color = PurpleAccent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onCancel)
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                )
                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .width(88.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF8E75B4))
                        .clickable(onClick = onSave),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Save", color = Color(0xFF17131F), fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "<",
                        color = Color(0xFFD7D3E4),
                        fontSize = 30.sp,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onBack),
                    )
                    Text(
                        text = "Add Limit",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
            item {
                LimitOptionRow(
                    title = selectedApp?.let { "Limit ${it.name}" }
                        ?: "Select apps, websites, or categories to\nlimit",
                    large = true
                )
            }
            item {
                Text(
                    text = "How do you want to limit usage?",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            item { LimitOptionRow("Set a Daily Usage Limit") }
            item { LimitOptionRow("Block on a schedule") }
            item { LimitOptionRow("Block permanently") }
            item { LimitOptionRow("Enable variable session limits") }
            item {
                Text(
                    text = "Want to name your limit? (Optional)",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            item {
                BasicTextField(
                    value = limitName,
                    onValueChange = { limitName = it },
                    textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFF8B8594), RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (limitName.isBlank()) {
                                Text("Limit Name", color = Color(0xFF8E8796), fontSize = 12.sp)
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }
    }
}


@Composable
private fun LimitOptionRow(
    title: String,
    large: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (large) 70.dp else 48.dp)
            .clip(RoundedCornerShape(9.dp))
            .border(1.dp, Color(0xFF625B6C), RoundedCornerShape(9.dp))
            .clickable { }
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            color = PurpleAccent,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            modifier = Modifier.weight(1f)
        )
        Text(">", color = Color(0xFF8E8796), fontSize = 26.sp)
    }
}