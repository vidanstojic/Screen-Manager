package com.example.screenmanager.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Razmaci na 4dp mreži — umesto "magičnih" dp vrednosti po ekranima. */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp

    /** Horizontalna margina sadržaja svakog ekrana. */
    val screen = 20.dp
}

/**
 * Zaobljenja: small → chip/polje, medium → dugme/red, large → kartica,
 * extraLarge → istaknuta (hero) kartica i donji list.
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
