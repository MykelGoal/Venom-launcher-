package com.venom.launcher.util

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings

/**
 * The system levers Game Turbo pulls: do-not-disturb, brightness, media volume
 * and "boost" (clearing cached background processes so the game gets the RAM).
 *
 * Every call is defensive: if the user hasn't granted the matching permission
 * the call returns false and the caller just carries on.
 */
object GameControls {

    // --------------------------------------------------------- notification --

    fun hasDndAccess(context: Context): Boolean {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return false
        return runCatching { nm.isNotificationPolicyAccessGranted }.getOrDefault(false)
    }

    fun requestDndAccess(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runCatching { context.startActivity(intent) }.isFailure) {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }

    /** Returns true if the filter actually changed. */
    fun setDnd(context: Context, on: Boolean): Boolean {
        if (!hasDndAccess(context)) return false
        val nm = context.getSystemService(NotificationManager::class.java) ?: return false
        return runCatching {
            nm.setInterruptionFilter(
                if (on) NotificationManager.INTERRUPTION_FILTER_NONE
                else NotificationManager.INTERRUPTION_FILTER_ALL
            )
            true
        }.getOrDefault(false)
    }

    // ------------------------------------------------------------- brightness --

    fun canWriteSettings(context: Context): Boolean =
        runCatching { Settings.System.canWrite(context) }.getOrDefault(false)

    fun requestWriteSettings(context: Context) {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            data = android.net.Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (runCatching { context.startActivity(intent) }.isFailure) {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }

    fun readBrightnessPercent(context: Context): Int {
        val raw = runCatching {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        }.getOrDefault(-1)
        if (raw < 0) return -1
        return (raw * 100 / 255).coerceIn(0, 100)
    }

    /** `percent` is 0..100. Returns true if it was applied. */
    fun setBrightnessPercent(context: Context, percent: Int): Boolean {
        if (!canWriteSettings(context)) return false
        val value = (percent.coerceIn(0, 100) * 255 / 100).coerceIn(0, 255)
        return runCatching {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, value)
            true
        }.getOrDefault(false)
    }

    // ------------------------------------------------------------------ audio --

    fun isMuted(context: Context): Boolean {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        return runCatching { am.getStreamVolume(AudioManager.STREAM_MUSIC) }.getOrDefault(1) == 0
    }

    fun setMuted(context: Context, muted: Boolean): Boolean {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        return runCatching {
            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val target = if (muted) 0 else (max * 2 / 5).coerceAtLeast(1)
            am.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
            true
        }.getOrDefault(false)
    }

    // ------------------------------------------------------------------ boost --

    /**
     * Cancels every cached background process we're allowed to touch, so the
     * game starts with as much free memory as Android will give us.
     *
     * `keep` protects the game itself and anything else that must survive.
     * Returns how many processes we asked the system to kill.
     */
    fun boost(context: Context, keep: Set<String>): Int {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return 0
        val self = context.packageName
        val protected = keep + self
        var killed = 0
        val processes = runCatching { am.runningAppProcesses }.getOrNull().orEmpty()
        for (process in processes) {
            if (process.importance < ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED) {
                continue
            }
            for (pkg in process.pkgList) {
                if (pkg in protected) continue
                if (runCatching { am.killBackgroundProcesses(pkg) }.isSuccess) killed++
            }
        }
        return killed
    }
}
