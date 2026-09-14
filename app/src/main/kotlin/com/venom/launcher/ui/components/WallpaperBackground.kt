package com.venom.launcher.ui.components

import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * The device wallpaper, with an optional cheap blur (downscale + upscale, so it
 * works on every API level without RenderEffect) and a dim scrim for legibility.
 */
@Composable
fun WallpaperBackground(
    dim: Float,
    blur: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    LaunchedEffect(blur) {
        bitmap = withContext(Dispatchers.IO) {
            val wm = WallpaperManager.getInstance(context.applicationContext)
            val drawable = runCatching { wm.drawable }.getOrNull()
            drawable?.toBitmap()?.let { if (blur) it.cheapBlur(14) else it }?.asImageBitmap()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(Modifier)
                .let { it }
        )
        // scrim
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = Color.Black.copy(alpha = dim))
        }
    }
}

private fun Drawable.toBitmap(): Bitmap? {
    if (this is BitmapDrawable) return runCatching { bitmap }.getOrNull()
    val w = intrinsicWidth.takeIf { it > 0 } ?: 1080
    val h = intrinsicHeight.takeIf { it > 0 } ?: 1920
    val bmp = runCatching { Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888) }.getOrNull()
        ?: return null
    val canvas = Canvas(bmp)
    setBounds(0, 0, w, h)
    runCatching { draw(canvas) }.onFailure { return null }
    return bmp
}

/** RenderScript-free blur: shrink hard, then scale back up with filtering. */
private fun Bitmap.cheapBlur(radius: Int): Bitmap {
    val factor = (1f / radius.coerceIn(1, 32) * 8f).coerceIn(0.02f, 0.5f)
    val w = (width * factor).roundToInt().coerceAtLeast(2)
    val h = (height * factor).roundToInt().coerceAtLeast(2)
    val small = Bitmap.createScaledBitmap(this, w, h, true)
    return Bitmap.createScaledBitmap(small, width, height, true)
}
