package com.venom.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

private val Context.settingsStore by preferencesDataStore(name = "venom_settings")
private val Context.layoutStore by preferencesDataStore(name = "venom_layout")

object VenomJson {
    val Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }
}

private object Keys {
    val GRID_COLUMNS = intPreferencesKey("grid_columns")
    val GRID_ROWS = intPreferencesKey("grid_rows")
    val DOCK_COUNT = intPreferencesKey("dock_count")
    val ACCENT = stringPreferencesKey("accent")
    val ICON_SHAPE = stringPreferencesKey("icon_shape")
    val ICON_SCALE = floatPreferencesKey("icon_scale")
    val SHOW_LABELS = booleanPreferencesKey("show_labels")
    val LABEL_LINES = intPreferencesKey("label_lines")
    val SHOW_CLOCK = booleanPreferencesKey("show_clock")
    val SHOW_DOCK = booleanPreferencesKey("show_dock")
    val WALLPAPER_DIM = floatPreferencesKey("wallpaper_dim")
    val WALLPAPER_BLUR = booleanPreferencesKey("wallpaper_blur")
    val ICON_PACK = stringPreferencesKey("icon_pack")
    val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    val SWIPE_UP = stringPreferencesKey("swipe_up")
    val SWIPE_DOWN = stringPreferencesKey("swipe_down")
    val SWIPE_LEFT = stringPreferencesKey("swipe_left")
    val SWIPE_RIGHT = stringPreferencesKey("swipe_right")
    val DOUBLE_TAP = stringPreferencesKey("double_tap")
    val HOME_PRESS = stringPreferencesKey("home_press")
    val DRAWER_SORT = stringPreferencesKey("drawer_sort")
    val DRAWER_COLUMNS = intPreferencesKey("drawer_columns")
    val HIDDEN_APPS = stringSetPreferencesKey("hidden_apps")
    val CHARGE_ANIM = booleanPreferencesKey("charge_anim")
    val CHARGE_OVERLAY = booleanPreferencesKey("charge_overlay")
    val CHARGE_LIMIT_ENABLED = booleanPreferencesKey("charge_limit_enabled")
    val CHARGE_LIMIT_PERCENT = intPreferencesKey("charge_limit_percent")
    val HEAT_GUARD = booleanPreferencesKey("heat_guard")
    val CHARGE_SOUND = booleanPreferencesKey("charge_sound")
    val APP_SHORTCUTS = booleanPreferencesKey("app_shortcuts")
    val NOTIF_BADGES = booleanPreferencesKey("notif_badges")
    val PREDICTIVE_ROW = booleanPreferencesKey("predictive_row")
    val HAPTICS = booleanPreferencesKey("haptics")
    val DOUBLE_TAP_LOCK = booleanPreferencesKey("double_tap_lock")
    val GAME_MODE = booleanPreferencesKey("game_mode")
    val AUTO_GAME = booleanPreferencesKey("auto_game")
    val OVERLAY_FPS = booleanPreferencesKey("overlay_fps")
    val OVERLAY_TEMP = booleanPreferencesKey("overlay_temp")
    val OVERLAY_BATTERY = booleanPreferencesKey("overlay_battery")
    val BLOCK_NOTIFS = booleanPreferencesKey("block_notifs_gaming")
    val GAME_BUBBLE = booleanPreferencesKey("game_bubble")
    val GAME_BOOST = booleanPreferencesKey("game_boost")
    val GAME_DND = booleanPreferencesKey("game_dnd")
    val THERMAL_GUARD = booleanPreferencesKey("thermal_guard")
    val FIRST_RUN = booleanPreferencesKey("first_run_done")
}

class LauncherPrefs(private val context: Context) {

    val settings: Flow<LauncherSettings> = context.settingsStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it.toSettings() }

    val layout: Flow<HomeLayout> = context.layoutStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            prefs[LAYOUT_JSON]?.let { raw ->
                runCatching { VenomJson.Json.decodeFromString<HomeLayout>(raw) }
                    .getOrNull()
            } ?: HomeLayout()
        }

    suspend fun currentSettings(): LauncherSettings = settings.first()

    suspend fun update(transform: (LauncherSettings) -> LauncherSettings) {
        context.settingsStore.edit { prefs ->
            prefs.writeFrom(transform(prefs.toSettings()))
        }
    }

    suspend fun updateLayout(transform: (HomeLayout) -> HomeLayout) {
        context.layoutStore.edit { prefs ->
            val current = prefs[LAYOUT_JSON]?.let { raw ->
                runCatching { VenomJson.Json.decodeFromString<HomeLayout>(raw) }.getOrNull()
            } ?: HomeLayout()
            val next = transform(current)
            prefs[LAYOUT_JSON] = VenomJson.Json.encodeToString(next)
        }
    }

    suspend fun resetLayout() {
        context.layoutStore.edit { it.clear() }
    }

    private companion object {
        val LAYOUT_JSON = stringPreferencesKey("layout_json")
    }
}

private fun Preferences.toSettings() = LauncherSettings(
    gridColumns = this[Keys.GRID_COLUMNS] ?: 4,
    gridRows = this[Keys.GRID_ROWS] ?: 5,
    dockCount = this[Keys.DOCK_COUNT] ?: 5,
    accent = Accent.of(this[Keys.ACCENT]),
    iconShape = IconShape.of(this[Keys.ICON_SHAPE]),
    iconScale = this[Keys.ICON_SCALE] ?: 1.0f,
    showLabels = this[Keys.SHOW_LABELS] ?: true,
    labelLines = this[Keys.LABEL_LINES] ?: 1,
    showClock = this[Keys.SHOW_CLOCK] ?: true,
    showDock = this[Keys.SHOW_DOCK] ?: true,
    wallpaperDim = this[Keys.WALLPAPER_DIM] ?: 0.28f,
    wallpaperBlur = this[Keys.WALLPAPER_BLUR] ?: false,
    iconPack = this[Keys.ICON_PACK],
    dynamicColor = this[Keys.DYNAMIC_COLOR] ?: false,
    swipeUp = GestureAction.of(this[Keys.SWIPE_UP]),
    swipeDown = GestureAction.of(this[Keys.SWIPE_DOWN]),
    swipeLeft = GestureAction.of(this[Keys.SWIPE_LEFT]),
    swipeRight = GestureAction.of(this[Keys.SWIPE_RIGHT]),
    doubleTap = GestureAction.of(this[Keys.DOUBLE_TAP]),
    homePressAction = GestureAction.of(this[Keys.HOME_PRESS]),
    drawerSort = DrawerSort.of(this[Keys.DRAWER_SORT]),
    drawerColumns = this[Keys.DRAWER_COLUMNS] ?: 4,
    hiddenApps = this[Keys.HIDDEN_APPS] ?: emptySet(),
    chargeAnimationEnabled = this[Keys.CHARGE_ANIM] ?: true,
    chargeOverlayEnabled = this[Keys.CHARGE_OVERLAY] ?: true,
    chargeLimitEnabled = this[Keys.CHARGE_LIMIT_ENABLED] ?: false,
    chargeLimitPercent = this[Keys.CHARGE_LIMIT_PERCENT] ?: 85,
    heatGuardEnabled = this[Keys.HEAT_GUARD] ?: true,
    chargeSoundEnabled = this[Keys.CHARGE_SOUND] ?: false,
    appShortcutsEnabled = this[Keys.APP_SHORTCUTS] ?: true,
    notificationBadges = this[Keys.NOTIF_BADGES] ?: true,
    predictiveRow = this[Keys.PREDICTIVE_ROW] ?: true,
    haptics = this[Keys.HAPTICS] ?: true,
    doubleTapLockEnabled = this[Keys.DOUBLE_TAP_LOCK] ?: false,
    gameModeEnabled = this[Keys.GAME_MODE] ?: false,
    autoGameMode = this[Keys.AUTO_GAME] ?: true,
    overlayShowFps = this[Keys.OVERLAY_FPS] ?: true,
    overlayShowTemp = this[Keys.OVERLAY_TEMP] ?: true,
    overlayShowBattery = this[Keys.OVERLAY_BATTERY] ?: true,
    blockNotificationsWhileGaming = this[Keys.BLOCK_NOTIFS] ?: true,
    gameBubbleEnabled = this[Keys.GAME_BUBBLE] ?: true,
    boostOnLaunch = this[Keys.GAME_BOOST] ?: true,
    dndWhileGaming = this[Keys.GAME_DND] ?: false,
    thermalGuardEnabled = this[Keys.THERMAL_GUARD] ?: true,
    firstRunDone = this[Keys.FIRST_RUN] ?: false,
)

private fun MutablePreferences.writeFrom(s: LauncherSettings) {
    this[Keys.GRID_COLUMNS] = s.gridColumns
    this[Keys.GRID_ROWS] = s.gridRows
    this[Keys.DOCK_COUNT] = s.dockCount
    this[Keys.ACCENT] = s.accent.name
    this[Keys.ICON_SHAPE] = s.iconShape.name
    this[Keys.ICON_SCALE] = s.iconScale
    this[Keys.SHOW_LABELS] = s.showLabels
    this[Keys.LABEL_LINES] = s.labelLines
    this[Keys.SHOW_CLOCK] = s.showClock
    this[Keys.SHOW_DOCK] = s.showDock
    this[Keys.WALLPAPER_DIM] = s.wallpaperDim
    this[Keys.WALLPAPER_BLUR] = s.wallpaperBlur
    this[Keys.DYNAMIC_COLOR] = s.dynamicColor
    this[Keys.SWIPE_UP] = s.swipeUp.name
    this[Keys.SWIPE_DOWN] = s.swipeDown.name
    this[Keys.SWIPE_LEFT] = s.swipeLeft.name
    this[Keys.SWIPE_RIGHT] = s.swipeRight.name
    this[Keys.DOUBLE_TAP] = s.doubleTap.name
    this[Keys.HOME_PRESS] = s.homePressAction.name
    this[Keys.DRAWER_SORT] = s.drawerSort.name
    this[Keys.DRAWER_COLUMNS] = s.drawerColumns
    this[Keys.HIDDEN_APPS] = s.hiddenApps
    this[Keys.CHARGE_ANIM] = s.chargeAnimationEnabled
    this[Keys.CHARGE_OVERLAY] = s.chargeOverlayEnabled
    this[Keys.CHARGE_LIMIT_ENABLED] = s.chargeLimitEnabled
    this[Keys.CHARGE_LIMIT_PERCENT] = s.chargeLimitPercent
    this[Keys.HEAT_GUARD] = s.heatGuardEnabled
    this[Keys.CHARGE_SOUND] = s.chargeSoundEnabled
    this[Keys.APP_SHORTCUTS] = s.appShortcutsEnabled
    this[Keys.NOTIF_BADGES] = s.notificationBadges
    this[Keys.PREDICTIVE_ROW] = s.predictiveRow
    this[Keys.HAPTICS] = s.haptics
    this[Keys.DOUBLE_TAP_LOCK] = s.doubleTapLockEnabled
    this[Keys.GAME_MODE] = s.gameModeEnabled
    this[Keys.AUTO_GAME] = s.autoGameMode
    this[Keys.OVERLAY_FPS] = s.overlayShowFps
    this[Keys.OVERLAY_TEMP] = s.overlayShowTemp
    this[Keys.OVERLAY_BATTERY] = s.overlayShowBattery
    this[Keys.BLOCK_NOTIFS] = s.blockNotificationsWhileGaming
    this[Keys.GAME_BUBBLE] = s.gameBubbleEnabled
    this[Keys.GAME_BOOST] = s.boostOnLaunch
    this[Keys.GAME_DND] = s.dndWhileGaming
    this[Keys.THERMAL_GUARD] = s.thermalGuardEnabled
    this[Keys.FIRST_RUN] = s.firstRunDone
    if (s.iconPack != null) this[Keys.ICON_PACK] = s.iconPack else this.remove(Keys.ICON_PACK)
}
