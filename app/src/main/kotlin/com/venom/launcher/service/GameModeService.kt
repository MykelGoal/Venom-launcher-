package com.venom.launcher.service

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Choreographer
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.venom.launcher.MainActivity
import com.venom.launcher.R
import com.venom.launcher.data.ChargeRepository
import com.venom.launcher.data.GameRepository
import com.venom.launcher.data.GameSession
import com.venom.launcher.data.GameStore
import com.venom.launcher.data.GameTelemetry
import com.venom.launcher.data.LauncherPrefs
import com.venom.launcher.data.VenomBus
import com.venom.launcher.overlay.GameOverlayView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Watches which app is in the foreground and, when it's a game:
 *
 *  - drops a lightweight FPS / temperature / battery HUD on top of it
 *  - records a play session (duration, battery drain, peak temp, avg FPS)
 *  - optionally snoozes incoming notifications so they don't cover the screen
 *
 * Runs as a foreground service because that's the only way to keep sampling
 * while you're inside a game. It is off until you switch Game Mode on.
 */
class GameModeService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var prefs: LauncherPrefs
    private lateinit var gameRepo: GameRepository
    private lateinit var gameStore: GameStore
    private lateinit var charge: ChargeRepository
    private lateinit var windowManager: WindowManager

    private var overlay: GameOverlayView? = null
    private var overlayShown = false

    private val frameTimes = ArrayDeque<Float>()
    private var lastFrameNs = 0L
    private var sampling = false

    private var currentPackage: String? = null
    private var session: GameSession? = null
    private var peakTemp = 0f
    private var minFps = Float.MAX_VALUE
    private val fpsSamples = ArrayList<Float>()
    private var monitorJob: Job? = null
    private var lastSettings = com.venom.launcher.data.LauncherSettings()

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (lastFrameNs > 0L) {
                val deltaMs = (frameTimeNanos - lastFrameNs) / 1_000_000f
                if (deltaMs > 0.5f && deltaMs < 200f) {
                    synchronized(frameTimes) {
                        frameTimes.addLast(deltaMs)
                        while (frameTimes.size > 120) frameTimes.removeFirst()
                    }
                }
            }
            lastFrameNs = frameTimeNanos
            if (sampling) Choreographer.getInstance().postFrameCallback(this)
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = LauncherPrefs(this)
        gameRepo = GameRepository(this)
        gameStore = GameStore(this)
        charge = ChargeRepository(application as android.app.Application)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopGameMode()
            stopSelf()
            return START_NOT_STICKY
        }
        startGameMode()
        return START_STICKY
    }

    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        stopGameMode()
        super.onDestroy()
    }

    // ------------------------------------------------------------- lifecycle --

    private fun startGameMode() {
        ensureChannel()
        runCatching { startForeground(NOTIF_ID, buildNotification("Watching for games")) }
            .onFailure {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && it is ForegroundServiceStartNotAllowedException) {
                    Toast.makeText(
                        this,
                        "Android blocked Game Mode from starting in the background. Open Venom first.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                stopSelf()
                return
            }

        monitorJob?.cancel()
        monitorJob = scope.launch {
            while (isActive) {
                val settings = runCatching { prefs.settings.first() }.getOrNull()
                    ?: lastSettings
                lastSettings = settings
                val enabled = settings.gameModeEnabled
                val autoDetect = settings.autoGameMode

                if (!enabled) {
                    finishSession()
                    hideOverlay()
                    delay(10_000)
                    continue
                }

                if (autoDetect) {
                    val pkg = foregroundPackage()
                    val isGame = pkg != null && gameRepo.isGamePackage(pkg)
                    if (isGame && currentPackage == null) {
                        beginSession(pkg!!)
                    } else if (!isGame && currentPackage != null) {
                        finishSession()
                    }
                }
                delay(3000)
            }
        }
    }

    private fun stopGameMode() {
        monitorJob?.cancel()
        monitorJob = null
        finishSession()
        hideOverlay()
        sampling = false
        VenomBus.setGamingPackage(null)
    }

    // -------------------------------------------------------------- sessions --

    private fun beginSession(packageName: String) {
        currentPackage = packageName
        val battery = charge.readState(this)
        peakTemp = battery.temperatureC
        minFps = Float.MAX_VALUE
        fpsSamples.clear()
        synchronized(frameTimes) { frameTimes.clear() }

        session = GameSession(
            packageName = packageName,
            startedAt = System.currentTimeMillis(),
            startBattery = battery.percent,
            startTempC = battery.temperatureC,
            peakTempC = battery.temperatureC,
        )

        VenomBus.setGamingPackage(packageName)
        startSampling()
        showOverlay()

        if (lastSettings.blockNotificationsWhileGaming) {
            VenomNotificationService.snoozeActive(SNOOZE_MS)
        }
        updateNotification(buildNotification("Session running · ${sessionLabel(packageName)}"))
    }

    private fun finishSession() {
        val s = session
        if (s != null && currentPackage != null) {
            val battery = charge.readState(this)
            val avg = if (fpsSamples.isEmpty()) 0f else fpsSamples.average().toFloat()
            val finished = s.copy(
                endedAt = System.currentTimeMillis(),
                endBattery = battery.percent,
                peakTempC = peakTemp,
                avgFps = avg,
                minFps = if (minFps == Float.MAX_VALUE) 0f else minFps,
            )
            scope.launch { runCatching { gameStore.add(finished) } }
        }
        session = null
        currentPackage = null
        stopSampling()
        hideOverlay()
        VenomBus.setGamingPackage(null)
        VenomBus.updateGameTelemetry(GameTelemetry())
        updateNotification(buildNotification("Watching for games"))
    }

    // ------------------------------------------------------------ foreground --

    private fun foregroundPackage(): String? {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return null
        val end = System.currentTimeMillis()
        val begin = end - 90_000L
        val stats = runCatching {
            usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, begin, end)
        }.getOrNull() ?: return null
        return stats.filter { it.lastTimeUsed > 0 }.maxByOrNull { it.lastTimeUsed }?.packageName
    }

    // --------------------------------------------------------------- sampling --

    private fun startSampling() {
        if (sampling) return
        sampling = true
        lastFrameNs = 0L
        mainHandler.post {
            runCatching { Choreographer.getInstance().postFrameCallback(frameCallback) }
        }
        scope.launch {
            while (isActive && sampling) {
                delay(500)
                val history: List<Float>
                val avgMs: Float
                synchronized(frameTimes) {
                    history = frameTimes.toList()
                    avgMs = if (history.isEmpty()) 0f else history.average().toFloat()
                }
                val fps = if (avgMs > 0f) (1000f / avgMs).roundToInt() else 0
                if (fps > 0) {
                    fpsSamples.add(fps.toFloat())
                    if (fps < minFps) minFps = fps.toFloat()
                }

                val battery = charge.readState(this@GameModeService)
                if (!battery.temperatureC.isNaN() && battery.temperatureC > peakTemp) {
                    peakTemp = battery.temperatureC
                }
                val started = session?.startedAt ?: System.currentTimeMillis()
                val telemetry = GameTelemetry(
                    packageName = currentPackage.orEmpty(),
                    fps = fps,
                    minFps = if (minFps == Float.MAX_VALUE) 0 else minFps.roundToInt(),
                    frameMs = avgMs,
                    batteryPercent = battery.percent,
                    batteryTempC = battery.temperatureC,
                    isCharging = battery.isCharging,
                    sessionMinutes = ((System.currentTimeMillis() - started) / 60_000L).toInt(),
                )
                VenomBus.updateGameTelemetry(telemetry)
                val snapshot = history
                mainHandler.post { overlay?.update(telemetry, snapshot) }
            }
        }
    }

    private fun stopSampling() {
        sampling = false
        mainHandler.post {
            runCatching { Choreographer.getInstance().removeFrameCallback(frameCallback) }
        }
    }

    // ---------------------------------------------------------------- overlay --

    private fun showOverlay() {
        if (!canDrawOverlays()) return
        if (overlayShown) return
        val showFps = lastSettings.overlayShowFps
        val showTemp = lastSettings.overlayShowTemp
        val showBattery = lastSettings.overlayShowBattery

        mainHandler.post {
            runCatching {
                val view = GameOverlayView(this@GameModeService).apply {
                    this.showFps = showFps
                    this.showTemp = showTemp
                    this.showBattery = showBattery
                }
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT,
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                    x = (12 * resources.displayMetrics.density).toInt()
                    y = (56 * resources.displayMetrics.density).toInt()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        layoutInDisplayCutoutMode =
                            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
                }
                windowManager.addView(view, params)
                overlay = view
                overlayShown = true
            }
        }
    }

    private fun hideOverlay() {
        mainHandler.post {
            runCatching {
                overlay?.let { windowManager.removeView(it) }
            }
            overlay = null
            overlayShown = false
        }
    }

    private fun sessionLabel(packageName: String): String = runCatching {
        packageManager.getApplicationLabel(
            packageManager.getApplicationInfo(packageName, 0)
        ).toString()
    }.getOrDefault(packageName)

    private fun canDrawOverlays(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)

    // ----------------------------------------------------------- notification --

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL) != null) return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Game Mode", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Runs while Game Mode is on"
            }
        )
    }

    private fun buildNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(MainActivity.EXTRA_OPEN_GAMES, true)
        }
        val pi = PendingIntent.getActivity(
            this, 12, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_bolt)
            .setContentTitle("Venom Game Mode")
            .setContentText(text)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pi)
            .build()
    }

    private fun updateNotification(n: Notification) {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        runCatching { nm.notify(NOTIF_ID, n) }
    }

    companion object {
        const val ACTION_STOP = "com.venom.launcher.action.GAME_MODE_STOP"
        const val CHANNEL = "venom_game_mode"
        const val NOTIF_ID = 9001
        private const val SNOOZE_MS = 3 * 60 * 60 * 1000L

        fun start(context: Context) {
            val i = Intent(context, GameModeService::class.java)
            runCatching {
                androidx.core.content.ContextCompat.startForegroundService(context, i)
            }.onFailure {
                Toast.makeText(
                    context,
                    "Couldn't start Game Mode: ${it.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, GameModeService::class.java)) }
        }
    }
}
