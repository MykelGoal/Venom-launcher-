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

    override fun onListenerConnected() = publish()
    override fun onNotificationPosted(sbn: StatusBarNotification?) = publish()
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = publish()
    override fun onListenerDisconnected() {
        VenomBus.updateBadges(emptyMap())
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
        fun isSupported() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
    }
}
