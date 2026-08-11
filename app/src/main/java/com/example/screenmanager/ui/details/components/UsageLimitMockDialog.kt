package com.example.screenmanager.ui.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.screenmanager.model.MockAppUsage
import com.example.screenmanager.ui.theme.DetailBackground
import com.example.screenmanager.ui.theme.DetailCard
import com.example.screenmanager.ui.theme.PurpleAccent

/**
 * Privremeni dijalog koji predstavlja budući limit editor iz detalja aplikacije.
 *
 * Do njega se dolazi iz ekrana detalja kada korisnik pritisne plus.
 */
@Composable
fun UsageLimitMockDialog(app: MockAppUsage, onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xAA000000))
            .clickable(onClick = onClose)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = DetailCard,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { }
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Usage limit", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Limit podešavanja za ${app.name} ćemo povezati sa pravom konfiguracijom u sledećem koraku.",
                    color = Color(0xFFD8D2E0)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PurpleAccent)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Text("OK", color = DetailBackground, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}