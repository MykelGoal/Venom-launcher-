package com.venom.launcher.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.venom.launcher.data.GameTelemetry
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The Game Turbo bubble: a small draggable FPS pill that expands into a control
 * panel when you tap it.
 *
 * Plain Android views, same reasoning as [GameOverlayView] — this thing lives
 * inside another app's frame budget, so no Compose runtime here.
 */
class GameBubbleView(context: Context) : FrameLayout(context) {

    interface Listener {
        fun onBoost()
        /** @return the new DND state */
        fun onToggleDnd(): Boolean
        fun onBrightness(deltaPercent: Int)
        /** @return the new muted state */
        fun onToggleMute(): Boolean
        fun onEndSession()
    }

    var listener: Listener? = null
    var gameLabel: String = ""

    private val d get() = resources.displayMetrics.density
    private var windowManager: WindowManager? = null
    private var expanded = false
    private var lastTelemetry = GameTelemetry()
    private var lastStatus = ""

    private var bubbleFps: TextView? = null
    private var panelFps: TextView? = null
    private var panelTemp: TextView? = null
    private var panelBattery: TextView? = null
    private var panelTime: TextView? = null
    private var panelStatus: TextView? = null
    private var chipDnd: TextView? = null
    private var chipMute: TextView? = null

    private var startRawX = 0f
    private var startRawY = 0f
    private var startX = 0
    private var startY = 0
    private var dragging = false
    private var downAt = 0L

    init {
        render()
    }

    fun attach(wm: WindowManager) {
        windowManager = wm
    }

    fun isExpanded(): Boolean = expanded

    fun collapse() {
        if (!expanded) return
        expanded = false
        render()
    }

    // ------------------------------------------------------------- rendering --

    private fun render() {
        removeAllViews()
        if (expanded) addView(buildPanel()) else addView(buildBubble())
        apply(lastTelemetry)
        if (lastStatus.isNotEmpty()) panelStatus?.text = lastStatus
    }

    private fun buildBubble(): View {
        val size = dp(56)
        val root = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(size, size)
            background = circle(Color.argb(228, 8, 12, 10), ACCENT_SOFT)
        }
        val fps = TextView(context).apply {
            text = "0"
            textSize = 16f
            setTextColor(ACCENT)
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, dp(20), Gravity.TOP
            ).apply { topMargin = dp(11) }
        }
        val unit = TextView(context).apply {
            text = "FPS"
            textSize = 8f
            setTextColor(Color.argb(170, 255, 255, 255))
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM
            ).apply { bottomMargin = dp(9) }
        }
        root.addView(fps)
        root.addView(unit)
        root.setOnTouchListener(dragTouch)
        bubbleFps = fps
        return root
    }

    private fun buildPanel(): View {
        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(dp(PANEL_W), FrameLayout.LayoutParams.WRAP_CONTENT)
            background = rounded(18f, Color.argb(240, 7, 11, 10), ACCENT_SOFT)
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }

        // header (drag handle + close)
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(26))
        }
        header.addView(TextView(context).apply {
            text = "VENOM TURBO"
            textSize = 11f
            setTextColor(ACCENT)
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        })
        header.addView(TextView(context).apply {
            text = "✕"
            textSize = 15f
            setTextColor(Color.argb(190, 255, 255, 255))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(26), dp(26))
            setOnClickListener { collapse() }
        })
        header.setOnTouchListener(dragTouch)
        panel.addView(header)

        // game name
        panel.addView(TextView(context).apply {
            text = gameLabel.ifEmpty { "In game" }
            textSize = 12f
            setTextColor(Color.argb(200, 255, 255, 255))
            setSingleLine(true)
            layoutParams = LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
        })

        // stats row
        val stats = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(46))
            background = rounded(12f, Color.argb(140, 16, 22, 20), Color.argb(60, 180, 255, 57))
            gravity = Gravity.CENTER_VERTICAL
        }
        panelFps = statCell(stats, "FPS")
        panelTemp = statCell(stats, "TEMP")
        panelBattery = statCell(stats, "BAT")
        panelTime = statCell(stats, "TIME")
        panel.addView(stats)

        // action chips: row 1
        panel.addView(chipRow(
            chip("BOOST") {
                setStatus("Boosting…")
                listener?.onBoost()
            },
            chip("DND") { chip ->
                val on = listener?.onToggleDnd() ?: false
                paintChip(chip, on)
                setStatus(if (on) "Do not disturb on" else "Do not disturb off")
            }.also { chipDnd = it },
            chip("MUTE") { chip ->
                val muted = listener?.onToggleMute() ?: false
                paintChip(chip, muted)
                setStatus(if (muted) "Media muted" else "Sound on")
            }.also { chipMute = it },
        ))

        // action chips: row 2
        panel.addView(chipRow(
            chip("−10%") { listener?.onBrightness(-10) },
            chip("+10%") { listener?.onBrightness(10) },
            chip("END") {
                setStatus("Ending session…")
                listener?.onEndSession()
            },
        ))

        // status line
        panelStatus = TextView(context).apply {
            text = ""
            textSize = 10f
            setTextColor(Color.argb(160, 180, 255, 57))
            typeface = Typeface.MONOSPACE
            layoutParams = LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
            setSingleLine(true)
        }
        panel.addView(panelStatus)

        return panel
    }

    private fun statCell(parent: LinearLayout, label: String): TextView {
        val cell = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
        }
        val value = TextView(context).apply {
            text = "--"
            textSize = 15f
            setTextColor(Color.WHITE)
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
        }
        val caption = TextView(context).apply {
            text = label
            textSize = 8f
            setTextColor(Color.argb(150, 255, 255, 255))
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
        }
        cell.addView(value)
        cell.addView(caption)
        parent.addView(cell)
        return value
    }

    private fun chipRow(vararg chips: TextView): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
        }
        chips.forEach { chip ->
            row.addView(chip.apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(34), 1f).apply {
                    marginEnd = dp(6)
                }
            })
        }
        return row
    }

    private fun chip(text: String, onClick: (TextView) -> Unit): TextView {
        val view = TextView(context).apply {
            this.text = text
            textSize = 10f
            setTextColor(Color.argb(220, 255, 255, 255))
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            background = rounded(10f, Color.argb(120, 22, 30, 27), Color.argb(70, 180, 255, 57))
            isClickable = true
            isFocusable = false
        }
        view.setOnClickListener { onClick(view) }
        return view
    }

    private fun paintChip(view: TextView, active: Boolean) {
        view.background = rounded(
            10f,
            if (active) Color.argb(210, 180, 255, 57) else Color.argb(120, 22, 30, 27),
            Color.argb(70, 180, 255, 57)
        )
        view.setTextColor(if (active) Color.argb(255, 6, 10, 8) else Color.argb(220, 255, 255, 255))
    }

    private fun setStatus(text: String) {
        lastStatus = text
        panelStatus?.text = text
    }

    // ----------------------------------------------------------------- state --

    fun update(telemetry: GameTelemetry) {
        lastTelemetry = telemetry
        post { apply(telemetry) }
    }

    fun setDndState(on: Boolean) {
        post { chipDnd?.let { paintChip(it, on) } }
    }

    fun setMuteState(muted: Boolean) {
        post { chipMute?.let { paintChip(it, muted) } }
    }

    private fun apply(t: GameTelemetry) {
        bubbleFps?.text = t.fps.toString()
        panelFps?.text = if (t.fps > 0) t.fps.toString() else "--"
        val temp = t.batteryTempC
        panelTemp?.text = if (temp > 0f) "${temp.roundToInt()}°" else "--"
        panelTemp?.setTextColor(
            when {
                temp >= HOT_C -> HOT
                temp >= WARM_C -> WARM
                else -> Color.WHITE
            }
        )
        panelBattery?.text = if (t.batteryPercent > 0) "${t.batteryPercent}%" else "--"
        panelTime?.text = "${t.sessionMinutes}m"
    }

    // ------------------------------------------------------------------ drag --

    private val dragTouch = OnTouchListener { _, event ->
        val wm = windowManager
        val lp = layoutParams as? WindowManager.LayoutParams
        if (wm == null || lp == null) return@OnTouchListener false
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startRawX = event.rawX
                startRawY = event.rawY
                startX = lp.x
                startY = lp.y
                dragging = false
                downAt = System.currentTimeMillis()
                true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - startRawX
                val dy = event.rawY - startRawY
                if (!dragging && (abs(dx) > 6 * d || abs(dy) > 6 * d)) dragging = true
                if (dragging) {
                    lp.x = startX + dx.roundToInt()
                    lp.y = startY + dy.roundToInt()
                    runCatching { wm.updateViewLayout(this@GameBubbleView, lp) }
                }
                true
            }
            MotionEvent.ACTION_UP -> {
                val tapped = !dragging && System.currentTimeMillis() - downAt < 400
                if (tapped) {
                    expanded = !expanded
                    render()
                }
                true
            }
            else -> false
        }
    }

    // ---------------------------------------------------------------- shapes --

    private fun dp(value: Int): Int = (value * d).roundToInt()

    private fun circle(fill: Int, stroke: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(fill)
            setStroke((1.5f * d).roundToInt().coerceAtLeast(1), stroke)
        }

    private fun rounded(radiusDp: Float, fill: Int, stroke: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusDp * d
            setColor(fill)
            setStroke((1f * d).roundToInt().coerceAtLeast(1), stroke)
        }

    companion object {
        private const val PANEL_W = 236
        private const val HOT_C = 43f
        private const val WARM_C = 39f

        private val ACCENT = Color.rgb(180, 255, 57)
        private val ACCENT_SOFT = Color.argb(110, 180, 255, 57)
        private val WARM = Color.rgb(255, 196, 0)
        private val HOT = Color.rgb(255, 92, 92)
    }
}
