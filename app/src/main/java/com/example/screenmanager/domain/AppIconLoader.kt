package com.example.screenmanager.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Učitava i kešira ikonice instaliranih aplikacija.
 *
 * Konvertuje Drawable u ImageBitmap jednom po paketu i čuva u memoriji,
 * jer je čitanje ikonice iz PackageManager-a relativno skupa operacija.
 */
object AppIconLoader {
    private val cache = mutableMapOf<String, ImageBitmap?>()

    fun getIcon(context: Context, packageName: String): ImageBitmap? {
        cache[packageName]?.let { return it }
        if (cache.containsKey(packageName)) return null // već probano, nema ikonice

        val bitmap = runCatching {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            drawableToBitmap(drawable).asImageBitmap()
        }.getOrNull()

        cache[packageName] = bitmap
        return bitmap
    }

    fun getAppLabel(context: Context, packageName: String): String {
        return runCatching {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        }.getOrDefault(packageName)
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 96
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 96
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}