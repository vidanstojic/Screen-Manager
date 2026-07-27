package com.example.screenmanager.ui.limits

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.screenmanager.model.MainDestination
import com.example.screenmanager.ui.components.BottomNavBar
import com.example.screenmanager.ui.theme.DetailBackground
import com.example.screenmanager.ui.theme.PurpleAccent


@Composable
fun UsageLimitsScreen(
    selectedDestination: MainDestination,
    onDestinationSelected: (MainDestination) -> Unit,
    onAddLimit: () -> Unit
) {
    Scaffold(
        containerColor = DetailBackground,
        bottomBar = {
            BottomNavBar(
                selected = selectedDestination,
                onSelected = onDestinationSelected
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 78.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FocusIllustration()
                Text(
                    text = "Tap on the \"Add Usage Limit\" button below and\n" +
                            "discover the most effective limitation options\n" +
                            "for you to take control of your screen time.",
                    color = Color(0xFFC4BEC9),
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(top = 34.dp)
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 14.dp)
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PurpleAccent)
                    .clickable(onClick = onAddLimit)
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("+", color = Color(0xFF1F1B29), fontSize = 22.sp, fontWeight = FontWeight.Medium)
                    Text(
                        text = "Add Usage Limit",
                        color = Color(0xFF1F1B29),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }
    }
}


@Composable
fun FocusIllustration() {
    Canvas(
        modifier = Modifier
            .width(180.dp)
            .height(130.dp)
    ) {
        val blue = Color(0xFF1177C8)
        val offWhite = Color(0xFFDCE6EF)
        val skin = Color(0xFFF2D7C6)
        val centerX = size.width * 0.52f
        val baseY = size.height * 0.78f

        drawLine(
            color = blue,
            start = Offset(size.width * 0.12f, baseY),
            end = Offset(size.width * 0.88f, baseY),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawCircle(offWhite, radius = 16.dp.toPx(), center = Offset(centerX, size.height * 0.32f))
        drawRect(
            color = blue,
            topLeft = Offset(centerX - 18.dp.toPx(), size.height * 0.43f),
            size = androidx.compose.ui.geometry.Size(36.dp.toPx(), 38.dp.toPx())
        )
        drawCircle(skin, radius = 6.dp.toPx(), center = Offset(centerX - 18.dp.toPx(), size.height * 0.54f))
        drawCircle(skin, radius = 6.dp.toPx(), center = Offset(centerX + 18.dp.toPx(), size.height * 0.54f))
        drawLine(offWhite, Offset(centerX - 18.dp.toPx(), size.height * 0.71f), Offset(centerX - 46.dp.toPx(), baseY), 5.dp.toPx(), cap = StrokeCap.Round)
        drawLine(offWhite, Offset(centerX + 18.dp.toPx(), size.height * 0.71f), Offset(centerX + 48.dp.toPx(), baseY), 5.dp.toPx(), cap = StrokeCap.Round)
        drawLine(offWhite, Offset(size.width * 0.18f, baseY), Offset(size.width * 0.28f, size.height * 0.55f), 5.dp.toPx(), cap = StrokeCap.Round)
        drawLine(offWhite, Offset(size.width * 0.23f, baseY), Offset(size.width * 0.14f, size.height * 0.64f), 5.dp.toPx(), cap = StrokeCap.Round)
        drawLine(offWhite, Offset(size.width * 0.76f, baseY), Offset(size.width * 0.86f, size.height * 0.52f), 5.dp.toPx(), cap = StrokeCap.Round)
        drawLine(offWhite, Offset(size.width * 0.80f, baseY), Offset(size.width * 0.69f, size.height * 0.58f), 5.dp.toPx(), cap = StrokeCap.Round)
    }
}