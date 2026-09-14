package com.venom.launcher.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.venom.launcher.data.BatteryState
import com.venom.launcher.data.PlugType
import kotlin.math.PI
import kotlin.math.sin

private const val BUBBLE_COUNT = 9

/**
 * The **Venom charging animation**: a liquid battery that fills to the current
 * level with a travelling wave, rising bubbles and a breathing glow while
 * plugged in. Drawn entirely on Canvas, so it costs one composable.
 */
@Composable
fun VenomChargeBattery(
    percent: Int,
    isCharging: Boolean,
    modifier: Modifier = Modifier,
    accent: Color = Color(0xFFB4FF39),
    showPercent: Boolean = true,
) {
    val infinite = rememberInfiniteTransition(label = "venom-charge")

    val wave by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing)
        ),
        label = "wave",
    )
    val bubblePhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4600, easing = LinearEasing)
        ),
        label = "bubbles",
    )
    val glow by infinite.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow",
    )
    val sweep by infinite.animateFloat(
        initialValue = -0.35f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = LinearEasing)
        ),
        label = "sweep",
    )

    val fill by animateFloatAsState(
        targetValue = (percent.coerceIn(0, 100) / 100f),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "fill",
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val nubH = h * 0.045f
            val bodyW = w * 0.66f
            val bodyH = h * 0.88f
            val left = (w - bodyW) / 2f
            val top = nubH + (h - nubH - bodyH) / 2f
            val radius = bodyW * 0.17f

            val bodyPath = Path().apply {
                addRoundRect(RoundRect(left, top, left + bodyW, top + bodyH, radius, radius))
            }

            // ---- outer glow while charging ----
            if (isCharging) {
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.30f * glow), Color.Transparent),
                        center = Offset(w / 2f, h / 2f),
                        radius = w * 0.62f,
                    ),
                    topLeft = Offset(left - w * 0.1f, top - h * 0.1f),
                    size = Size(bodyW + w * 0.2f, bodyH + h * 0.2f),
                    cornerRadius = CornerRadius(radius, radius),
                )
            }

            // ---- terminal nub ----
            drawRoundRect(
                color = accent.copy(alpha = 0.85f),
                topLeft = Offset(left + bodyW * 0.33f, top - nubH * 0.85f),
                size = Size(bodyW * 0.34f, nubH),
                cornerRadius = CornerRadius(nubH * 0.4f, nubH * 0.4f),
            )

            // ---- shell ----
            drawRoundRect(
                color = accent.copy(alpha = 0.10f),
                topLeft = Offset(left, top),
                size = Size(bodyW, bodyH),
                cornerRadius = CornerRadius(radius, radius),
            )

            // ---- liquid ----
            clipPath(bodyPath) {
                val liquidTop = top + bodyH * (1f - fill)
                val amp = bodyH * 0.022f
                val wavelength = bodyW / 2f

                val liquid = Path().apply {
                    moveTo(left, liquidTop)
                    var x = left
                    while (x <= left + bodyW) {
                        val t = ((x - left) / wavelength) * 2f * PI.toFloat()
                        val y = liquidTop + sin(t + wave * 2f * PI.toFloat()) * amp
                        lineTo(x, y)
                        x += 5f
                    }
                    lineTo(left + bodyW, top + bodyH)
                    lineTo(left, top + bodyH)
                    close()
                }

                drawPath(
                    path = liquid,
                    brush = Brush.verticalGradient(
                        colors = listOf(accent, accent.copy(alpha = 0.45f)),
                        startY = liquidTop,
                        endY = top + bodyH,
                    ),
                )

                // shimmer band that sweeps down the liquid while charging
                if (isCharging && fill > 0.02f) {
                    val bandH = bodyH * 0.16f
                    val bandTop = liquidTop + (bodyH * fill + bandH) * sweep - bandH
                    drawPath(
                        path = liquid,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.20f),
                                Color.Transparent,
                            ),
                            startY = bandTop,
                            endY = bandTop + bandH,
                        ),
                    )
                }

                // ---- bubbles ----
                if (isCharging) {
                    for (i in 0 until BUBBLE_COUNT) {
                        val seed = (i * 0.137f) % 1f
                        val phase = (bubblePhase + seed) % 1f
                        val bx = left + bodyW * ((seed * 0.86f) + 0.07f)
                        val by = (top + bodyH) - (phase * bodyH * fill)
                        val r = (bodyW * 0.018f) + (bodyW * 0.012f * ((i % 3) / 2f))
                        drawCircle(
                            color = Color.White.copy(alpha = 0.42f * (1f - phase)),
                            radius = r,
                            center = Offset(
                                bx + sin(phase * 6f + i) * bodyW * 0.015f,
                                by,
                            ),
                        )
                    }
                }
            }

            // ---- outline ----
            drawRoundRect(
                color = accent.copy(alpha = 0.85f),
                topLeft = Offset(left, top),
                size = Size(bodyW, bodyH),
                cornerRadius = CornerRadius(radius, radius),
                style = Stroke(width = (bodyW * 0.022f).coerceAtLeast(2f)),
            )

            // ---- charging bolt ----
            if (isCharging) {
                val cx = w / 2f
                val cy = top + bodyH * 0.5f
                val bh = bodyH * 0.26f
                val bw = bh * 0.62f
                val bolt = Path().apply {
                    moveTo(cx + bw * 0.16f, cy - bh * 0.5f)
                    lineTo(cx - bw * 0.42f, cy + bh * 0.10f)
                    lineTo(cx - bw * 0.02f, cy + bh * 0.10f)
                    lineTo(cx - bw * 0.18f, cy + bh * 0.5f)
                    lineTo(cx + bw * 0.44f, cy - bh * 0.12f)
                    lineTo(cx + bw * 0.02f, cy - bh * 0.12f)
                    close()
                }
                drawPath(bolt, Color(0xCC05070A))
                drawPath(
                    bolt,
                    Color.White,
                    style = Stroke(
                        width = (bodyW * 0.012f).coerceAtLeast(1.2f),
                        pathEffect = PathEffect.cornerPathEffect(4f),
                    ),
                )
            }
        }

        if (showPercent) {
            Text(
                text = "$percent%",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                color = if (fill > 0.55f) Color(0xFF06080A) else Color.White,
            )
        }
    }
}

/**
 * Full-screen charging screen. Venom can show this the moment power is
 * connected — think of it as an ambient charging animation you actually own.
 */
@Composable
fun ChargeOverlay(
    battery: BatteryState,
    accent: Color,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF04070A), Color(0xFF080D0A), Color(0xFF04070A))
                )
            )
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 28.dp),
        ) {
            Text(
                text = battery.plugType.label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = accent,
            )
            Spacer(Modifier.height(6.dp))

            VenomChargeBattery(
                percent = battery.percent,
                isCharging = battery.isCharging,
                accent = accent,
                modifier = Modifier
                    .width(210.dp)
                    .height(300.dp),
            )

            Spacer(Modifier.height(18.dp))

            Text(
                text = if (battery.isCharging && battery.minutesToFull != null) {
                    "Full in ${battery.minutesToFull / 60}h ${battery.minutesToFull % 60}m"
                } else if (battery.isCharging) {
                    "Charging…"
                } else {
                    "On battery"
                },
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )

            if (battery.isCharging) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${battery.speed.label} · ${"%.1f".format(battery.watts)}W · ${"%.0f".format(battery.temperatureC)}°C",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f),
                )
            }

            if (battery.isOverheating) {
                Spacer(Modifier.height(16.dp))
                HeatWarning(accent = Color(0xFFFF6B4A))
            }

            Spacer(Modifier.height(30.dp))
            Text(
                text = "tap anywhere to dismiss",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.32f),
            )
        }
    }
}

@Composable
fun HeatWarning(accent: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "🌡", fontSize = 14.sp)
        Text(
            text = "Battery is hot. Charging this warm wears the cell faster.",
            style = MaterialTheme.typography.labelSmall,
            color = accent,
        )
    }
}

/** Compact sparkline for the charging curve. */
@Composable
fun ChargeCurve(
    samples: List<com.venom.launcher.data.ChargeSample>,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    if (samples.size < 2) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = "Plug in to record a charging curve",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.35f),
            )
        }
        return
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minT = samples.first().t
        val maxT = samples.last().t
        val spanT = (maxT - minT).coerceAtLeast(1L)

        val path = Path()
        val fillPath = Path()
        samples.forEachIndexed { i, s ->
            val x = (s.t - minT).toFloat() / spanT * w
            val y = h - (s.percent / 100f) * h
            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, h)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(w, h)
        fillPath.close()

        drawPath(fillPath, Brush.verticalGradient(listOf(accent.copy(alpha = 0.28f), Color.Transparent)))
        drawPath(path, accent, style = Stroke(width = 3f))
    }
}

@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = Color.White,
) {
    Column(
        modifier = modifier
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.035f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.45f),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = accent,
        )
    }
}
