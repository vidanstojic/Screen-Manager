package com.example.screenmanager.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Gradivni blokovi stila "aurora / glass". Komponente ih kombinuju umesto da
 * svaka sama slaže background + border + senku.
 */

/**
 * "Staklena" površina: poluprovidna ispuna i tanka svetla ivica, isečena na [shape].
 * [border] zamenjuje podrazumevanu ivicu (npr. obojena ivica upozorenja).
 */
fun Modifier.glass(colors: AppColors, shape: Shape, border: Brush = colors.glassBorderBrush): Modifier =
    clip(shape)
        .background(colors.glassBrush)
        .border(1.dp, border, shape)

/**
 * Meki sjaj u boji [color] iza elementa (primarno dugme, izabrana stavka).
 * Mora da stoji PRE `clip`/`background` u lancu modifikatora.
 *
 * @param cornerRadius zaobljenje elementa; `null` = potpuno zaobljen ("pilula" ili krug).
 */
fun Modifier.glow(
    color: Color,
    cornerRadius: Dp? = null,
    blurRadius: Dp = 16.dp,
    offsetY: Dp = 6.dp
): Modifier = drawWithCache {
    // Senka se crta providnom bojom, pa se vidi samo sjaj (boja senke nosi svoju providnost).
    val paint = Paint().asFrameworkPaint().apply {
        this.color = android.graphics.Color.TRANSPARENT
        setShadowLayer(blurRadius.toPx(), 0f, offsetY.toPx(), color.toArgb())
    }
    val radius = cornerRadius?.toPx() ?: (size.minDimension / 2f)
    onDrawBehind {
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, radius, radius, paint)
        }
    }
}

/** Providnost sjaja oko akcentnih elemenata. */
const val GlowAlpha = 0.5f
