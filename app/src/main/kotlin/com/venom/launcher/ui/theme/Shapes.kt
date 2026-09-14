package com.venom.launcher.ui.theme

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/**
 * A true superellipse (Apple-style squircle). Rounded rectangle corners never
 * quite look right next to real icon shapes, so Venom uses this everywhere.
 */
class SquircleShape(private val exponent: Float = 4.2f) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = Path()
        val a = size.width / 2f
        val b = size.height / 2f
        val p = 2f / exponent
        val steps = 72

        for (i in 0..steps) {
            val t = (i.toFloat() / steps) * 2f * PI.toFloat()
            val ct = cos(t)
            val st = sin(t)
            val x = a + a * sign(ct) * abs(ct).pow(p)
            val y = b + b * sign(st) * abs(st).pow(p)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

val VenomSquircle = SquircleShape()

/** Hexagon mask, used by the "Hexagon" icon shape option. */
class PolygonShape(private val sides: Int, private val rotationDeg: Float = 0f) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.width / 2f
        for (i in 0 until sides) {
            val angle = Math.toRadians((360.0 / sides * i) + rotationDeg - 90.0)
            val x = cx + (r * cos(angle)).toFloat()
            val y = cy + (r * sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}
