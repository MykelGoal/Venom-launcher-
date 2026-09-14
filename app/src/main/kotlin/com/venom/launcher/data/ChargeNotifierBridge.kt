package com.venom.launcher.data

import android.content.Context

/** Thin indirection so the Application class doesn't hard-depend on notifier internals. */
object ChargeNotifierBridge {
    fun ensureChannel(context: Context) = ChargeNotifier.ensureChannel(context)
}

/**
 * Realtime mirror of the two Charge Lab settings that a broadcast receiver must
 * read off the main thread. VenomApp keeps it in sync.
 */
object ChargePrefsCache {
    @Volatile
    var enabled: Boolean = false

    @Volatile
    var percent: Int = 85
}
