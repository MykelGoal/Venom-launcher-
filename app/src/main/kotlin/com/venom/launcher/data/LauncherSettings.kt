package com.venom.launcher.data

/** Accent palettes. [color] is the raw ARGB long; Compose converts it. */
enum class Accent(val label: String, val color: Long) {
    VENOM("Venom", 0xFFB4FF39),
    TOXIC("Toxic", 0xFF3DFFD0),
    PLASMA("Plasma", 0xFFB45CFF),
    BLOOD("Blood", 0xFFFF4D5E),
    ICE("Ice", 0xFF8AB4FF),
    EMBER("Ember", 0xFFFFA62B),
    ;

    companion object {
        fun of(name: String?) = entries.firstOrNull { it.name == name } ?: VENOM
    }
}

/** Uniform mask applied to every icon. */
enum class IconShape(val label: String) {
    SYSTEM("System"),
    SQUIRCLE("Squircle"),
    CIRCLE("Circle"),
    ROUNDED("Rounded"),
    HEXAGON("Hexagon"),
    ;

    companion object {
        fun of(name: String?) = entries.firstOrNull { it.name == name } ?: SQUIRCLE
    }
}

/** What a gesture on the home screen does. */
enum class GestureAction(val label: String) {
    NONE("Do nothing"),
    OPEN_DRAWER("Open app drawer"),
    NOTIFICATIONS("Open notifications"),
    QUICK_SETTINGS("Open quick settings"),
    LOCK("Lock screen"),
    SEARCH("Open search"),
    CHARGE_LAB("Open Charge Lab"),
    APP_STATS("Open app stats"),
    GAME_LIBRARY("Open game library"),
    ;

    companion object {
        fun of(name: String?) = entries.firstOrNull { it.name == name } ?: NONE
    }
}

enum class DrawerSort(val label: String) {
    ALPHABETICAL("A-Z"),
    INSTALL_TIME("Recently installed"),
    MOST_USED("Most used"),
    ;

    companion object {
        fun of(name: String?) = entries.firstOrNull { it.name == name } ?: ALPHABETICAL
    }
}

data class LauncherSettings(
    // --- grid ---
    val gridColumns: Int = 4,
    val gridRows: Int = 5,
    val dockCount: Int = 5,
    // --- look ---
    val accent: Accent = Accent.VENOM,
    val iconShape: IconShape = IconShape.SQUIRCLE,
    val iconScale: Float = 1.0f,
    val showLabels: Boolean = true,
    val labelLines: Int = 1,
    val showClock: Boolean = true,
    val showDock: Boolean = true,
    val wallpaperDim: Float = 0.28f,
    val wallpaperBlur: Boolean = false,
    val iconPack: String? = null,
    val dynamicColor: Boolean = false,
    // --- gestures ---
    val swipeUp: GestureAction = GestureAction.OPEN_DRAWER,
    val swipeDown: GestureAction = GestureAction.NOTIFICATIONS,
    val swipeLeft: GestureAction = GestureAction.CHARGE_LAB,
    val swipeRight: GestureAction = GestureAction.APP_STATS,
    val doubleTap: GestureAction = GestureAction.LOCK,
    val homePressAction: GestureAction = GestureAction.NONE,
    // --- drawer ---
    val drawerSort: DrawerSort = DrawerSort.ALPHABETICAL,
    val drawerColumns: Int = 4,
    val hiddenApps: Set<String> = emptySet(),
    // --- venom charge lab ---
    val chargeAnimationEnabled: Boolean = true,
    val chargeOverlayEnabled: Boolean = true,
    val chargeLimitEnabled: Boolean = false,
    val chargeLimitPercent: Int = 85,
    val heatGuardEnabled: Boolean = true,
    val chargeSoundEnabled: Boolean = false,
    // --- pro ---
    val appShortcutsEnabled: Boolean = true,
    val notificationBadges: Boolean = true,
    val predictiveRow: Boolean = true,
    val haptics: Boolean = true,
    // --- gaming ---
    val gameModeEnabled: Boolean = false,
    val autoGameMode: Boolean = true,
    val overlayShowFps: Boolean = true,
    val overlayShowTemp: Boolean = true,
    val overlayShowBattery: Boolean = true,
    val blockNotificationsWhileGaming: Boolean = true,
    val gameBubbleEnabled: Boolean = true,
    val boostOnLaunch: Boolean = true,
    val dndWhileGaming: Boolean = false,
    val thermalGuardEnabled: Boolean = true,
    // --- misc ---
    val doubleTapLockEnabled: Boolean = false,
    val firstRunDone: Boolean = false,
) {
    val cellsPerPage: Int get() = gridColumns * gridRows
}
