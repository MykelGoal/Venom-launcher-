package com.venom.launcher.data

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The engine behind **Venom Charge Lab**.
 *
 * Reads the sticky ACTION_BATTERY_CHANGED intent plus the hidden-ish
 * BatteryManager properties (charge counter, instantaneous current) to produce
 * everything the UI shows: level, volts, amps, watts, charge-speed class,
 * estimated time to full, temperature and health.
 *
 * Everything is optional-by-design: if an OEM blocks a property we simply show
 * "—" instead of crashing.
 */
class ChargeRepository(private val app: Application) {

    private val batteryManager: BatteryManager? =
        app.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager

    private val scope = CoroutineScope(Dispatchers.Default + Job())

    private val _state = MutableStateFlow(readState(app))
    val state: StateFlow<BatteryState> = _state.asStateFlow()

    private val _sessions = MutableStateFlow<List<ChargeSession>>(emptyList())
    val sessions: StateFlow<List<ChargeSession>> = _sessions.asStateFlow()

    private val _samples = MutableStateFlow<List<ChargeSample>>(emptyList())
    val samples: StateFlow<List<ChargeSample>> = _samples.asStateFlow()

    /** Fires once per session when the user's charge limit is crossed. */
    private val _limitHit = MutableStateFlow(false)
    val limitHit: StateFlow<Boolean> = _limitHit.asStateFlow()

    private var activeSession: ChargeSession? = null
    private var limitNotified = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED -> refresh(intent)
                Intent.ACTION_POWER_CONNECTED -> refresh(context.registerReceiver(null, filter))
                Intent.ACTION_POWER_DISCONNECTED -> {
                    closeSession()
                    refresh(context.registerReceiver(null, filter))
                }
            }
        }
    }

    private val filter: IntentFilter by lazy {
        IntentFilter(Intent.ACTION_BATTERY_CHANGED).apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
    }

    fun start() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                app.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                app.registerReceiver(receiver, filter)
            }
        }
        refresh(app.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)))

        // Sample every 20s so the charging curve has shape even when the
        // broadcast is quiet (many devices only emit it on whole percent steps).
        scope.launch {
            while (isActive) {
                delay(20_000)
                if (_state.value.isCharging) {
                    refresh(app.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)))
                }
            }
        }
    }

    fun stop() {
        runCatching { app.unregisterReceiver(receiver) }
    }

    fun acknowledgeLimit() {
        _limitHit.value = false
    }

    private fun refresh(intent: Intent?) {
        if (intent == null) return
        val next = readState(app, intent)
        _state.value = next

        if (next.isCharging) {
            if (activeSession == null) {
                activeSession = ChargeSession(
                    startedAt = System.currentTimeMillis(),
                    startPercent = next.percent,
                    peakWatts = next.watts,
                    peakTempC = next.temperatureC,
                    plugType = next.plugType.name,
                )
                limitNotified = false
            }
            activeSession = activeSession?.copy(
                endPercent = next.percent,
                endedAt = System.currentTimeMillis(),
                peakWatts = maxOf(activeSession?.peakWatts ?: 0f, next.watts),
                peakTempC = maxOf(activeSession?.peakTempC ?: 0f, next.temperatureC),
            )
            _samples.update { list ->
                val last = list.lastOrNull()
                val sample = ChargeSample(
                    t = System.currentTimeMillis(),
                    percent = next.percent,
                    watts = next.watts,
                    tempC = next.temperatureC,
                )
                if (last != null && last.percent == sample.percent &&
                    (sample.t - last.t) < 60_000
                ) {
                    list.dropLast(1) + sample
                } else {
                    (list + sample).takeLast(600)
                }
            }
            // charge-limit "caretaker"
            if (!limitNotified) {
                val limit = ChargePrefs.limitPercent
                if (ChargePrefs.limitEnabled && next.percent >= limit) {
                    limitNotified = true
                    _limitHit.value = true
                    ChargeNotifier.notifyLimitReached(app, limit)
                }
            }
        } else {
            closeSession()
        }
    }

    private fun closeSession() {
        val s = activeSession ?: return
        activeSession = null
        if (s.endedAt - s.startedAt > 60_000 && s.endPercent > s.startPercent) {
            _sessions.update { (listOf(s) + it).take(50) }
        }
    }

    // ------------------------------------------------------------ reading ----

    fun readState(context: Context): BatteryState =
        readState(context, context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)))

    private fun readState(context: Context, intent: Intent?): BatteryState {
        if (intent == null) return BatteryState()

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val percent = if (level >= 0 && scale > 0) (level * 100f / scale).roundToInt() else 0

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val plugType = when (intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)) {
            BatteryManager.BATTERY_PLUGGED_AC -> PlugType.AC
            BatteryManager.BATTERY_PLUGGED_USB -> PlugType.USB
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> PlugType.WIRELESS
            BatteryManager.BATTERY_PLUGGED_DOCK -> PlugType.DOCK
            0 -> PlugType.NONE
            else -> PlugType.UNKNOWN
        }

        val tempC = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1).let {
            if (it < 0) Float.NaN else it / 10f
        }
        val voltageV = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1).let {
            if (it < 0) Float.NaN else it / 1000f
        }
        val technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY).orEmpty()
        val health = when (intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheating"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Unknown"
        }

        // ---- properties that need BatteryManager (API 21+) ----
        val currentUa = batteryManager?.let { bm ->
            runCatching { bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) }.getOrNull()
                ?.takeIf { it != Int.MIN_VALUE }
        }
        val chargeCounterUah = batteryManager?.let { bm ->
            runCatching { bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) }.getOrNull()
                ?.takeIf { it != Int.MIN_VALUE }
        }

        val currentMa = currentUa?.let { (it / 1000f).roundToInt() } ?: 0
        val chargedMah = chargeCounterUah?.let { it / 1000 } ?: 0
        val estimatedCapacityMah = if (chargeCounterUah != null && percent > 3) {
            (chargeCounterUah / 1000f / (percent / 100f)).roundToInt()
        } else 0

        val watts = if (voltageV.isNaN() || currentUa == null) 0f
        else abs(voltageV * currentUa / 1_000_000f)

        // time to full: remaining micro-amp-hours / charging micro-amps
        val minutesToFull = if (isCharging && currentUa != null &&
            chargeCounterUah != null && percent > 0 && percent < 100 && currentUa > 0
        ) {
            val fullUah = chargeCounterUah / (percent / 100f)
            val remainingUah = fullUah - chargeCounterUah
            ((remainingUah / currentUa.toFloat()) * 60f).roundToInt().takeIf { it in 1..600 }
        } else null

        val minutesToEmpty = if (!isCharging && currentUa != null &&
            chargeCounterUah != null && currentUa < 0
        ) {
            ((chargeCounterUah / abs(currentUa).toFloat()) * 60f).roundToInt()
                .takeIf { it in 1..3000 }
        } else null

        return BatteryState(
            percent = percent,
            isCharging = isCharging,
            plugType = plugType,
            temperatureC = tempC,
            voltageV = voltageV,
            currentMa = currentMa,
            watts = watts,
            speed = ChargeSpeed.fromWatts(watts),
            technology = technology,
            health = health,
            chargedMah = chargedMah,
            estimatedCapacityMah = estimatedCapacityMah,
            minutesToFull = minutesToFull,
            minutesToEmpty = minutesToEmpty,
            isOverheating = !tempC.isNaN() && tempC >= HEAT_LIMIT_C,
            updatedAt = System.currentTimeMillis(),
        )
    }

    companion object {
        const val HEAT_LIMIT_C = 42f
    }
}
