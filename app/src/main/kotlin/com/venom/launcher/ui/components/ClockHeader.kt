package com.venom.launcher.ui.components

import android.content.Intent
import android.provider.AlarmClock
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.venom.launcher.data.BatteryState
import com.venom.launcher.data.ChargeSpeed
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
private val DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())

@Composable
fun ClockHeader(
    battery: BatteryState,
    modifier: Modifier = Modifier,
    onOpenChargeLab: () -> Unit = {},
) {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(60_000L - (now % 60_000L))
        }
    }

    val time = remember(now) { TIME_FORMATTER.format(LocalTime.now()) }
    val date = remember(now) { DATE_FORMATTER.format(LocalDate.now()) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 26.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable {
                    runCatching {
                        context.startActivity(
                            Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                },
        ) {
            Text(
                text = time,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 46.sp,
                    brush = Brush.verticalGradient(
                        listOf(Color.White, Color.White.copy(alpha = 0.82f))
                    ),
                ),
            )
            Text(
                text = date,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.62f),
            )
        }

        BatteryPill(
            battery = battery,
            modifier = Modifier.combinedClickable(
                onClick = onOpenChargeLab,
                onLongClick = onOpenChargeLab,
            ),
        )
    }
}

@Composable
fun BatteryPill(battery: BatteryState, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val tint = when {
        battery.isOverheating -> Color(0xFFFF6B4A)
        battery.isCharging -> accent
        battery.percent <= 15 -> Color(0xFFFF4D5E)
        else -> Color.White.copy(alpha = 0.7f)
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (battery.isCharging) {
                Text(
                    text = "⚡",
                    fontSize = 13.sp,
                    color = tint,
                )
                androidx.compose.foundation.layout.Spacer(
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }
            Text(
                text = "${battery.percent}%",
                style = MaterialTheme.typography.titleMedium,
                color = tint,
            )
        }
        if (battery.isCharging && battery.speed != ChargeSpeed.IDLE) {
            Text(
                text = "${battery.speed.label} · ${"%.1f".format(battery.watts)}W",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.55f),
            )
        } else if (!battery.isCharging && battery.minutesToEmpty != null) {
            Text(
                text = "${battery.minutesToEmpty / 60}h ${battery.minutesToEmpty % 60}m left",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.55f),
            )
        }
    }
}
