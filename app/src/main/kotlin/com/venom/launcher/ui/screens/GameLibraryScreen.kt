package com.venom.launcher.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.venom.launcher.data.GameInfo
import com.venom.launcher.data.GameProfile
import com.venom.launcher.data.GameSession
import com.venom.launcher.data.GameStats
import com.venom.launcher.data.GameTelemetry
import com.venom.launcher.ui.components.AppIcon
import com.venom.launcher.ui.components.AppOptionsSheet
import com.venom.launcher.ui.components.StatTile

/**
 * The gaming half of Venom: every installed game, how long you've actually
 * played it, and the live HUD readout when a session is running.
 */
@Composable
fun GameLibraryScreen(
    games: List<GameInfo>,
    sessions: List<GameSession>,
    telemetry: GameTelemetry,
    gamingPackage: String?,
    gameModeEnabled: Boolean,
    hasOverlayPermission: Boolean,
    onStatsFor: (String) -> GameStats,
    onBanner: (String) -> android.graphics.drawable.Drawable?,
    settings: com.venom.launcher.data.LauncherSettings,
    appsByKey: Map<String, com.venom.launcher.data.AppInfo>,
    packs: List<com.venom.launcher.data.IconPackRef>,
    badges: Map<String, Int>,
    modifier: Modifier = Modifier,
    profiles: Map<String, GameProfile> = emptyMap(),
    onProfileChange: (GameProfile) -> Unit = {},
    onLaunch: (GameInfo) -> Unit,
    onToggleGameMode: (Boolean) -> Unit,
    onRequestOverlay: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
) {
    var optionsGame by remember { mutableStateOf<GameInfo?>(null) }
    val accent = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF04070A), Color(0xFF0A0D14), Color(0xFF04070A))
                )
            ),
    ) {
        if (games.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("🎮", fontSize = 40.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    "No games detected",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Venom picks up anything the system flags as a game. Install one and it'll appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.45f),
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    "Back",
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                    modifier = Modifier.clickable(onClick = onBack),
                )
            }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp),
        ) {
            item { Spacer(Modifier.statusBarsPadding()) }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("GAME LIBRARY", style = MaterialTheme.typography.labelSmall, color = accent)
                        Text(
                            "${games.size} games",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                        )
                    }
                    androidx.compose.material3.TextButton(onClick = onOpenSettings) {
                        Text("Settings", color = accent)
                    }
                    androidx.compose.material3.TextButton(onClick = onBack) {
                        Text("Close", color = accent)
                    }
                }
            }

            // ---- live HUD card ----
            if (gamingPackage != null) {
                item { LiveSessionCard(telemetry = telemetry, accent = accent) }
            }

            // ---- game mode toggle ----
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(20.dp))
                        .background(accent.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                        .padding(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Game Mode",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                            )
                            Text(
                                if (hasOverlayPermission) {
                                    "FPS + temperature overlay, session tracking, notification blocking"
                                } else {
                                    "Needs 'draw over other apps' for the in-game HUD"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.5f),
                            )
                        }
                        androidx.compose.material3.Switch(
                            checked = gameModeEnabled,
                            onCheckedChange = onToggleGameMode,
                            colors = androidx.compose.material3.SwitchDefaults.colors(
                                checkedThumbColor = accent
                            ),
                        )
                    }
                    if (!hasOverlayPermission) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Grant overlay permission",
                            style = MaterialTheme.typography.labelLarge,
                            color = accent,
                            modifier = Modifier.clickable(onClick = onRequestOverlay),
                        )
                    }
                }
            }

            // ---- the grid ----
            val played = games.sortedByDescending { onStatsFor(it.packageName).totalMinutes }
            items(items = played, key = { it.packageName }) { game ->
                GameCard(
                    game = game,
                    stats = onStatsFor(game.packageName),
                    banner = onBanner(game.packageName),
                    accent = accent,
                    profile = profiles[game.packageName],
                    onProfileChange = onProfileChange,
                    onClick = { onLaunch(game) },
                    onLongPress = { optionsGame = game },
                )
            }

            item { Spacer(Modifier.height(24.dp)) }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }

        optionsGame?.let { game ->
            val app = appsByKey[game.componentKey]
            if (app != null) {
                AppOptionsSheet(
                    app = app,
                    shortcuts = emptyList(),
                    pack = null,
                    iconShape = settings.iconShape,
                    isHidden = false,
                    onLaunch = { onLaunch(game); optionsGame = null },
                    onAddToHome = { optionsGame = null },
                    onAddToDock = { optionsGame = null },
                    onAppInfo = { optionsGame = null },
                    onUninstall = { optionsGame = null },
                    onToggleHidden = { optionsGame = null },
                    onShortcut = { optionsGame = null },
                    onDismiss = { optionsGame = null },
                )
            } else {
                optionsGame = null
            }
        }
    }
}

@Composable
private fun LiveSessionCard(telemetry: GameTelemetry, accent: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .background(accent.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(accent, androidx.compose.foundation.shape.CircleShape),
            )
            Text(
                "SESSION RUNNING",
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                modifier = Modifier.padding(start = 8.dp),
            )
            Spacer(Modifier.weight(1f))
            Text(
                "${telemetry.sessionMinutes}m",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f),
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(
                "FPS now",
                "${telemetry.fps}",
                accent = if (telemetry.fps >= 55) accent else Color(0xFFFFD646),
                modifier = Modifier.weight(1f),
            )
            StatTile("Low", "${telemetry.minFps}", modifier = Modifier.weight(1f))
            StatTile("Frame", "${"%.1f".format(telemetry.frameMs)}ms", modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(
                "Battery",
                "${telemetry.batteryPercent}%",
                modifier = Modifier.weight(1f),
            )
            StatTile(
                "Temp",
                if (!telemetry.batteryTempC.isNaN()) "${"%.0f".format(telemetry.batteryTempC)}°C" else "—",
                accent = if (telemetry.batteryTempC >= 44f) Color(0xFFFF6B4A) else accent,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun GameCard(
    game: GameInfo,
    stats: GameStats,
    banner: android.graphics.drawable.Drawable?,
    accent: Color,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    profile: GameProfile? = null,
    onProfileChange: (GameProfile) -> Unit = {},
) {
    val base = profile ?: GameProfile.defaultFor(game.packageName)
    val boostOn = base.boostOnLaunch ?: true
    val dndOn = base.dndWhilePlaying ?: false
    val bitmap = remember(game.packageName) {
        runCatching {
            val d = banner ?: return@runCatching null
            val w = d.intrinsicWidth.takeIf { it > 0 } ?: 320
            val h = d.intrinsicHeight.takeIf { it > 0 } ?: 180
            val bmp = android.graphics.Bitmap.createBitmap(
                w.coerceAtMost(640), h.coerceAtMost(360), android.graphics.Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(bmp)
            d.setBounds(0, 0, bmp.width, bmp.height)
            d.draw(canvas)
            bmp
        }.getOrNull()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.035f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center,
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = game.label,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
            ) {
                Text(
                    text = game.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                val hours = stats.totalMinutes / 60
                val mins = stats.totalMinutes % 60
                Text(
                    text = if (stats.sessions == 0) "Not played yet"
                    else "${stats.sessions} sessions · ${if (hours > 0) "${hours}h ${mins}m" else "${mins}m"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f),
                )
                if (stats.sessions > 0) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = listOfNotNull(
                            "${stats.avgFps.toInt()} fps avg".takeIf { stats.avgFps > 0 },
                            "${stats.batteryDrained}% battery".takeIf { stats.batteryDrained > 0 },
                            "${"%.0f".format(stats.peakTempC)}°C peak".takeIf { stats.peakTempC > 0 },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = accent.copy(alpha = 0.85f),
                    )
                }
            }
            Text("▸", color = Color.White.copy(alpha = 0.4f), fontSize = 18.sp)
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProfileChip(
                label = "BOOST",
                active = boostOn,
                accent = accent,
                onClick = { onProfileChange(base.copy(boostOnLaunch = !boostOn)) },
            )
            ProfileChip(
                label = "DND",
                active = dndOn,
                accent = accent,
                onClick = { onProfileChange(base.copy(dndWhilePlaying = !dndOn)) },
            )
            ProfileChip(
                label = "MUTE",
                active = base.muteOnLaunch ?: false,
                accent = accent,
                onClick = { onProfileChange(base.copy(muteOnLaunch = !(base.muteOnLaunch ?: false))) },
            )
        }
    }
}

@Composable
private fun ProfileChip(
    label: String,
    active: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (active) accent.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.05f))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = if (active) Color(0xFF06080A) else Color.White.copy(alpha = 0.55f),
        )
    }
}
