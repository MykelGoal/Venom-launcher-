package com.venom.launcher.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.venom.launcher.MainActivity
import com.venom.launcher.R

/**
 * The only notifications Venom ever posts: "you asked to be told at 85%".
 * No ads, no nagging, no analytics.
 */
object ChargeNotifier {

    const val CHANNEL_CHARGE = "venom_charge"
    const val ID_LIMIT = 4201
    const val ID_HEAT = 4202

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_CHARGE) != null) return
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_CHARGE,
                "Charge Lab",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Charge limit and temperature alerts"
                enableVibration(true)
            }
        )
    }

    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun notifyLimitReached(context: Context, percent: Int) {
        if (!canPost(context)) return
        ensureChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(MainActivity.EXTRA_OPEN_CHARGE_LAB, true)
        }
        val pi = PendingIntent.getActivity(
            context, 7, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentFlags()
        )
        val n = NotificationCompat.Builder(context, CHANNEL_CHARGE)
            .setSmallIcon(R.drawable.ic_bolt)
            .setContentTitle("Charge limit reached — $percent%")
            .setContentText("Unplug now to keep the battery healthy. Tap to open Charge Lab.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(ID_LIMIT, n) }
    }

    fun notifyOverheat(context: Context, tempC: Float) {
        if (!canPost(context)) return
        ensureChannel(context)
        val n = NotificationCompat.Builder(context, CHANNEL_CHARGE)
            .setSmallIcon(R.drawable.ic_bolt)
            .setContentTitle("Battery is hot — ${"%.0f".format(tempC)}°C")
            .setContentText("Charging this hot wears the cell faster. Consider unplugging for a bit.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(ID_HEAT, n) }
    }

    private fun pendingIntentFlags(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
}
