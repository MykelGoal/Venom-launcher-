package com.venom.launcher.util

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.venom.launcher.data.AppInfo
import com.venom.launcher.receiver.VenomDeviceAdmin

// -------------------------------------------------------------- app launch ----

fun Context.launchApp(app: AppInfo) {
    val intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
        component = app.componentName
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
    }
    runCatching { startActivity(intent) }
        .onFailure {
            // Fall back to whatever the package's default launch intent is
            val fallback = packageManager.getLaunchIntentForPackage(app.packageName)
            if (fallback != null) runCatching {
                startActivity(fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
}

fun Context.openAppInfo(packageName: String) {
    runCatching {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.parse("package:$packageName"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

fun Context.requestUninstall(packageName: String) {
    runCatching {
        startActivity(
            Intent(Intent.ACTION_DELETE)
                .setData(Uri.parse("package:$packageName"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

fun Context.openWallpaperPicker() {
    runCatching {
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Set wallpaper"))
    }
}

// ---------------------------------------------------------------- gestures ----

/**
 * StatusBarManager is a hidden API, so this is best-effort. On Android 12+ some
 * OEMs block it; the caller should fall back to something else if it returns
 * false.
 */
fun Context.expandNotifications(): Boolean = runCatching {
    val service = getSystemService("statusbar")
    val clazz = Class.forName("android.app.StatusBarManager")
    val method = clazz.getMethod("expandNotificationsPanel")
    method.invoke(service)
    true
}.getOrDefault(false)

fun Context.expandQuickSettings(): Boolean = runCatching {
    val service = getSystemService("statusbar")
    val clazz = Class.forName("android.app.StatusBarManager")
    val method = clazz.getMethod("expandSettingsPanel")
    method.invoke(service)
    true
}.getOrDefault(false)

fun deviceAdminComponent(context: Context) =
    ComponentName(context, VenomDeviceAdmin::class.java)

fun Context.isDeviceAdminActive(): Boolean = runCatching {
    (getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager)
        .isAdminActive(deviceAdminComponent(this))
}.getOrDefault(false)

fun Context.lockScreen(): Boolean {
    if (!isDeviceAdminActive()) return false
    return runCatching {
        (getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager).lockNow()
        true
    }.getOrDefault(false)
}

fun Context.requestDeviceAdmin() {
    runCatching {
        startActivity(
            Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdminComponent(this))
                .putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Venom needs device admin only to lock your screen on double tap."
                )
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

// ----------------------------------------------------------------- system ----

/** Android 10+ uses the Home role; older versions just open the chooser screen. */
fun Context.requestDefaultLauncher() {
    runCatching {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val rm = getSystemService(android.app.role.RoleManager::class.java)
            if (rm != null && !rm.isRoleHeld(android.app.role.RoleManager.ROLE_HOME)) {
                startActivity(
                    rm.createRequestRoleIntent(android.app.role.RoleManager.ROLE_HOME)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                return
            }
        }
        startActivity(
            Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

fun Context.openNotificationListenerSettings() {
    runCatching {
        startActivity(
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

fun Context.canDrawOverlays(): Boolean =
    android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M ||
        android.provider.Settings.canDrawOverlays(this)

/** Game Mode's HUD needs "draw over other apps". */
fun Context.requestOverlayPermission() {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) return
    runCatching {
        startActivity(
            Intent(
                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

fun Context.openUsageAccessSettings() {
    runCatching {
        startActivity(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
