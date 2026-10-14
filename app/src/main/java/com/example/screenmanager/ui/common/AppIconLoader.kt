package com.example.screenmanager.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Učitava i kešira ikonice instaliranih aplikacija.
 *
 * Čitanje iz PackageManager-a je sporo, pa se radi van main thread-a
 * ([load]) i jednom po paketu. Ikonica se smanjuje na [MAX_ICON_PX] da
 * duga lista aplikacija ne zauzme previše memorije.
 */
object AppIconLoader {
    private const val MAX_ICON_PX = 144
    private const val SAMPLE_SIZE = 24
    private const val HUE_BUCKETS = 12

    /** Najmanje "obojenih" piksela (zbir težina) da bi se boja uopšte prihvatila. */
    private const val MIN_COLORFUL_WEIGHT = 12f

    /** Ograničen keš: ~4 MB ikonica (≈50 aplikacija od 144 px), ne cela lista instaliranih. */
    private const val CACHE_BYTES = 4 * 1024 * 1024

    /** `Entry(null)` znači "već probano, paket nema ikonicu" (npr. deinstaliran). */
    private class Entry(val bitmap: ImageBitmap?)

    /**
     * Proces živi koliko i FocusMonitorService (stalno), pa neograničen keš ikonica
     * iz izbora aplikacija ostaje u memoriji zauvek. LruCache + [trim] kad se UI sakrije.
     */
    private val cache = object : LruCache<String, Entry>(CACHE_BYTES) {
        override fun sizeOf(key: String, value: Entry): Int =
            value.bitmap?.let { it.width * it.height * 4 } ?: 1
    }

    /** `AccentEntry(null)` znači "ikonica nema izraženu boju" (npr. crno-bela). */
    private class AccentEntry(val color: Color?)

    private val accents = ConcurrentHashMap<String, AccentEntry>()

    /** Već učitana ikonica, bez čekanja — da se pri skrolovanju ne vidi treptaj. */
    fun cached(packageName: String): ImageBitmap? = cache.get(packageName)?.bitmap

    suspend fun load(context: Context, packageName: String): ImageBitmap? {
        cache.get(packageName)?.let { return it.bitmap }
        return withContext(Dispatchers.IO) {
            val bitmap = runCatching {
                val drawable = context.applicationContext.packageManager.getApplicationIcon(packageName)
                drawable.toBitmap().asImageBitmap()
            }.getOrNull()
            cache.put(packageName, Entry(bitmap))
            bitmap
        }
    }

    /** UI više nije vidljiv: ikonice se oslobađaju (boje su male i ostaju). */
    fun trim() = cache.evictAll()

    /** Već izračunata boja aplikacije, bez čekanja. */
    fun cachedAccent(packageName: String): Color? = accents[packageName]?.color

    /**
     * Prepoznatljiva boja aplikacije iz njene ikonice (Instagram roze, YouTube
     * crvena...), posvetljena da se dobro vidi na tamnoj pozadini.
     * `null` ako ikonica nema izraženu boju.
     */
    suspend fun loadAccent(context: Context, packageName: String): Color? {
        accents[packageName]?.let { return it.color }
        val icon = load(context, packageName)
        return withContext(Dispatchers.Default) {
            val color = icon?.let { dominantColor(it.asAndroidBitmap()) }
            accents[packageName] = AccentEntry(color)
            color
        }
    }

    /**
     * Dominantna "živa" boja: pikseli se grupišu po nijansi (12 grupa po 30°),
     * a svaki piksel vredi koliko je zasićen i svetao — siva, bela i crna
     * pozadina ikonice se tako ne računaju.
     */
    private fun dominantColor(bitmap: Bitmap): Color? {
        val small = Bitmap.createScaledBitmap(bitmap, SAMPLE_SIZE, SAMPLE_SIZE, true)
        val pixels = IntArray(SAMPLE_SIZE * SAMPLE_SIZE)
        small.getPixels(pixels, 0, SAMPLE_SIZE, 0, 0, SAMPLE_SIZE, SAMPLE_SIZE)

        val weight = FloatArray(HUE_BUCKETS)
        val sinSum = FloatArray(HUE_BUCKETS)
        val cosSum = FloatArray(HUE_BUCKETS)
        val satSum = FloatArray(HUE_BUCKETS)
        val hsv = FloatArray(3)
        for (pixel in pixels) {
            if (android.graphics.Color.alpha(pixel) < 200) continue
            android.graphics.Color.colorToHSV(pixel, hsv)
            val (hue, saturation, value) = Triple(hsv[0], hsv[1], hsv[2])
            if (saturation < 0.25f || value < 0.3f) continue
            val bucket = (hue / (360f / HUE_BUCKETS)).toInt().coerceIn(0, HUE_BUCKETS - 1)
            val w = saturation * value
            val radians = Math.toRadians(hue.toDouble())
            weight[bucket] += w
            sinSum[bucket] += (sin(radians) * w).toFloat()
            cosSum[bucket] += (cos(radians) * w).toFloat()
            satSum[bucket] += saturation * w
        }
        val best = weight.indices.maxByOrNull { weight[it] } ?: return null
        if (weight[best] < MIN_COLORFUL_WEIGHT) return null

        var hue = Math.toDegrees(atan2(sinSum[best].toDouble(), cosSum[best].toDouble())).toFloat()
        if (hue < 0f) hue += 360f
        val saturation = (satSum[best] / weight[best]).coerceIn(0.55f, 0.85f)
        return Color.hsv(hue, saturation, 0.95f)
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
