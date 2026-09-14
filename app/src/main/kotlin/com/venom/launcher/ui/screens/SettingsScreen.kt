package com.venom.launcher.ui.screens

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.venom.launcher.data.Accent
import com.venom.launcher.data.AppInfo
import com.venom.launcher.data.DrawerSort
import com.venom.launcher.data.GestureAction
import com.venom.launcher.data.IconPackRef
import com.venom.launcher.data.IconShape
import com.venom.launcher.data.LauncherSettings

@Composable
fun SettingsScreen(
    settings: LauncherSettings,
    apps: List<AppInfo>,
    packs: List<IconPackRef>,
    hasUsageAccess: Boolean,
    isNotificationListenerEnabled: Boolean,
    isDeviceAdmin: Boolean,
    hasOverlayPermission: Boolean,
    modifier: Modifier = Modifier,
    onUpdate: ((LauncherSettings) -> LauncherSettings) -> Unit,
    onSetIconPack: (String?) -> Unit,
    onGridSize: (Int, Int) -> Unit,
    onPageCount: (Int) -> Unit,
    onResetLayout: () -> Unit,
    onRequestDefaultLauncher: () -> Unit,
    onRequestUsageAccess: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestDeviceAdmin: () -> Unit,
    onOpenChargeLab: () -> Unit,
    onOpenGames: () -> Unit,
    onToggleGameMode: (Boolean) -> Unit,
    onRequestOverlay: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onBack: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070B0E)),
    ) {
        LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp)) {
            item { Spacer(Modifier.statusBarsPadding()) }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("VENOM", style = MaterialTheme.typography.labelSmall, color = accent)
                        Text(
                            "Settings",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                        )
                    }
                    androidx.compose.material3.TextButton(onClick = onBack) {
                        Text("Done", color = accent)
                    }
                }
            }

            // -------------------------------------------------- look ----
            item { Section("LOOK") }
            item {
                Card {
                    Text("Accent", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Accent.entries.forEach { option ->
                            Chip(
                                label = option.label,
                                selected = settings.accent == option,
                                accent = Color(option.color),
                            ) { onUpdate { it.copy(accent = option) } }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    SettingToggle(
                        "Match system colours (Android 12+)",
                        settings.dynamicColor,
                    ) { onUpdate { it.copy(dynamicColor = it.dynamicColor.not()) } }
                }
            }

            item {
                Card {
                    Text("Icon shape", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        IconShape.entries.forEach { option ->
                            Chip(
                                label = option.label,
                                selected = settings.iconShape == option,
                                accent = accent,
                            ) { onUpdate { it.copy(iconShape = option) } }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    var scale by remember { mutableFloatStateOf(settings.iconScale) }
                    Text(
                        "Icon size · ${"%.2f".format(scale)}×",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f),
                    )
                    Slider(
                        value = scale,
                        onValueChange = { scale = it },
                        onValueChangeFinished = { onUpdate { it.copy(iconScale = scale) } },
                        valueRange = 0.7f..1.35f,
                    )
                    Spacer(Modifier.height(6.dp))
                    SettingToggle("Show labels", settings.showLabels) {
                        onUpdate { s -> s.copy(showLabels = !s.showLabels) }
                    }
                    SettingToggle("Show clock", settings.showClock) {
                        onUpdate { s -> s.copy(showClock = !s.showClock) }
                    }
                    SettingToggle("Show dock", settings.showDock) {
                        onUpdate { s -> s.copy(showDock = !s.showDock) }
                    }
                    SettingToggle("Show prediction row", settings.predictiveRow) {
                        onUpdate { s -> s.copy(predictiveRow = !s.predictiveRow) }
                    }
                    SettingToggle("App shortcuts on long-press", settings.appShortcutsEnabled) {
                        onUpdate { s -> s.copy(appShortcutsEnabled = !s.appShortcutsEnabled) }
                    }
                    SettingToggle("Notification dots", settings.notificationBadges) {
                        onUpdate { s -> s.copy(notificationBadges = !s.notificationBadges) }
                    }
                    SettingToggle("Haptics", settings.haptics) {
                        onUpdate { s -> s.copy(haptics = !s.haptics) }
                    }
                }
            }

            item {
                Card {
                    Text("Wallpaper", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    SettingToggle("Blur wallpaper", settings.wallpaperBlur) {
                        onUpdate { s -> s.copy(wallpaperBlur = !s.wallpaperBlur) }
                    }
                    var dim by remember { mutableFloatStateOf(settings.wallpaperDim) }
                    Text(
                        "Dim · ${"%.0f".format(dim * 100)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f),
                    )
                    Slider(
                        value = dim,
                        onValueChange = { dim = it },
                        onValueChangeFinished = { onUpdate { it.copy(wallpaperDim = dim) } },
                        valueRange = 0f..0.8f,
                    )
                }
            }

            // -------------------------------------------------- grid ----
            item { Section("HOME SCREEN") }
            item {
                Card {
                    StepperRow("Columns", settings.gridColumns, 3..7) { cols ->
                        onGridSize(cols, settings.gridRows)
                    }
                    StepperRow("Rows", settings.gridRows, 3..7) { rows ->
                        onGridSize(settings.gridColumns, rows)
                    }
                    StepperRow("Dock slots", settings.dockCount, 3..7) {
                        onUpdate { s -> s.copy(dockCount = it) }
                    }
                    StepperRow("Pages", 0, 1..9) {
                        onPageCount(it)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallAction("Reset home layout", accent, Modifier.weight(1f)) { onResetLayout() }
                    }
                }
            }

            // ------------------------------------------------ gestures ----
            item { Section("GESTURES") }
            item {
                Card {
                    GesturePicker("Swipe up", settings.swipeUp, accent) {
                        onUpdate { s -> s.copy(swipeUp = it) }
                    }
                    GesturePicker("Swipe down", settings.swipeDown, accent) {
                        onUpdate { s -> s.copy(swipeDown = it) }
                    }
                    GesturePicker("Swipe left on clock", settings.swipeLeft, accent) {
                        onUpdate { s -> s.copy(swipeLeft = it) }
                    }
                    GesturePicker("Swipe right on clock", settings.swipeRight, accent) {
                        onUpdate { s -> s.copy(swipeRight = it) }
                    }
                    GesturePicker("Double tap", settings.doubleTap, accent) {
                        onUpdate { s -> s.copy(doubleTap = it) }
                    }
                }
            }

            // -------------------------------------------------- charge ----
            item { Section("CHARGE LAB") }
            item {
                Card {
                    SettingToggle("Charging animation", settings.chargeAnimationEnabled) {
                        onUpdate { s -> s.copy(chargeAnimationEnabled = !s.chargeAnimationEnabled) }
                    }
                    SettingToggle("Full-screen overlay when plugged in", settings.chargeOverlayEnabled) {
                        onUpdate { s -> s.copy(chargeOverlayEnabled = !s.chargeOverlayEnabled) }
                    }
                    SettingToggle("Heat guard warning", settings.heatGuardEnabled) {
                        onUpdate { s -> s.copy(heatGuardEnabled = !s.heatGuardEnabled) }
                    }
                    Spacer(Modifier.height(8.dp))
                    SmallAction("Open Charge Lab", accent, Modifier.fillMaxWidth()) { onOpenChargeLab() }
                }
            }

            // --------------------------------------------------- gaming ----
            item { Section("GAMING") }
            item {
                Card {
                    SettingToggle("Game Mode", settings.gameModeEnabled) {
                        onToggleGameMode(!settings.gameModeEnabled)
                    }
                    Text(
                        "FPS + temperature HUD over any game, session tracking, and notifications pushed aside while you play.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.45f),
                    )
                    Spacer(Modifier.height(10.dp))
                    if (!hasOverlayPermission) {
                        SmallAction("Grant overlay permission", accent, Modifier.fillMaxWidth()) {
                            onRequestOverlay()
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    SettingToggle("Auto-detect games", settings.autoGameMode) {
                        onUpdate { s -> s.copy(autoGameMode = !s.autoGameMode) }
                    }
                    SettingToggle("Show FPS in HUD", settings.overlayShowFps) {
                        onUpdate { s -> s.copy(overlayShowFps = !s.overlayShowFps) }
                    }
                    SettingToggle("Show temperature in HUD", settings.overlayShowTemp) {
                        onUpdate { s -> s.copy(overlayShowTemp = !s.overlayShowTemp) }
                    }
                    SettingToggle("Show battery in HUD", settings.overlayShowBattery) {
                        onUpdate { s -> s.copy(overlayShowBattery = !s.overlayShowBattery) }
                    }
                    SettingToggle("Push notifications aside while gaming", settings.blockNotificationsWhileGaming) {
                        onUpdate { s -> s.copy(blockNotificationsWhileGaming = !s.blockNotificationsWhileGaming) }
                    }
                    Spacer(Modifier.height(10.dp))
                    SmallAction("Open Game Library", accent, Modifier.fillMaxWidth()) { onOpenGames() }
                }
            }

            // --------------------------------------------------- drawer ----
            item { Section("APP DRAWER") }
            item {
                Card {
                    Text("Sort", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DrawerSort.entries.forEach { option ->
                            Chip(
                                label = option.label,
                                selected = settings.drawerSort == option,
                                accent = accent,
                            ) { onUpdate { it.copy(drawerSort = option) } }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    StepperRow("Columns", settings.drawerColumns, 3..6) {
                        onUpdate { s -> s.copy(drawerColumns = it) }
                    }
                }
            }

            // -------------------------------------------------- icon pack --
            item { Section("ICON PACK") }
            item {
                Card {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Chip(
                            label = "System",
                            selected = settings.iconPack == null,
                            accent = accent,
                        ) { onSetIconPack(null) }
                        packs.forEach { pack ->
                            Chip(
                                label = pack.label,
                                selected = settings.iconPack == pack.packageName,
                                accent = accent,
                            ) { onSetIconPack(pack.packageName) }
                        }
                    }
                    if (packs.isEmpty()) {
                        Text(
                            "No icon packs installed. Any Nova/ADW/GO-style pack will show up here.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.4f),
                        )
                    }
                }
            }

            // --------------------------------------------------- hidden ----
            item { Section("HIDDEN APPS (${settings.hiddenApps.size})") }
            item {
                Card {
                    if (settings.hiddenApps.isEmpty()) {
                        Text(
                            "Nothing hidden. Long-press an app in the drawer to hide it.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.4f),
                        )
                    } else {
                        val hidden = apps.filter { it.componentKey in settings.hiddenApps }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            hidden.forEach { app ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onUpdate { s ->
                                                s.copy(hiddenApps = s.hiddenApps - app.componentKey)
                                            }
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        app.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text("Unhide", style = MaterialTheme.typography.labelSmall, color = accent)
                                }
                            }
                        }
                    }
                }
            }

            // --------------------------------------------------- system ----
            item { Section("SYSTEM") }
            item {
                Card {
                    SmallAction("Set Venom as default launcher", accent, Modifier.fillMaxWidth()) {
                        onRequestDefaultLauncher()
                    }
                    Spacer(Modifier.height(8.dp))
                    if (!hasUsageAccess) {
                        SmallAction("Grant Usage Access (powers the prediction row)", accent, Modifier.fillMaxWidth()) {
                            onRequestUsageAccess()
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    if (!isNotificationListenerEnabled) {
                        SmallAction("Grant notification access (powers dots)", accent, Modifier.fillMaxWidth()) {
                            onRequestNotifications()
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    if (!isDeviceAdmin) {
                        SmallAction("Enable device admin (powers double-tap lock)", accent, Modifier.fillMaxWidth()) {
                            onRequestDeviceAdmin()
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallAction("Export layout", accent, Modifier.weight(1f)) { onExport() }
                        SmallAction("Import layout", accent, Modifier.weight(1f)) { onImport() }
                    }
                }
            }

            // ---------------------------------------------------- about ----
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Venom Launcher",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                    Text(
                        "v0.1.0 · built for one device: yours",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.3f),
                    )
                }
            }

            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}

// ------------------------------------------------------------- building blocks

@Composable
private fun Section(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 22.dp, bottom = 8.dp),
    )
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) { content() }
}

@Composable
private fun Chip(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .border(
                1.dp,
                if (selected) accent.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.10f),
                RoundedCornerShape(14.dp),
            )
            .background(
                if (selected) accent.copy(alpha = 0.16f) else Color.Transparent,
                RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) accent else Color.White.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun SettingToggle(
    label: String,
    checked: Boolean,
    onChanged: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.88f),
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = { onChanged() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            ),
        )
    }
}

@Composable
private fun StepperRow(
    label: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.88f),
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(32.dp)
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                .clickable { (value - 1).coerceIn(range).let(onChange) },
            contentAlignment = Alignment.Center,
        ) { Text("−", color = Color.White) }
        Text(
            text = if (label == "Pages") "${pageCountLabel(value)}" else "$value",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 14.dp),
        )
        Box(
            modifier = Modifier
                .size(32.dp)
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                .clickable { (value + 1).coerceIn(range).let(onChange) },
            contentAlignment = Alignment.Center,
        ) { Text("+", color = Color.White) }
    }
}

private fun pageCountLabel(value: Int) = if (value == 0) "—" else "$value"

@Composable
private fun SmallAction(
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = accent,
        )
    }
}

@Composable
private fun GesturePicker(
    label: String,
    current: GestureAction,
    accent: Color,
    onPick: (GestureAction) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.88f),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = current.label,
                style = MaterialTheme.typography.labelSmall,
                color = accent,
            )
        }
        if (expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                GestureAction.entries.forEach { action ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (action == current) accent.copy(alpha = 0.5f)
                                else Color.White.copy(alpha = 0.08f),
                                RoundedCornerShape(12.dp),
                            )
                            .background(
                                if (action == current) accent.copy(alpha = 0.12f)
                                else Color.Transparent,
                                RoundedCornerShape(12.dp),
                            )
                            .clickable {
                                onPick(action)
                                expanded = false
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Text(
                            text = action.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (action == current) accent else Color.White.copy(alpha = 0.75f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun settingsContext(): Context = LocalContext.current
