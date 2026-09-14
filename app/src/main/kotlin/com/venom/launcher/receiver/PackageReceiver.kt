package com.venom.launcher.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.venom.launcher.data.VenomBus

/**
 * Keeps the in-memory app list fresh. Venom also re-queries on every resume, so
 * this is belt-and-braces for OEMs that still deliver these broadcasts to
 * manifest receivers.
 */
class PackageReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_PACKAGE_REMOVED && intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
            return // handled by ACTION_PACKAGE_REPLACED
        }
        VenomBus.requestAppReload()
    }
}
