package com.venom.launcher.data

import kotlinx.serialization.Serializable

/** An installed game, resolved from the system's game category flags. */
data class GameInfo(
    val label: String,
    val packageName: String,
    val activityName: String,
    val isGame: Boolean = true,
) {
    val componentKey: String get() = "$packageName/$activityName"
}

/** One play session recorded by Game Mode. */
@Serializable
data class GameSession(
    val packageName: String = "",
    val startedAt: Long = 0L,
    val endedAt: Long = 0L,
    val startBattery: Int = 0,
    val endBattery: Int = 0,
    val startTempC: Float = 0f,
    val peakTempC: Float = 0f,
    val avgFps: Float = 0f,
    val minFps: Float = 0f,
) {
    val durationMinutes: Int get() = ((endedAt - startedAt) / 60_000L).toInt()
    val batteryDrained: Int get() = (startBattery - endBattery).coerceAtLeast(0)
}

@Serializable
data class GameSessions(
    val sessions: List<GameSession> = emptyList(),
)

/** Aggregated lifetime stats for one game. */
data class GameStats(
    val packageName: String,
    val sessions: Int,
    val totalMinutes: Int,
    val batteryDrained: Int,
    val peakTempC: Float,
    val avgFps: Float,
    val lastPlayed: Long,
)

/** Live numbers pushed from GameModeService to whoever is watching. */
data class GameTelemetry(
    val packageName: String = "",
    val fps: Int = 0,
    val minFps: Int = 0,
    val frameMs: Float = 0f,
    val batteryPercent: Int = 0,
    val batteryTempC: Float = 0f,
    val isCharging: Boolean = false,
    val sessionMinutes: Int = 0,
)
