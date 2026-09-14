package com.venom.launcher.data

import android.content.ComponentName

/**
 * A launchable activity, resolved once from the system and cached in memory.
 *
 * [componentKey] ("com.pkg/com.pkg.MainActivity") is the stable identity we
 * persist everywhere: home layout, dock, folders, hidden list.
 */
data class AppInfo(
    val label: String,
    val packageName: String,
    val activityName: String,
    val isSystemApp: Boolean,
) {
    val componentKey: String get() = "$packageName/$activityName"

    val componentName: ComponentName get() = ComponentName(packageName, activityName)

    /** Cheap, pre-computed lowercase label for search ranking. */
    val searchKey: String get() = label.lowercase()

    val firstLetter: Char get() = label.firstOrNull()?.uppercaseChar() ?: '#'

    companion object {
        fun key(packageName: String, activityName: String) = "$packageName/$activityName"
    }
}
