package com.example.screenmanager.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Učitava i kešira ikonice instaliranih aplikacija.
 *
 * Čitanje iz PackageManager-a je sporo, pa se radi van main thread-a
 * ([load]) i jednom po paketu. Ikonica se smanjuje na [MAX_ICON_PX] da
 * duga lista aplikacija ne zauzme previše memorije.
 */
object AppIconLoader {
    private const val MAX_ICON_PX = 144

    /** `Entry(null)` znači "već probano, paket nema ikonicu" (npr. deinstaliran). */
    private class Entry(val bitmap: ImageBitmap?)

    private val cache = ConcurrentHashMap<String, Entry>()

    /** Već učitana ikonica, bez čekanja — da se pri skrolovanju ne vidi treptaj. */
    fun cached(packageName: String): ImageBitmap? = cache[packageName]?.bitmap

    suspend fun load(context: Context, packageName: String): ImageBitmap? {
        cache[packageName]?.let { return it.bitmap }
        return withContext(Dispatchers.IO) {
            val bitmap = runCatching {
                val drawable = context.applicationContext.packageManager.getApplicationIcon(packageName)
                drawable.toBitmap().asImageBitmap()
            }.getOrNull()
            cache[packageName] = Entry(bitmap)
            bitmap
        }
    }

    private fun Drawable.toBitmap(): Bitmap {
        val width = intrinsicWidth.takeIf { it > 0 }?.coerceAtMost(MAX_ICON_PX) ?: MAX_ICON_PX
        val height = intrinsicHeight.takeIf { it > 0 }?.coerceAtMost(MAX_ICON_PX) ?: MAX_ICON_PX
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        return bitmap
    }
}
