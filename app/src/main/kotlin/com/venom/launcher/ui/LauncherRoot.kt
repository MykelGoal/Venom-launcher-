package com.venom.launcher.ui

import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.venom.launcher.data.AppInfo
import com.venom.launcher.data.GestureAction
import com.venom.launcher.data.LauncherItem
import com.venom.launcher.data.VenomBus
import com.venom.launcher.data.VenomJson
import com.venom.launcher.ui.components.AppOptionsSheet
import com.venom.launcher.ui.components.ChargeOverlay
import com.venom.launcher.ui.components.FolderDialog
import com.venom.launcher.ui.components.HomeMenuSheet
import com.venom.launcher.ui.components.AppIcon
import com.venom.launcher.ui.components.PickerSheet
import com.venom.launcher.ui.screens.ChargeLabScreen
import com.venom.launcher.ui.screens.DrawerScreen
import com.venom.launcher.ui.screens.GameLibraryScreen
import com.venom.launcher.ui.screens.HomeScreen
import com.venom.launcher.ui.screens.SettingsScreen
import com.venom.launcher.util.canDrawOverlays
import com.venom.launcher.util.expandNotifications
import com.venom.launcher.util.expandQuickSettings
import com.venom.launcher.util.isDeviceAdminActive
import com.venom.launcher.util.launchApp
import com.venom.launcher.util.lockScreen
import com.venom.launcher.util.openAppInfo
import com.venom.launcher.util.openNotificationListenerSettings
import com.venom.launcher.util.openUsageAccessSettings
import com.venom.launcher.util.openWallpaperPicker
import com.venom.launcher.util.requestOverlayPermission
import com.venom.launcher.util.requestDefaultLauncher
import com.venom.launcher.util.requestDeviceAdmin
import com.venom.launcher.util.requestUninstall
import com.venom.launcher.util.GameControls
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ceil

private enum class Screen { HOME, SETTINGS, CHARGE_LAB, STATS, GAMES }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LauncherRoot(vm: com.venom.launcher.vm.LauncherViewModel) {
    val context = LocalContext.current
    val view = androidx.compose.ui.platform.LocalView.current
    val configuration = LocalConfiguration.current
    val scope = rememberCoroutineScope()

    val settings by vm.settings.collectAsState()
    val layout by vm.layout.collectAsState()
    val apps by vm.apps.collectAsState()
    val pack by vm.iconPack.collectAsState()
    val packs by vm.packs.collectAsState()
    val battery by vm.battery.collectAsState()
    val samples by vm.chargeSamples.collectAsState()
    val sessions by vm.chargeSessions.collectAsState()
    val badges by vm.badges.collectAsState()
    val usage by vm.usage.collectAsState()
    val games by vm.games.collectAsState()
    val gameSessions by vm.gameSessions.collectAsState()
    val gameTelemetry by vm.gameTelemetry.collectAsState()
    val gamingPackage by vm.gamingPackage.collectAsState()
    val gameProfiles by vm.gameProfiles.collectAsState()

    val appsByKey = remember(apps) { apps.associateBy { it.componentKey } }
    val pageCount = layout.pages.size
    val pagerState = rememberPagerState(pageCount = { pageCount })

    var screen by remember { mutableStateOf(Screen.HOME) }
    var drawerOpen by remember { mutableStateOf(false) }
    var openFolder by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var optionsApp by remember { mutableStateOf<AppInfo?>(null) }
    var dockSlot by remember { mutableIntStateOf(-1) }
    var homeMenu by remember { mutableStateOf(false) }
    var widgetPicker by remember { mutableStateOf(false) }
    var statsOpen by remember { mutableStateOf(false) }
    var chargeDismissed by remember { mutableStateOf(false) }

    // -------------------------------------------------------- bus plumbing ----
    LaunchedEffect(Unit) {
        VenomBus.homePress.collect {
            when {
                drawerOpen -> drawerOpen = false
                openFolder != null -> openFolder = null
                optionsApp != null -> optionsApp = null
                screen != Screen.HOME -> screen = Screen.HOME
                else -> if (pagerState.currentPage != 0) {
                    pagerState.animateScrollToPage(0)
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        VenomBus.openChargeLab.collect {
            drawerOpen = false
            screen = Screen.CHARGE_LAB
        }
    }
    LaunchedEffect(Unit) {
        VenomBus.openGames.collect {
            drawerOpen = false
            screen = Screen.GAMES
        }
    }
    LaunchedEffect(battery.isCharging) {
        if (!battery.isCharging) chargeDismissed = false
    }

    // ------------------------------------------------------------- widgets ----
    var pendingWidget by remember { mutableStateOf<Pair<Int, AppWidgetProviderInfo>?>(null) }

    fun finishAddWidget(id: Int, info: AppWidgetProviderInfo) {
        val cols = settings.gridColumns
        val rows = settings.gridRows
        val cellW = configuration.screenWidthDp.toFloat() / cols
        val cellH = ((configuration.screenHeightDp - 320).coerceAtLeast(200).toFloat()) / rows
        val spanX = ceil(info.minWidth / cellW).toInt().coerceIn(1, cols)
        val spanY = ceil(info.minHeight / cellH).toInt().coerceIn(1, rows)

        val page = pagerState.currentPage
        val slot = layout.pages.getOrNull(page)?.indexOfFirst { it == null } ?: -1
        if (slot >= 0) {
            vm.addWidget(page, slot, id, spanX, spanY, cols)
        } else {
            vm.removeWidget(id)
            Toast.makeText(context, "No free space on this page", Toast.LENGTH_SHORT).show()
        }
    }

    // declared first because the bind-result handler can fall through to it
    val configureLauncherState = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val pending = pendingWidget ?: return@rememberLauncherForActivityResult
        pendingWidget = null
        finishAddWidget(pending.first, pending.second)
    }

    val bindLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val pending = pendingWidget
        if (pending != null) {
            val (id, info) = pending
            val configure = vm.configureWidgetIntent(id, info)
            if (configure != null) {
                pendingWidget = pending
                scope.launch {
                    runCatching { configureLauncherState.launch(configure) }
                }
            } else {
                finishAddWidget(id, info)
            }
        }
    }

    fun pickWidget(info: AppWidgetProviderInfo) {
        val id = vm.allocateWidgetId()
        if (id <= 0) return
        pendingWidget = id to info
        if (vm.bindWidgetIfAllowed(id, info)) {
            val configure = vm.configureWidgetIntent(id, info)
            if (configure != null) {
                scope.launch { runCatching { configureLauncherState.launch(configure) } }
            } else {
                pendingWidget = null
                finishAddWidget(id, info)
            }
        } else {
            bindLauncher.launch(vm.bindWidgetIntent(id, info))
        }
        widgetPicker = false
    }

    // ------------------------------------------------------ backup / restore ----
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val json = VenomJson.Json.encodeToString(layout)
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray())
                    }
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?.decodeToString()
                }.getOrNull()
            }
            if (!text.isNullOrBlank()) {
                val parsed = runCatching {
                    VenomJson.Json.decodeFromString<com.venom.launcher.data.HomeLayout>(text)
                }.getOrNull()
                if (parsed != null) {
                    vm.replaceLayout(parsed)
                    Toast.makeText(context, "Layout imported", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "That file isn't a Venom layout", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ---------------------------------------------------------------- actions ----
    fun haptic() {
        if (settings.haptics) {
            runCatching {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            }
        }
    }

    fun runGesture(action: GestureAction) {
        when (action) {
            GestureAction.NONE -> Unit
            GestureAction.OPEN_DRAWER -> drawerOpen = true
            GestureAction.SEARCH -> drawerOpen = true
            GestureAction.NOTIFICATIONS -> {
                haptic()
                if (!context.expandNotifications()) drawerOpen = true
            }

            GestureAction.QUICK_SETTINGS -> {
                haptic()
                if (!context.expandQuickSettings()) context.expandNotifications()
            }

            GestureAction.LOCK -> {
                haptic()
                if (!context.lockScreen()) {
                    Toast.makeText(
                        context,
                        "Enable device admin in Venom settings to use double-tap lock",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            GestureAction.CHARGE_LAB -> screen = Screen.CHARGE_LAB
            GestureAction.APP_STATS -> screen = Screen.STATS
            GestureAction.GAME_LIBRARY -> screen = Screen.GAMES
        }
    }

    fun launch(app: AppInfo) {
        haptic()
        context.launchApp(app)
        drawerOpen = false
    }

    fun launchGame(game: com.venom.launcher.data.GameInfo) {
        haptic()
        context.launchApp(
            com.venom.launcher.data.AppInfo(
                label = game.label,
                packageName = game.packageName,
                activityName = game.activityName,
                isSystemApp = false,
            )
        )
        if (settings.gameModeEnabled) {
            com.venom.launcher.service.GameModeService.start(context)
        }
    }

    // -------------------------------------------------------------------- UI ----
    Box(modifier = Modifier.fillMaxSize()) {
        HomeScreen(
            battery = battery,
            layout = layout,
            appsByKey = appsByKey,
            settings = settings,
            pack = pack,
            badges = if (settings.notificationBadges) badges else emptyMap(),
            predictions = remember(apps, usage, settings.predictiveRow) {
                vm.predictions()
            },
            pagerState = pagerState,
            onLaunchApp = { launch(it) },
            onOpenFolder = { page, slot -> openFolder = page to slot },
            onMove = { page, from, to -> vm.moveItem(page, from, to) },
            onCreateFolder = { page, from, to -> vm.createFolder(page, from, to) },
            onDropIntoFolder = { page, folderSlot, key ->
                vm.addToFolder(page, folderSlot, key)
            },
            onRemoveItem = { page, slot ->
                val item = layout.pages.getOrNull(page)?.getOrNull(slot)
                if (item is com.venom.launcher.data.WidgetItem) {
                    vm.removeWidget(item.widgetId)
                } else {
                    vm.removeItem(page, slot)
                }
            },
            onMissingWidget = { },
            onLongPressEmpty = { homeMenu = true },
            onDockLongPress = { slot -> dockSlot = slot },
            onSwipeUp = { runGesture(settings.swipeUp) },
            onSwipeDown = { runGesture(settings.swipeDown) },
            onSwipeLeft = { runGesture(settings.swipeLeft) },
            onSwipeRight = { runGesture(settings.swipeRight) },
            onDoubleTap = { runGesture(settings.doubleTap) },
            onOpenChargeLab = { screen = Screen.CHARGE_LAB },
        )

        // ---- app drawer ----
        AnimatedVisibility(
            visible = drawerOpen,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            DrawerScreen(
                apps = apps,
                settings = settings,
                pack = pack,
                badges = if (settings.notificationBadges) badges else emptyMap(),
                onLaunch = { launch(it) },
                onLongPress = { optionsApp = it },
                onOpenSettings = { screen = Screen.SETTINGS },
                onClose = { drawerOpen = false },
            )
        }

        // ---- charge overlay ----
        val showChargeOverlay = settings.chargeOverlayEnabled &&
            battery.isCharging &&
            !chargeDismissed &&
            screen == Screen.HOME &&
            !drawerOpen &&
            openFolder == null &&
            optionsApp == null &&
            !homeMenu &&
            !widgetPicker

        AnimatedVisibility(
            visible = showChargeOverlay,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            if (settings.chargeAnimationEnabled) {
                ChargeOverlay(
                    battery = battery,
                    accent = MaterialTheme.colorScheme.primary,
                    onDismiss = { chargeDismissed = true },
                )
            }
        }

        // ---- folders ----
        val folderRef = openFolder
        if (folderRef != null) {
            val (page, slot) = folderRef
            val folder = layout.pages.getOrNull(page)?.getOrNull(slot)
                as? com.venom.launcher.data.FolderItem
            if (folder != null) {
                FolderDialog(
                    folder = folder,
                    appsByKey = appsByKey,
                    pack = pack,
                    iconShape = settings.iconShape,
                    badges = badges,
                    onRename = { vm.renameFolder(page, slot, it) },
                    onLaunch = { app ->
                        launch(app)
                        openFolder = null
                    },
                    onRemoveFromFolder = { vm.removeFromFolder(page, slot, it) },
                    onDismiss = { openFolder = null },
                )
            } else {
                LaunchedEffect(folderRef) { openFolder = null }
            }
        }

        // ---- app options (drawer or dock) ----
        optionsApp?.let { app ->
            AppOptionsSheet(
                app = app,
                shortcuts = remember(app, settings.appShortcutsEnabled) { vm.shortcutsFor(app) },
                pack = pack,
                iconShape = settings.iconShape,
                isHidden = app.componentKey in settings.hiddenApps,
                onLaunch = { launch(app); optionsApp = null },
                onAddToHome = {
                    val page = pagerState.currentPage
                    val slot = layout.pages.getOrNull(page)?.indexOfFirst { it == null } ?: -1
                    if (slot >= 0) vm.placeApp(page, slot, app.componentKey)
                    optionsApp = null
                },
                onAddToDock = { vm.addToDock(app.componentKey); optionsApp = null },
                onAppInfo = { context.openAppInfo(app.packageName); optionsApp = null },
                onUninstall = { context.requestUninstall(app.packageName); optionsApp = null },
                onToggleHidden = { vm.toggleHidden(app.componentKey); optionsApp = null },
                onShortcut = { shortcut ->
                    vm.startShortcut(app.packageName, shortcut.id)
                    optionsApp = null
                },
                onDismiss = { optionsApp = null },
            )
        }

        if (dockSlot >= 0) {
            val dockItem = layout.dock.getOrNull(dockSlot)
            val dockApp = (dockItem as? com.venom.launcher.data.AppItem)
                ?.let { appsByKey[it.key] }
            if (dockApp != null) {
                AppOptionsSheet(
                    app = dockApp,
                    shortcuts = remember(dockApp) { vm.shortcutsFor(dockApp) },
                    pack = pack,
                    iconShape = settings.iconShape,
                    isHidden = dockApp.componentKey in settings.hiddenApps,
                    onLaunch = { launch(dockApp); dockSlot = -1 },
                    onAddToHome = {
                        val page = pagerState.currentPage
                        val slot = layout.pages.getOrNull(page)?.indexOfFirst { it == null } ?: -1
                        if (slot >= 0) vm.placeApp(page, slot, dockApp.componentKey)
                        dockSlot = -1
                    },
                    onAddToDock = {
                        // already in dock; treat as remove
                        vm.removeFromDock(dockSlot)
                        dockSlot = -1
                    },
                    onAppInfo = { context.openAppInfo(dockApp.packageName); dockSlot = -1 },
                    onUninstall = { context.requestUninstall(dockApp.packageName); dockSlot = -1 },
                    onToggleHidden = { vm.toggleHidden(dockApp.componentKey); dockSlot = -1 },
                    onShortcut = { vm.startShortcut(dockApp.packageName, it.id); dockSlot = -1 },
                    onDismiss = { dockSlot = -1 },
                )
            } else {
                LaunchedEffect(dockSlot) { dockSlot = -1 }
            }
        }

        // ---- home long-press menu ----
        if (homeMenu) {
            HomeMenuSheet(
                pageCount = pageCount,
                onWallpapers = { context.openWallpaperPicker(); homeMenu = false },
                onWidgets = { homeMenu = false; widgetPicker = true },
                onSettings = { homeMenu = false; screen = Screen.SETTINGS },
                onAddPage = { vm.setPageCount(pageCount + 1); homeMenu = false },
                onRemovePage = {
                    if (pageCount > 1) vm.setPageCount(pageCount - 1)
                    homeMenu = false
                },
                onDismiss = { homeMenu = false },
            )
        }

        // ---- widget picker ----
        if (widgetPicker) {
            val providers = vm.widgets.collectAsState().value
            PickerSheet(
                title = "Add widget",
                items = providers,
                selected = { false },
                labelOf = { info ->
                    val label = runCatching { info.loadLabel(context.packageManager) }.getOrNull()
                    "${label ?: "Widget"}  (${info.minWidth}×${info.minHeight})"
                },
                onPick = { pickWidget(it) },
                onDismiss = { widgetPicker = false },
            )
        }

        // ---- settings / charge lab / stats ----
        AnimatedVisibility(
            visible = screen == Screen.SETTINGS,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            SettingsScreen(
                settings = settings,
                apps = apps,
                packs = packs,
                hasUsageAccess = remember { vm.usageRepo.hasPermission() },
                isNotificationListenerEnabled = remember(context) { isNotificationListenerEnabled(context) },
                isDeviceAdmin = remember(context) { context.isDeviceAdminActive() },
                hasOverlayPermission = remember(context) { context.canDrawOverlays() },
                hasDndAccess = remember(context) { GameControls.hasDndAccess(context) },
                onRequestDnd = { GameControls.requestDndAccess(context) },
                onUpdate = { vm.update(it) },
                onSetIconPack = { vm.setIconPack(it) },
                onGridSize = { c, r -> vm.setGridSize(c, r) },
                onPageCount = { vm.setPageCount(it) },
                onResetLayout = { vm.resetLayout() },
                onRequestDefaultLauncher = { context.requestDefaultLauncher() },
                onRequestUsageAccess = { context.openUsageAccessSettings() },
                onRequestNotifications = { context.openNotificationListenerSettings() },
                onRequestDeviceAdmin = { context.requestDeviceAdmin() },
                onOpenChargeLab = { screen = Screen.CHARGE_LAB },
                onOpenGames = { screen = Screen.GAMES },
                onToggleGameMode = { vm.setGameMode(it) },
                onRequestOverlay = { context.requestOverlayPermission() },
                onExport = { exportLauncher.launch("venom-layout.json") },
                onImport = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                onBack = { screen = Screen.HOME },
            )
        }

        AnimatedVisibility(
            visible = screen == Screen.CHARGE_LAB,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            ChargeLabScreen(
                battery = battery,
                samples = samples,
                sessions = sessions,
                limitEnabled = settings.chargeLimitEnabled,
                limitPercent = settings.chargeLimitPercent,
                onLimitChanged = { enabled, percent -> vm.setChargeLimit(enabled, percent) },
                onBack = { screen = Screen.HOME },
            )
        }

        AnimatedVisibility(
            visible = screen == Screen.GAMES,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            GameLibraryScreen(
                games = games,
                sessions = gameSessions,
                telemetry = gameTelemetry,
                gamingPackage = gamingPackage,
                gameModeEnabled = settings.gameModeEnabled,
                hasOverlayPermission = remember(context) { context.canDrawOverlays() },
                profiles = gameProfiles,
                onProfileChange = { vm.setProfile(it) },
                onStatsFor = { vm.statsFor(it) },
                onBanner = { vm.gameBanner(it) },
                settings = settings,
                appsByKey = appsByKey,
                packs = packs,
                badges = badges,
                onLaunch = { launchGame(it) },
                onToggleGameMode = { vm.setGameMode(it) },
                onRequestOverlay = { context.requestOverlayPermission() },
                onOpenSettings = { screen = Screen.SETTINGS },
                onBack = { screen = Screen.HOME },
            )
        }

        AnimatedVisibility(
            visible = screen == Screen.STATS,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            AppStatsScreen(
                usage = usage,
                apps = apps,
                pack = pack,
                accent = MaterialTheme.colorScheme.primary,
                onBack = { screen = Screen.HOME },
            )
        }
    }

    // ---------------------------------------------------------------- back ----
    BackHandler(enabled = drawerOpen || openFolder != null || optionsApp != null ||
        homeMenu || widgetPicker || screen != Screen.HOME) {
        when {
            widgetPicker -> widgetPicker = false
            homeMenu -> homeMenu = false
            optionsApp != null -> optionsApp = null
            openFolder != null -> openFolder = null
            drawerOpen -> drawerOpen = false
            screen != Screen.HOME -> screen = Screen.HOME
        }
    }
}

private fun isNotificationListenerEnabled(context: Context): Boolean {
    val flat = Settings.Secure.getString(
        context.contentResolver, "enabled_notification_listeners"
    ) ?: return false
    return flat.split(':').any { it.contains(context.packageName) }
}

@Composable
private fun AppStatsScreen(
    usage: Map<String, com.venom.launcher.data.UsageRepository.UsageRow>,
    apps: List<AppInfo>,
    pack: com.venom.launcher.data.IconPack?,
    accent: Color,
    onBack: () -> Unit,
) {
    val byPackage = remember(apps) {
        LinkedHashMap<String, AppInfo>().apply {
            for (a in apps) putIfAbsent(a.packageName, a)
        }
    }
    val rows = remember(usage, apps) {
        usage.values
            .mapNotNull { row -> byPackage[row.packageName]?.let { it to row.totalMillis } }
            .sortedByDescending { it.second }
            .take(20)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B0E)),
    ) {
        LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp)) {
            item { Spacer(Modifier.statusBarsPadding()) }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("APP STATS", style = MaterialTheme.typography.labelSmall, color = accent)
                        Text(
                            "Last 7 days",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                        )
                    }
                    androidx.compose.material3.TextButton(onClick = onBack) {
                        Text("Close", color = accent)
                    }
                }
            }
            if (rows.isEmpty()) {
                item {
                    Text(
                        "Grant Usage Access in Settings to see screen time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.45f),
                    )
                }
            } else {
                items(rows) { (app, millis) ->
                    val minutes = millis / 60_000
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIcon(
                            app = app,
                            pack = pack,
                            shape = com.venom.launcher.data.IconShape.SQUIRCLE,
                            size = 36.dp,
                        )
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp),
                        )
                        Text(
                            text = if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m",
                            style = MaterialTheme.typography.titleMedium,
                            color = accent,
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}
