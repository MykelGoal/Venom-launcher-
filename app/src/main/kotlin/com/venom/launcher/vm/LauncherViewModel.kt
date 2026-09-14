package com.venom.launcher.vm

import android.app.Application
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.Build
import android.os.Process
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.venom.launcher.VenomApp
import com.venom.launcher.data.AppInfo
import com.venom.launcher.data.AppItem
import com.venom.launcher.data.AppRepository
import com.venom.launcher.data.BatteryState
import com.venom.launcher.data.ChargeRepository
import com.venom.launcher.data.ChargeSample
import com.venom.launcher.data.ChargePrefs
import com.venom.launcher.data.ChargeSession
import com.venom.launcher.data.FolderItem
import com.venom.launcher.data.GameInfo
import com.venom.launcher.data.GameRepository
import com.venom.launcher.data.GameSession
import com.venom.launcher.data.GameStats
import com.venom.launcher.data.GameProfile
import com.venom.launcher.data.GameStore
import com.venom.launcher.data.GameTelemetry
import com.venom.launcher.data.HomeLayout
import com.venom.launcher.data.IconPack
import com.venom.launcher.data.IconPackManager
import com.venom.launcher.data.IconPackRef
import com.venom.launcher.data.LauncherPrefs
import com.venom.launcher.data.LauncherSettings
import com.venom.launcher.data.UsageRepository
import com.venom.launcher.data.VenomBus
import com.venom.launcher.data.VenomWidgetHost
import com.venom.launcher.data.WidgetItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.ceil

class LauncherViewModel(private val app: Application) : AndroidViewModel(app) {

    private val prefs = LauncherPrefs(app)
    private val appRepo = AppRepository(app)
    val usageRepo = UsageRepository(app)
    private val charge: ChargeRepository = (app as VenomApp).chargeRepository
    private val gameRepo = GameRepository(app)
    private val gameStore = GameStore(app)

    // ------------------------------------------------------------- streams ----

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    val settings: StateFlow<LauncherSettings> = prefs.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, LauncherSettings())

    val layout: StateFlow<HomeLayout> = prefs.layout
        .stateIn(viewModelScope, SharingStarted.Eagerly, HomeLayout())

    private val _iconPack = MutableStateFlow<IconPack?>(null)
    val iconPack: StateFlow<IconPack?> = _iconPack.asStateFlow()

    private val _packs = MutableStateFlow<List<IconPackRef>>(emptyList())
    val packs: StateFlow<List<IconPackRef>> = _packs.asStateFlow()

    private val _usage = MutableStateFlow<Map<String, UsageRepository.UsageRow>>(emptyMap())
    val usage: StateFlow<Map<String, UsageRepository.UsageRow>> = _usage.asStateFlow()

    private val _hourly = MutableStateFlow<Map<String, IntArray>>(emptyMap())
    val hourly: StateFlow<Map<String, IntArray>> = _hourly.asStateFlow()

    private val _widgets = MutableStateFlow<List<AppWidgetProviderInfo>>(emptyList())
    val widgets: StateFlow<List<AppWidgetProviderInfo>> = _widgets.asStateFlow()

    private val _games = MutableStateFlow<List<GameInfo>>(emptyList())
    val games: StateFlow<List<GameInfo>> = _games.asStateFlow()

    val gameSessions: StateFlow<List<GameSession>> = gameStore.sessions
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val gameTelemetry: StateFlow<GameTelemetry> = VenomBus.gameTelemetry
    val gamingPackage: StateFlow<String?> = VenomBus.gamingPackage

    val gameProfiles: StateFlow<Map<String, GameProfile>> = gameStore.profiles
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val _isDefaultLauncher = MutableStateFlow(false)
    val isDefaultLauncher: StateFlow<Boolean> = _isDefaultLauncher.asStateFlow()

    // Charge Lab
    val battery: StateFlow<BatteryState> = charge.state
    val chargeSessions: StateFlow<List<ChargeSession>> = charge.sessions
    val chargeSamples: StateFlow<List<ChargeSample>> = charge.samples
    val chargeLimitHit: StateFlow<Boolean> = charge.limitHit

    val badges: StateFlow<Map<String, Int>> = VenomBus.badges

    init {
        viewModelScope.launch { reloadApps() }

        viewModelScope.launch {
            settings.map { it.iconPack }.distinctUntilChanged().collect { pkg ->
                _iconPack.value = IconPackManager.load(app, pkg)
            }
        }
        viewModelScope.launch {
            _packs.value = IconPackManager.listPacks(app)
        }
        viewModelScope.launch {
            VenomBus.appReloads.collect { reloadApps() }
        }
        viewModelScope.launch { refreshWidgetProviders() }
    }

    fun onResume() {
        viewModelScope.launch { reloadApps() }
        viewModelScope.launch {
            _isDefaultLauncher.value = isDefaultHomeApp()
        }
    }

    // ---------------------------------------------------------------- apps ----

    suspend fun reloadApps() {
        val loaded = appRepo.loadApps()
        _apps.value = loaded
        ensureFirstRun(loaded)
        _games.value = gameRepo.loadGames()
        if (usageRepo.hasPermission()) {
            _usage.value = usageRepo.loadUsage(days = 7)
            _hourly.value = usageRepo.loadHourlyAffinity(days = 14)
        }
    }

    private suspend fun ensureFirstRun(loaded: List<AppInfo>) {
        val s = prefs.currentSettings()
        if (s.firstRunDone) return

        val byPkg = loaded.associateBy { it.packageName }
        val dock = DOCK_GROUPS.mapNotNull { group -> group.firstNotNullOfOrNull { byPkg[it] } }
            .take(s.dockCount)
            .map { AppItem(newId("a"), it.componentKey) }

        val preferred = HOME_CANDIDATES.mapNotNull { byPkg[it] }
        val rest = loaded.filter { it.packageName !in HOME_CANDIDATES_SET }
        val home = (preferred + rest)
            .take(s.cellsPerPage)
            .map { AppItem(newId("a"), it.componentKey) }

        prefs.updateLayout { HomeLayout(pages = listOf(home), dock = dock) }
        prefs.update { it.copy(firstRunDone = true) }
    }

    // -------------------------------------------------------------- gaming ----

    fun statsFor(packageName: String): GameStats =
        gameRepo.statsFor(gameSessions.value, packageName)

    fun gameBanner(packageName: String): android.graphics.drawable.Drawable? =
        gameRepo.banner(packageName)

    /** Flipping this starts or stops the monitoring service. */
    fun setProfile(profile: GameProfile) {
        viewModelScope.launch { runCatching { gameStore.saveProfile(profile) } }
    }

    fun setGameMode(enabled: Boolean) {
        update { it.copy(gameModeEnabled = enabled) }
        if (enabled) {
            com.venom.launcher.service.GameModeService.start(app)
        } else {
            com.venom.launcher.service.GameModeService.stop(app)
        }
    }

    fun clearGameHistory() {
        viewModelScope.launch { runCatching { gameStore.clear() } }
    }

    fun appByKey(key: String): AppInfo? =
        _apps.value.firstOrNull { it.componentKey == key }

    // -------------------------------------------------------------- layout ----

    fun placeApp(pageIndex: Int, slot: Int, key: String) = layout {
        val pages = it.pages.toMutableList()
        val page = pages.getOrNull(pageIndex)?.toMutableList() ?: return@layout it
        if (slot !in page.indices) return@layout it
        when (val target = page[slot]) {
            null -> page[slot] = AppItem(newId("a"), key)
            is FolderItem -> page[slot] = target.copy(items = (target.items + key).distinct())
            else -> return@layout it
        }
        pages[pageIndex] = page
        it.copy(pages = pages)
    }

    fun moveItem(pageIndex: Int, from: Int, to: Int) = layout {
        val pages = it.pages.toMutableList()
        val page = pages.getOrNull(pageIndex)?.toMutableList() ?: return@layout it
        if (from !in page.indices || to !in page.indices || from == to) return@layout it
        val a = page[from] ?: return@layout it
        val b = page[to]
        page[to] = a
        page[from] = b
        pages[pageIndex] = page
        it.copy(pages = pages)
    }

    fun moveItemToPage(fromPage: Int, fromSlot: Int, toPage: Int, toSlot: Int) = layout {
        val pages = it.pages.map { p -> p.toMutableList() }.toMutableList()
        val src = pages.getOrNull(fromPage) ?: return@layout it
        val dst = pages.getOrNull(toPage) ?: return@layout it
        if (fromSlot !in src.indices || toSlot !in dst.indices) return@layout it
        val item = src[fromSlot] ?: return@layout it
        if (dst[toSlot] != null) return@layout it
        src[fromSlot] = null
        dst[toSlot] = item
        it.copy(pages = pages.map { p -> p.toList() })
    }

    /** Drop one icon on top of another to make a folder. */
    fun createFolder(pageIndex: Int, from: Int, to: Int) = layout {
        val pages = it.pages.toMutableList()
        val page = pages.getOrNull(pageIndex)?.toMutableList() ?: return@layout it
        if (from !in page.indices || to !in page.indices) return@layout it
        val a = page[from] as? AppItem ?: return@layout it
        when (val b = page[to]) {
            null -> return@layout it
            is FolderItem -> {
                page[to] = b.copy(items = (b.items + a.key).distinct())
                page[from] = null
            }

            is AppItem -> {
                page[to] = FolderItem(
                    id = newId("f"),
                    label = "Folder",
                    items = listOf(b.key, a.key),
                )
                page[from] = null
            }

            else -> return@layout it
        }
        pages[pageIndex] = page
        it.copy(pages = pages)
    }

    fun renameFolder(pageIndex: Int, slot: Int, label: String) = layout {
        val pages = it.pages.toMutableList()
        val page = pages.getOrNull(pageIndex)?.toMutableList() ?: return@layout it
        val f = page.getOrNull(slot) as? FolderItem ?: return@layout it
        page[slot] = f.copy(label = label)
        pages[pageIndex] = page
        it.copy(pages = pages)
    }

    fun removeFromFolder(pageIndex: Int, slot: Int, key: String) = layout {
        val pages = it.pages.toMutableList()
        val page = pages.getOrNull(pageIndex)?.toMutableList() ?: return@layout it
        val f = page.getOrNull(slot) as? FolderItem ?: return@layout it
        val items = f.items - key
        page[slot] = if (items.size <= 1) {
            items.firstOrNull()?.let { AppItem(newId("a"), it) }
        } else {
            f.copy(items = items)
        }
        pages[pageIndex] = page
        it.copy(pages = pages)
    }

    /** Drop an app into an existing folder (used by drag-and-drop). */
    fun addToFolder(pageIndex: Int, folderSlot: Int, key: String) = layout {
        val pages = it.pages.toMutableList()
        val page = pages.getOrNull(pageIndex)?.toMutableList() ?: return@layout it
        val folder = page.getOrNull(folderSlot) as? FolderItem ?: return@layout it
        page[folderSlot] = folder.copy(items = (folder.items + key).distinct())
        pages[pageIndex] = page
        it.copy(pages = pages)
    }

    /** Used by Settings -> Import layout. */
    fun replaceLayout(next: HomeLayout) {
        viewModelScope.launch {
            prefs.updateLayout { next }
        }
        viewModelScope.launch { reloadApps() }
    }

    fun removeItem(pageIndex: Int, slot: Int) = layout {
        val pages = it.pages.toMutableList()
        val page = pages.getOrNull(pageIndex)?.toMutableList() ?: return@layout it
        if (slot !in page.indices) return@layout it
        page[slot] = null
        pages[pageIndex] = page
        it.copy(pages = pages)
    }

    fun addToDock(key: String) = layout {
        val dock = it.dock.toMutableList()
        val s = settings.value
        while (dock.size < s.dockCount) dock.add(null)
        val free = dock.indexOfFirst { item -> item == null }
        if (free == -1) return@layout it
        dock[free] = AppItem(newId("a"), key)
        it.copy(dock = dock)
    }

    fun removeFromDock(slot: Int) = layout {
        val dock = it.dock.toMutableList()
        if (slot !in dock.indices) return@layout it
        dock[slot] = null
        it.copy(dock = dock)
    }

    fun setPageCount(count: Int) = layout {
        val target = count.coerceIn(1, 9)
        val cells = settings.value.cellsPerPage
        val pages = it.pages.toMutableList()
        while (pages.size < target) pages.add(List(cells) { null })
        val trimmed = pages.take(target).map { p ->
            val q = p.toMutableList()
            while (q.size < cells) q.add(null)
            q.take(cells)
        }
        it.copy(pages = trimmed)
    }

    fun setGridSize(cols: Int, rows: Int) {
        viewModelScope.launch {
            prefs.update { it.copy(gridColumns = cols, gridRows = rows) }
            prefs.updateLayout { l ->
                val cells = (cols * rows).coerceAtLeast(1)
                val all = l.pages.flatten().filterNotNull()
                val pages = all.chunked(cells)
                    .map { chunk -> chunk + List(cells - chunk.size) { null } }
                    .ifEmpty { listOf(List(cells) { null }) }
                l.copy(pages = pages)
            }
        }
    }

    fun resetLayout() {
        viewModelScope.launch {
            prefs.resetLayout()
            prefs.update { it.copy(firstRunDone = false) }
            reloadApps()
        }
    }

    // ------------------------------------------------------------- widgets ----

    fun refreshWidgetProviders() {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                runCatching {
                    AppWidgetManager.getInstance(app).installedProviders
                }.getOrDefault(emptyList())
            }.let { _widgets.value = it }
        }
    }

    fun allocateWidgetId(): Int =
        runCatching { VenomWidgetHost.get(app).allocateAppWidgetId() }.getOrDefault(-1)

    fun bindWidgetIfAllowed(id: Int, info: AppWidgetProviderInfo): Boolean {
        val awm = AppWidgetManager.getInstance(app)
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                awm.bindAppWidgetIdIfAllowed(id, Process.myUserHandle(), info.provider, null)
            } else {
                @Suppress("DEPRECATION")
                awm.bindAppWidgetIdIfAllowed(id, info.provider)
            }
        }.getOrDefault(false)
    }

    fun bindWidgetIntent(id: Int, info: AppWidgetProviderInfo): Intent =
        Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
        }

    fun configureWidgetIntent(id: Int, info: AppWidgetProviderInfo): Intent? {
        val configure = info.configure ?: return null
        return Intent().apply {
            component = configure
            action = AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Reserves a spanX x spanY block of cells for a widget, evicting whatever was
     * underneath into the next free slots.
     */
    fun addWidget(
        pageIndex: Int,
        slot: Int,
        widgetId: Int,
        spanX: Int,
        spanY: Int,
        cols: Int,
    ) = layout {
        val pages = it.pages.toMutableList()
        val page = pages.getOrNull(pageIndex)?.toMutableList() ?: return@layout it
        if (slot !in page.indices) return@layout it

        val sx = spanX.coerceIn(1, cols)
        val sy = spanY.coerceAtLeast(1)
        val col = slot % cols
        val row = slot / cols
        val startCol = (col + sx - 1).coerceAtMost(cols - sx).coerceAtLeast(0)
        val startRow = (row + sy - 1).coerceAtMost((page.size / cols) - sy).coerceAtLeast(0)
        val origin = startRow * cols + startCol

        val evicted = mutableListOf<com.venom.launcher.data.LauncherItem>()
        for (r in startRow until (startRow + sy)) {
            for (c in startCol until (startCol + sx)) {
                val idx = r * cols + c
                if (idx in page.indices) {
                    page[idx]?.let { item -> evicted.add(item) }
                    page[idx] = null
                }
            }
        }
        // re-home anything we displaced
        for (item in evicted) {
            val free = page.indexOfFirst { it == null }
            if (free >= 0) page[free] = item
        }

        page[origin] = WidgetItem(newId("w"), widgetId, sx, sy)
        pages[pageIndex] = page
        it.copy(pages = pages)
    }

    fun removeWidget(widgetId: Int) {
        viewModelScope.launch {
            runCatching { VenomWidgetHost.get(app).deleteAppWidgetId(widgetId) }
        }
        layout {
            val s = settings.value
            it.copy(
                pages = it.pages.map { page ->
                    page.map { item ->
                        if (item is WidgetItem && item.widgetId == widgetId) null else item
                    }
                },
                dock = it.dock.map { item ->
                    if (item is WidgetItem && item.widgetId == widgetId) null else item
                }.let { d -> if (s.dockCount >= d.size) d else d }
            )
        }
    }

    // ------------------------------------------------------------ shortcuts ----

    private fun launcherApps(): LauncherApps? =
        app.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps

    fun hasShortcutHostPermission(): Boolean = runCatching {
        launcherApps()?.hasShortcutHostPermission()
    }.getOrDefault(false) ?: false

    fun shortcutsFor(appInfo: AppInfo): List<ShortcutInfo> {
        if (!settings.value.appShortcutsEnabled) return emptyList()
        val la = launcherApps() ?: return emptyList()
        if (!hasShortcutHostPermission()) return emptyList()
        val query = LauncherApps.ShortcutQuery().apply {
            setPackage(appInfo.packageName)
            setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
            )
        }
        return runCatching {
            la.getShortcuts(query, Process.myUserHandle())?.take(6)
        }.getOrDefault(emptyList()) ?: emptyList()
    }

    fun startShortcut(packageName: String, shortcutId: String) {
        runCatching {
            launcherApps()?.startShortcut(packageName, shortcutId, null, null, Process.myUserHandle())
        }
    }

    // ---------------------------------------------------------- predictions ----

    /**
     * Time-aware "you probably want this next" row.
     *
     * Builds a per-app hourly histogram from UsageEvents, then ranks apps by how
     * much of their usage happens in the current hour of day, with a small
     * overall-usage tiebreaker. Falls back to plain recency when there isn't
     * enough signal yet.
     */
    fun predictions(now: Long = System.currentTimeMillis(), limit: Int = 6): List<AppInfo> {
        if (!settings.value.predictiveRow) return emptyList()
        val allApps = _apps.value
        if (allApps.isEmpty()) return emptyList()

        val hour = Calendar.getInstance().apply { timeInMillis = now }
            .get(Calendar.HOUR_OF_DAY)

        val byPackage = LinkedHashMap<String, AppInfo>()
        for (a in allApps) byPackage.putIfAbsent(a.packageName, a)

        val histogram = _hourly.value
        if (histogram.isNotEmpty()) {
            val scored = histogram.mapNotNull { (pkg, counts) ->
                val app = byPackage[pkg] ?: return@mapNotNull null
                val total = counts.sum().coerceAtLeast(1)
                val affinity = counts[hour].toFloat() / total
                val volume = (total.toFloat() / (total + 40f)) * 0.35f
                app to (affinity + volume)
            }
                .sortedByDescending { it.second }
                .take(limit)
                .map { it.first }
            if (scored.size >= limit) return scored
            if (scored.size >= 3) return scored
        }

        return _usage.value.values
            .sortedByDescending { it.lastUsed }
            .mapNotNull { byPackage[it.packageName] }
            .take(limit)
    }

    // -------------------------------------------------------------- charge ----

    fun setChargeLimit(enabled: Boolean, percent: Int) {
        viewModelScope.launch {
            ChargePrefs.set(app, enabled, percent.coerceIn(40, 100))
            prefs.update {
                it.copy(chargeLimitEnabled = enabled, chargeLimitPercent = percent.coerceIn(40, 100))
            }
        }
    }

    fun acknowledgeChargeLimit() = charge.acknowledgeLimit()

    // ------------------------------------------------------------- settings ----

    fun update(transform: (LauncherSettings) -> LauncherSettings) {
        viewModelScope.launch { prefs.update(transform) }
    }

    fun toggleHidden(key: String) {
        update { s ->
            val next = if (key in s.hiddenApps) s.hiddenApps - key else s.hiddenApps + key
            s.copy(hiddenApps = next)
        }
    }

    fun setIconPack(packageName: String?) {
        update { it.copy(iconPack = packageName) }
    }

    private fun isDefaultHomeApp(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = app.packageManager.resolveActivity(intent, 0)
        return resolveInfo?.activityInfo?.packageName == app.packageName
    }

    private fun layout(transform: (HomeLayout) -> HomeLayout) {
        viewModelScope.launch { prefs.updateLayout(transform) }
    }

    private fun newId(prefix: String): String =
        "$prefix-${System.nanoTime().toString(36)}-${(0..9999).random()}"

    companion object {
        private val DOCK_GROUPS = listOf(
            listOf(
                "com.android.dialer", "com.google.android.dialer",
                "com.samsung.android.dialer", "com.android.contacts"
            ),
            listOf(
                "com.google.android.apps.messaging", "com.android.mms",
                "com.samsung.android.messaging", "com.whatsapp"
            ),
            listOf(
                "com.android.chrome", "org.mozilla.firefox",
                "com.microsoft.emmx", "com.brave.browser", "com.opera.browser"
            ),
            listOf(
                "com.android.camera", "com.google.android.GoogleCamera",
                "com.sec.android.app.camera", "com.motorola.camera"
            ),
            listOf("com.android.settings"),
        )

        private val HOME_CANDIDATES = listOf(
            "com.whatsapp", "com.instagram.android", "com.google.android.youtube",
            "com.google.android.gm", "com.android.vending", "com.google.android.apps.maps",
            "com.google.android.apps.photos", "com.spotify.music", "com.netflix.mediaclient",
            "com.google.android.calendar", "com.google.android.apps.docs", "com.twitter.android",
            "com.facebook.katana", "com.google.android.gm", "com.android.chrome",
            "com.google.android.apps.messaging",
        )
        private val HOME_CANDIDATES_SET = HOME_CANDIDATES.toSet()
    }
}
