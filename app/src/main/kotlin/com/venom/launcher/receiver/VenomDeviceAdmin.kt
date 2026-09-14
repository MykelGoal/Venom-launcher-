package com.venom.launcher.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

/**
 * Only exists so "double tap to lock" can call DevicePolicyManager.lockNow().
 * Nothing happens until the user activates it from Venom Settings.
 */
class VenomDeviceAdmin : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        com.venom.launcher.data.VenomBus.deviceAdminChanged(true)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        com.venom.launcher.data.VenomBus.deviceAdminChanged(false)
    }
}
