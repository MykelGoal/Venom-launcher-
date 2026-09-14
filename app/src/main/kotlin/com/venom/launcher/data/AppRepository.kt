package com.venom.launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads the system's list of launchable activities. */
class AppRepository(private val context: Context) {

    suspend fun loadApps(): List<AppInfo> = withContext(Dispatchers.Default) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved: List<ResolveInfo> = pm.queryIntentActivities(intent, 0)
        val self = context.packageName

        resolved.mapNotNull { ri ->
            val ai = ri.activityInfo ?: return@mapNotNull null
            val pkg = ai.packageName ?: return@mapNotNull null
            if (pkg == self) return@mapNotNull null // don't launch Venom from Venom

            val label = ri.loadLabel(pm)?.toString()
                .takeUnless { it.isNullOrBlank() }
                ?: ai.name.substringAfterLast('.')

            AppInfo(
                label = label,
                packageName = pkg,
                activityName = ai.name,
                isSystemApp = runCatching {
                    (pm.getApplicationInfo(pkg, 0).flags and ApplicationInfo.FLAG_SYSTEM) != 0
                }.getOrDefault(false),
            )
        }
            .distinctBy { it.componentKey }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }

    /** Apps whose package matches one of the usual suspects, in the order given. */
    suspend fun pickSeedApps(candidates: List<String>): List<AppInfo> {
        val all = loadApps()
        val byPackage = all.associateBy { it.packageName }
        return candidates.mapNotNull { byPackage[it] }
    }
}
