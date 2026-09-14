package com.venom.launcher.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Finds installed games and builds the lifetime stats shown in Game Library.
 *
 * Games are detected from `ApplicationInfo.category == CATEGORY_GAME` (API 26+)
 * with the legacy `FLAG_IS_GAME` bit as a fallback for older/odd packages.
 */
class GameRepository(private val context: Context) {

    suspend fun loadGames(): List<GameInfo> = withContext(Dispatchers.Default) {
        val pm = context.packageManager
        val installed = runCatching { pm.getInstalledApplications(0) }.getOrDefault(emptyList())

        val launchIntents = runCatching {
            pm.queryIntentActivities(
                android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(
                    android.content.Intent.CATEGORY_LAUNCHER
                ), 0
            )
        }.getOrDefault(emptyList())

        val launcherByPackage = HashMap<String, String>()
        val labelByPackage = HashMap<String, String>()
        for (ri in launchIntents) {
            val ai = ri.activityInfo ?: continue
            launcherByPackage.putIfAbsent(ai.packageName, ai.name)
            labelByPackage.putIfAbsent(
                ai.packageName,
                runCatching { ri.loadLabel(pm).toString() }.getOrNull() ?: ai.packageName
            )
        }

        installed.mapNotNull { ai ->
            val isGame = isGameApp(ai)
            if (!isGame) return@mapNotNull null
            val activity = launcherByPackage[ai.packageName] ?: return@mapNotNull null
            GameInfo(
                label = labelByPackage[ai.packageName] ?: ai.packageName,
                packageName = ai.packageName,
                activityName = activity,
            )
        }.sortedBy { it.label.lowercase() }
    }

    /** Banner art if the game ships any, otherwise the launcher icon. */
    fun banner(packageName: String): Drawable? {
        val pm = context.packageManager
        return runCatching { pm.getApplicationBanner(packageName) }.getOrNull()
            ?: runCatching { pm.getApplicationIcon(packageName) }.getOrNull()
    }

    fun isGamePackage(packageName: String): Boolean = runCatching {
        val ai = context.packageManager.getApplicationInfo(packageName, 0)
        isGameApp(ai)
    }.getOrDefault(false)

    private fun isGameApp(ai: ApplicationInfo): Boolean {
        val categoryGame = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
            ai.category == ApplicationInfo.CATEGORY_GAME
        @Suppress("DEPRECATION")
        val legacyGame = (ai.flags and ApplicationInfo.FLAG_IS_GAME) != 0
        return categoryGame || legacyGame
    }

    fun statsFor(sessions: List<GameSession>, packageName: String): GameStats {
        val mine = sessions.filter { it.packageName == packageName }
        if (mine.isEmpty()) {
            return GameStats(packageName, 0, 0, 0, 0f, 0f, 0L)
        }
        return GameStats(
            packageName = packageName,
            sessions = mine.size,
            totalMinutes = mine.sumOf { it.durationMinutes },
            batteryDrained = mine.sumOf { it.batteryDrained },
            peakTempC = mine.maxOf { it.peakTempC },
            avgFps = mine.map { it.avgFps }.average().toFloat(),
            lastPlayed = mine.maxOf { it.endedAt },
        )
    }
}
