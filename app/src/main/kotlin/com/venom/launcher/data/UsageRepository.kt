package com.venom.launcher.data

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * Powers Venom's "brains": most-used sorting, the predictive row, and screen-time
 * stats. Everything here degrades gracefully to an empty map when the user has
 * not granted Usage Access.
 */
class UsageRepository(private val context: Context) {

    fun hasPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        if (mode == AppOpsManager.MODE_ALLOWED) return true

        // A handful of OEMs report through the package manager instead.
        return runCatching {
            context.packageManager.checkPermission(
                android.Manifest.permission.PACKAGE_USAGE_STATS,
                context.packageName
            ) == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
    }

    fun openUsageAccessSettings() {
        context.startActivity(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    data class UsageRow(
        val packageName: String,
        val totalMillis: Long,
        val lastUsed: Long,
    )

    /**
     * package -> 24 hourly buckets of "moved to foreground" counts.
     *
     * This is what makes Venom's prediction row feel psychic: an app you only
     * ever open at 8am shouldn't be offered at 8pm.
     */
    suspend fun loadHourlyAffinity(days: Int = 14): Map<String, IntArray> =
        withContext(Dispatchers.Default) {
            if (!hasPermission()) return@withContext emptyMap()
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return@withContext emptyMap()

            val now = System.currentTimeMillis()
            val from = now - days * 24L * 60L * 60L * 1000L
            val events = runCatching { usm.queryEvents(from, now) }.getOrNull()
                ?: return@withContext emptyMap()

            val buckets = HashMap<String, IntArray>()
            val event = android.app.usage.UsageEvents.Event()
            val calendar = java.util.Calendar.getInstance()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType != android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND) continue
                val arr = buckets.getOrPut(event.packageName) { IntArray(24) }
                calendar.timeInMillis = event.timeStamp
                arr[calendar.get(java.util.Calendar.HOUR_OF_DAY)]++
            }
            buckets
        }

    suspend fun loadUsage(days: Int = 7): Map<String, UsageRow> = withContext(Dispatchers.Default) {
        if (!hasPermission()) return@withContext emptyMap()
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return@withContext emptyMap()

        val now = System.currentTimeMillis()
        val from = now - days * 24L * 60L * 60L * 1000L
        val stats: List<UsageStats> = runCatching {
            usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, from, now)
        }.getOrNull() ?: return@withContext emptyMap()

        val merged = LinkedHashMap<String, UsageRow>()
        for (s in stats) {
            val pkg = s.packageName ?: continue
            val prev = merged[pkg]
            merged[pkg] = UsageRow(
                packageName = pkg,
                totalMillis = (prev?.totalMillis ?: 0L) + s.totalTimeInForeground,
                lastUsed = max(prev?.lastUsed ?: 0L, s.lastTimeUsed),
            )
        }
        merged
    }
}
