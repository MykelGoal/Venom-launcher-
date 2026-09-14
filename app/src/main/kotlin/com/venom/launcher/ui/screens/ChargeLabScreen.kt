package com.venom.launcher.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.venom.launcher.data.BatteryState
import com.venom.launcher.data.ChargeSample
import com.venom.launcher.data.ChargeSession
import com.venom.launcher.data.ChargeSpeed
import com.venom.launcher.ui.components.ChargeCurve
import com.venom.launcher.ui.components.StatTile
import com.venom.launcher.ui.components.VenomChargeBattery
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * **Venom Charge Lab** — the bit no other launcher ships.
 *
 * Live charging telemetry straight off BatteryManager, a battery caretaker that
 * nags you at a chosen percentage, a heat guard, a recorded charging curve, and
 * a history of past sessions.
 */
@Composable
fun ChargeLabScreen(
    battery: BatteryState,
    samples: List<ChargeSample>,
    sessions: List<ChargeSession>,
    limitEnabled: Boolean,
    limitPercent: Int,
    onLimitChanged: (Boolean, Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF04070A), Color(0xFF0A100C), Color(0xFF04070A))
                )
            ),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp),
        ) {
            item { Spacer(Modifier.statusBarsPadding()) }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CHARGE LAB",
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                        )
                        Text(
                            text = if (battery.isCharging) "Charging via ${battery.plugType.label}"
                            else "On battery",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                        )
                    }
                    androidx.compose.material3.TextButton(onClick = onBack) {
                        Text("Close", color = accent)
                    }
                }
            }

            // ---- the animation ----
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    VenomChargeBattery(
                        percent = battery.percent,
                        isCharging = battery.isCharging,
                        accent = accent,
                        modifier = Modifier
                            .size(width = 130.dp, height = 190.dp),
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        StatTile(
                            "Speed",
                            if (battery.isCharging) "${battery.speed.label} · ${"%.1f".format(battery.watts)}W"
                            else "—",
                            accent = accent,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        StatTile(
                            "Time to full",
                            battery.minutesToFull?.let { "${it / 60}h ${it % 60}m" } ?: "—",
                            modifier = Modifier.fillMaxWidth(),
                        )
                        StatTile(
                            "Temperature",
                            if (!battery.temperatureC.isNaN()) "${"%.1f".format(battery.temperatureC)}°C"
                            else "—",
                            accent = if (battery.isOverheating) Color(0xFFFF6B4A) else accent,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            // ---- telemetry ----
            item {
                SectionTitle("TELEMETRY")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatTile("Volts", "%.2f".format(battery.voltageV) + " V", modifier = Modifier.weight(1f))
                    StatTile("Current", "${battery.currentMa} mA", modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatTile(
                        "Charge now",
                        "${battery.chargedMah} mAh",
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        "Est. capacity",
                        battery.estimatedCapacityMah.takeIf { it > 0 }?.let { "$it mAh" } ?: "—",
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatTile("Health", battery.health, modifier = Modifier.weight(1f))
                    StatTile(
                        "Chemistry",
                        battery.technology.ifBlank { "—" },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // ---- caretaker ----
            item {
                SectionTitle("BATTERY CARETAKER")
                CaretakerCard(
                    enabled = limitEnabled,
                    percent = limitPercent,
                    accent = accent,
                    onChanged = onLimitChanged,
                )
            }

            // ---- curve ----
            item {
                SectionTitle("CHARGING CURVE")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(18.dp))
                        .padding(12.dp),
                ) {
                    ChargeCurve(
                        samples = samples,
                        accent = accent,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            // ---- history ----
            if (sessions.isNotEmpty()) {
                item { SectionTitle("RECENT SESSIONS") }
                items(sessions.take(12)) { session ->
                    SessionRow(session = session, accent = accent)
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White.copy(alpha = 0.35f),
        modifier = Modifier.padding(top = 22.dp, bottom = 10.dp),
    )
}

@Composable
private fun CaretakerCard(
    enabled: Boolean,
    percent: Int,
    accent: Color,
    onChanged: (Boolean, Int) -> Unit,
) {
    var sliderValue by remember(percent) { mutableFloatStateOf(percent.toFloat()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(20.dp))
            .background(accent.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Charge limit",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
                Text(
                    text = "Venom nudges you at ${sliderValue.toInt()}% so you can unplug before the stressful top end.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f),
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = { onChanged(it, sliderValue.toInt()) },
                colors = SwitchDefaults.colors(checkedThumbColor = accent),
            )
        }

        if (enabled) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "${sliderValue.toInt()}%",
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Monospace),
                color = accent,
            )
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { onChanged(true, sliderValue.toInt()) },
                valueRange = 40f..100f,
                steps = 11,
            )
        }
    }
}

@Composable
private fun SessionRow(session: ChargeSession, accent: Color) {
    val fmt = remember { SimpleDateFormat("d MMM · HH:mm", Locale.getDefault()) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.025f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fmt.format(Date(session.startedAt)),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
            )
            Text(
                text = "${session.plugType} · peak ${"%.1f".format(session.peakWatts)}W · ${"%.0f".format(session.peakTempC)}°C",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.45f),
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "+${session.gainedPercent}%",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                ),
                color = accent,
            )
            Text(
                text = "${session.durationMinutes} min",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.45f),
            )
        }
    }
}
