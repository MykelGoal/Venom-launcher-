package com.venom.launcher.service

import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.venom.launcher.data.VenomBus

/**
 * Purely counts notifications per package so Venom can draw dots on home-screen
 * icons. Nothing is read, stored, logged or transmitted anywhere.
 */
class VenomNotificationService : NotificationListenerService() {

    override fun onListenerConnected() {
        instance = this
        publish()
    }
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = publish()
    override fun onListenerDisconnected() {
        instance = null
        VenomBus.updateBadges(emptyMap())
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        publish()
        // Game Mode: push incoming notifications out of the way while playing
        val pkg = com.venom.launcher.data.VenomBus.gamingPackage.value
        if (pkg != null && sbn?.isClearable == true) {
            runCatching { snoozeNotification(sbn.key, SNOOZE_WHILE_GAMING_MS) }
        }
    }

    private fun publish() {
        val active = runCatching { activeNotifications }.getOrNull() ?: emptyArray()
        val counts = HashMap<String, Int>()
        for (sbn in active) {
            if (!sbn.isClearable) continue // ongoing/foreground services aren't "unread"
            val pkg = sbn.packageName ?: continue
            counts[pkg] = (counts[pkg] ?: 0) + 1
        }
        VenomBus.updateBadges(counts)
    }

    companion object {
        private const val SNOOZE_WHILE_GAMING_MS = 3 * 60 * 60 * 1000L

        @Volatile
        private var instance: VenomNotificationService? = null

        /** Snoozes everything currently in the shade (used when a game launches). */
        fun snoozeActive(durationMs: Long) {
            val svc = instance ?: return
            val active = runCatching { svc.activeNotifications }.getOrNull() ?: return
            for (sbn in active) {
                if (!sbn.isClearable) continue
                runCatching { svc.snoozeNotification(sbn.key, durationMs) }
            }
        }
    }
}
