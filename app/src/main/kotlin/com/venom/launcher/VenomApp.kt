package com.venom.launcher

import android.app.Application
import android.util.Log
import com.venom.launcher.data.ChargeNotifierBridge
import com.venom.launcher.data.ChargePrefsCache
import com.venom.launcher.data.ChargePrefs
import com.venom.launcher.data.ChargeRepository
import com.venom.launcher.data.VenomBus
import com.venom.launcher.data.VenomWidgetHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class VenomApp : Application() {

    lateinit var chargeRepository: ChargeRepository
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        // Warm the widget host so the first home frame can bind widgets without
        // a stutter. Cheap: AppWidgetHost's constructor does no IPC.
        runCatching { VenomWidgetHost.get(this) }
            .onFailure { Log.w(TAG, "Widget host init failed", it) }

        chargeRepository = ChargeRepository(this).also { it.start() }
        ChargeNotifierBridge.ensureChannel(this)

        // Keep the charge-limit cache warm so a broadcast receiver can read it
        // synchronously without touching disk.
        appScope.launch {
            ChargePrefs.observe(this@VenomApp)
                .distinctUntilChanged()
                .collect { (enabled, percent) ->
                    ChargePrefsCache.enabled = enabled
                    ChargePrefsCache.percent = percent
                }
        }

        appScope.launch {
            VenomBus.badgeUpdates.collect { map -> VenomBus.setBadges(map) }
        }
    }

    override fun onTerminate() {
        runCatching { chargeRepository.stop() }
        super.onTerminate()
    }

    private companion object {
        const val TAG = "VenomApp"
    }
}
