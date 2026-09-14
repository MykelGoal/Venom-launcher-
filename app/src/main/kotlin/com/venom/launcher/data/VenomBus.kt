package com.venom.launcher.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tiny process-wide event bus for the handful of things that need to poke the UI
 * from outside a composable (package installs, device-admin changes, notification
 * counts, home-button presses).
 */
object VenomBus {

    private val _appReloads = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    val appReloads: SharedFlow<Unit> = _appReloads.asSharedFlow()

    private val _adminChanges = MutableSharedFlow<Boolean>(extraBufferCapacity = 2)
    val adminChanges: SharedFlow<Boolean> = _adminChanges.asSharedFlow()

    private val _homePress = MutableSharedFlow<Unit>(extraBufferCapacity = 2)
    val homePress: SharedFlow<Unit> = _homePress.asSharedFlow()

    private val _openGames = MutableSharedFlow<Unit>(extraBufferCapacity = 2)
    val openGames: SharedFlow<Unit> = _openGames.asSharedFlow()

    private val _openChargeLab = MutableSharedFlow<Unit>(extraBufferCapacity = 2)
    val openChargeLab: SharedFlow<Unit> = _openChargeLab.asSharedFlow()

    private val _badgeUpdates = MutableSharedFlow<Map<String, Int>>(extraBufferCapacity = 4)
    val badgeUpdates: SharedFlow<Map<String, Int>> = _badgeUpdates.asSharedFlow()

    private val _gameTelemetry = MutableStateFlow(GameTelemetry())
    val gameTelemetry: StateFlow<GameTelemetry> = _gameTelemetry.asStateFlow()

    private val _gamingPackage = MutableStateFlow<String?>(null)
    val gamingPackage: StateFlow<String?> = _gamingPackage.asStateFlow()

    private val _badges = MutableStateFlow<Map<String, Int>>(emptyMap())
    val badges: StateFlow<Map<String, Int>> = _badges.asStateFlow()

    fun requestAppReload() = _appReloads.tryEmit(Unit)
    fun deviceAdminChanged(active: Boolean) = _adminChanges.tryEmit(active)
    fun homePressed() = _homePress.tryEmit(Unit)
    fun openChargeLab() = _openChargeLab.tryEmit(Unit)
    fun openGames() = _openGames.tryEmit(Unit)
    fun updateBadges(map: Map<String, Int>) = _badgeUpdates.tryEmit(map)
    fun updateGameTelemetry(t: GameTelemetry) {
        _gameTelemetry.value = t
    }

    fun setGamingPackage(pkg: String?) {
        _gamingPackage.value = pkg
    }
    fun setBadges(map: Map<String, Int>) {
        _badges.value = map
    }
}
