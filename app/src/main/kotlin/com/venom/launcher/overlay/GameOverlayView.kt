package com.venom.launcher.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Build
import android.view.View
import com.venom.launcher.data.GameTelemetry
import kotlin.math.max

/**
 * The little HUD that sits on top of whatever game you're playing.
 *
 * Deliberately a plain Canvas [View] rather than Compose: an overlay that
 * repaints several times a second must not drag the Compose runtime (and its
 * allocations) into the game's frame budget.
 */
class GameOverlayView(context: Context) : View(context) {

    var telemetry: GameTelemetry = GameTelemetry()
        private set
    var frameHistory: List<Float> = emptyList()
        private set
    var showFps = true
    var showTemp = true
    var showBattery = true

    private val density get() = resources.displayMetrics.density
    private val pad = 10f
    private val box = RectF()
    private val shell = RectF()

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(215, 8, 12, 10)
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(120, 180, 255, 57)
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * resources.displayMetrics.density
    }
    private val bigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = android.graphics.Typeface.MONOSPACE
        textSize = 20f * resources.displayMetrics.density
        isFakeBoldText = true
    }
    private val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = android.graphics.Typeface.MONOSPACE
        textSize = 9f * resources.displayMetrics.density
        color = Color.argb(190, 255, 255, 255)
    }
    private val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = android.graphics.Typeface.MONOSPACE
        textSize = 10f * resources.displayMetrics.density
    }
    private val graphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 2f * resources.displayMetrics.density
        style = Paint.Style.STROKE
    }

    fun update(telemetry: GameTelemetry, history: List<Float>) {
        this.telemetry = telemetry
        this.frameHistory = history
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = (104 * density).toInt()
        val h = (46 * density).toInt()
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        shell.set(0f, 0f, w, h)
        box.set(pad * density * 0.4f, pad * density * 0.4f, w - pad * density * 0.4f, h - pad * density * 0.4f)

        canvas.drawRoundRect(box, 12f * density, 12f * density, bgPaint)
        canvas.drawRoundRect(box, 12f * density, 12f * density, borderPaint)

        val left = box.left + 10f * density
        val top = box.top + 6f * density

        if (showFps) {
            bigPaint.color = fpsColor(telemetry.fps)
            canvas.drawText("${telemetry.fps}", left, top + 20f * density, bigPaint)
            val numWidth = bigPaint.measureText("${telemetry.fps}")
            canvas.drawText("FPS", left + numWidth + 4f * density, top + 20f * density, unitPaint)
        }

        // secondary line: temp + battery
        val bits = ArrayList<String>(3)
        if (showTemp && !telemetry.batteryTempC.isNaN() && telemetry.batteryTempC > 0f) {
            bits.add("${"%.0f".format(telemetry.batteryTempC)}°C")
        }
        if (showBattery) {
            bits.add("${telemetry.batteryPercent}%")
            if (telemetry.isCharging) bits.add("⚡")
        }
        if (bits.isNotEmpty()) {
            val line = bits.joinToString(" ")
            smallPaint.color = when {
                telemetry.batteryTempC >= 44f -> Color.rgb(255, 107, 74)
                telemetry.batteryPercent <= 15 -> Color.rgb(255, 77, 94)
                else -> Color.argb(200, 255, 255, 255)
            }
            canvas.drawText(line, left, top + 34f * density, smallPaint)
        }

        // frame-time sparkline along the right edge
        if (frameHistory.size > 3) {
            val gx = box.right - 4f * density
            val gw = 34f * density
            val gh = 16f * density
            val gy = box.top + 8f * density
            val maxMs = max(24f, frameHistory.maxOrNull() ?: 24f)
            val path = Path()
            val step = gw / (frameHistory.size - 1)
            frameHistory.forEachIndexed { i, ms ->
                val x = gx - gw + (i * step)
                val y = gy + gh - ((ms.coerceAtMost(maxMs) / maxMs) * gh)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            graphPaint.color = fpsColor(telemetry.fps)
            canvas.drawPath(path, graphPaint)
        }
    }

    private fun fpsColor(fps: Int): Int = when {
        fps <= 0 -> Color.argb(160, 255, 255, 255)
        fps >= 55 -> Color.rgb(180, 255, 57)
        fps >= 30 -> Color.rgb(255, 214, 70)
        else -> Color.rgb(255, 77, 94)
    }
}
