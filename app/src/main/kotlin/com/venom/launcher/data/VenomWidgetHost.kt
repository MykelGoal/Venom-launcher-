package com.venom.launcher.data

import android.appwidget.AppWidgetHost
import android.content.Context

/**
 * One [AppWidgetHost] for the whole process.
 *
 * The activity calls [startListening] / [stopListening] around its visible
 * lifecycle; the host itself must outlive configuration changes so widget ids
 * keep working.
 */
object VenomWidgetHost {

    private const val HOST_ID = 0x56454E // "VEN"
    private const val PREF_ALLOC = 9000

    @Volatile
    private var host: AppWidgetHost? = null

    fun get(context: Context): AppWidgetHost = host ?: synchronized(this) {
        host ?: AppWidgetHost(context.applicationContext, HOST_ID).also { host = it }
    }

    /** Ids below this are system-reserved for hosts; never hand one to a widget. */
    fun nextAppWidgetId(): Int = PREF_ALLOC + (host?.appWidgetIds?.size ?: 0) + 1
}
