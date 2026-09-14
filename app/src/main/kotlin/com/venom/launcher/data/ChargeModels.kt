package com.venom.launcher.data

/** How the device is being powered right now. */
enum class PlugType(val label: String) {
    NONE("On battery"),
    AC("Charger"),
    USB("USB"),
    WIRELESS("Wireless"),
    DOCK("Dock"),
    UNKNOWN("Powered"),
}

/** Human-readable charging speed, derived from measured watts. */
enum class ChargeSpeed(val label: String, val minWatts: Float) {
    IDLE("Idle", 0f),
    TRICKLE("Trickle", 0.1f),
    SLOW("Slow", 2.5f),
    NORMAL("Normal", 7.5f),
    FAST("Fast", 15f),
    TURBO("Turbo", 30f),
    HYPER("Hyper", 60f),
    ;

    companion object {
        fun fromWatts(watts: Float): ChargeSpeed =
            if (watts <= 0f) IDLE else entries.lastOrNull { watts >= it.minWatts } ?: SLOW
    }
}

data class BatteryState(
    val percent: Int = 0,
    val isCharging: Boolean = false,
    val plugType: PlugType = PlugType.NONE,
    val temperatureC: Float = 0f,
    val voltageV: Float = 0f,
    val currentMa: Int = 0,
    val watts: Float = 0f,
    val speed: ChargeSpeed = ChargeSpeed.IDLE,
    val technology: String = "",
    val health: String = "Unknown",
    val chargedMah: Int = 0,
    val estimatedCapacityMah: Int = 0,
    val minutesToFull: Int? = null,
    val minutesToEmpty: Int? = null,
    val isOverheating: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
)

/** One completed (or in-progress) charging session. */
@kotlinx.serialization.Serializable
data class ChargeSession(
    val startedAt: Long = 0L,
    val endedAt: Long = 0L,
    val startPercent: Int = 0,
    val endPercent: Int = 0,
    val peakWatts: Float = 0f,
    val peakTempC: Float = 0f,
    val plugType: String = PlugType.UNKNOWN.name,
) {
    val durationMinutes: Int get() = ((endedAt - startedAt) / 60_000L).toInt()
    val gainedPercent: Int get() = (endPercent - startPercent).coerceAtLeast(0)
}

/** A single point on the charging curve. */
@kotlinx.serialization.Serializable
data class ChargeSample(
    val t: Long,
    val percent: Int,
    val watts: Float,
    val tempC: Float,
)

@kotlinx.serialization.Serializable
data class ChargeHistory(
    val sessions: List<ChargeSession> = emptyList(),
    val samples: List<ChargeSample> = emptyList(),
)
