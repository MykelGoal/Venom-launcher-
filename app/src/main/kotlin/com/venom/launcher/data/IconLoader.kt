package com.venom.launcher.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.pow

/**
 * Renders one icon into a fixed-size ARGB_8888 bitmap, applies an optional
 * uniform mask, and caches the result in an LRU.
 *
 * Runs off the main thread; see [iconPainter] for the Compose-side wrapper.
 */
object IconLoader {

    private const val SIZE = 192
    private const val CACHE_ENTRIES = 260

    private val cache = object : LruCache<String, ImageBitmap>(CACHE_ENTRIES) {}

    fun load(context: Context, app: AppInfo, pack: IconPack?, shape: IconShape): ImageBitmap {
        val key = "${app.componentKey}|${shape.name}|${pack?.packageName ?: "-"}"
        cache.get(key)?.let { return it }
        val bitmap = render(context, app, pack, shape)
        cache.put(key, bitmap)
        return bitmap
    }

    fun clear() = cache.evictAll()

    private fun render(
        context: Context,
        app: AppInfo,
        pack: IconPack?,
        shape: IconShape,
    ): ImageBitmap {
        val drawable: Drawable? = pack?.getDrawable(app.componentKey, app.packageName)
            ?: runCatching { context.packageManager.getActivityIcon(app.componentName) }.getOrNull()

        val base = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(base)

        if (drawable == null) {
            drawLetterFallback(canvas, app)
        } else {
            // Adaptive icons carry their own safe-zone; shrink slightly so the
            // artwork doesn't get clipped by the mask we apply afterwards.
            val inset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                drawable is AdaptiveIconDrawable
            ) (SIZE * 0.06f).roundToInt() else 0
            drawable.setBounds(inset, inset, SIZE - inset, SIZE - inset)
            runCatching { drawable.draw(canvas) }.onFailure { drawLetterFallback(canvas, app) }
        }

        val shaped = if (shape == IconShape.SYSTEM) base else applyMask(base, shape)
        return shaped.asImageBitmap()
    }

    private fun drawLetterFallback(canvas: Canvas, app: AppInfo) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(255, 28, 38, 30)
        }
        canvas.drawCircle(SIZE / 2f, SIZE / 2f, SIZE / 2f, paint)
        val letter = app.firstLetter.toString()
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(255, 180, 255, 57)
            textAlign = Paint.Align.CENTER
            textSize = SIZE * 0.46f
        }
        val y = SIZE / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(letter, SIZE / 2f, y, textPaint)
    }

    private fun applyMask(src: Bitmap, shape: IconShape): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val path = shapePath(shape, src.width.toFloat(), src.height.toFloat())
        canvas.clipPath(path)
        canvas.drawBitmap(src, 0f, 0f, null)
        return out
    }

    internal fun shapePath(shape: IconShape, w: Float, h: Float): Path {
        val path = Path()
        when (shape) {
            IconShape.CIRCLE -> {
                path.addCircle(w / 2f, h / 2f, w / 2f, Path.Direction.CW)
            }

            IconShape.ROUNDED -> {
                val r = w * 0.22f
                path.addRoundRect(
                    0f, 0f, w, h,
                    floatArrayOf(r, r, r, r, r, r, r, r),
                    Path.Direction.CW
                )
            }

            IconShape.HEXAGON -> {
                val cx = w / 2f
                val cy = h / 2f
                val r = w / 2f
                for (i in 0 until 6) {
                    val a = Math.toRadians((60.0 * i) - 90.0)
                    val x = cx + (r * cos(a)).toFloat()
                    val y = cy + (r * sin(a)).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
            }

            IconShape.SQUIRCLE -> {
                // superellipse with n = 4.2, sampled at 72 points
                val a = w / 2f
                val b = h / 2f
                val n = 4.2f
                val p = 2f / n
                val steps = 72
                for (i in 0..steps) {
                    val t = (i.toFloat() / steps) * 2f * Math.PI.toFloat()
                    val ct = cos(t)
                    val st = sin(t)
                    val x = a + a * kotlin.math.sign(ct) * abs(ct).pow(p)
                    val y = b + b * kotlin.math.sign(st) * abs(st).pow(p)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
            }

            IconShape.SYSTEM -> path.addRect(0f, 0f, w, h, Path.Direction.CW)
        }
        return path
    }
}
