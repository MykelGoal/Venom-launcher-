package com.venom.launcher.data

import kotlinx.serialization.Serializable

/**
 * Per-game behaviour for Game Mode / Game Turbo.
 *
 * The nullable fields mean "follow the global setting" — so a game you haven't
 * touched yet behaves like every other game, and one you've tuned keeps its
 * own personality (e.g. DND for Call of Duty, brightness boost for a dark RPG).
 */
@Serializable
data class GameProfile(
    val packageName: String = "",
    val boostOnLaunch: Boolean? = null,
    val dndWhilePlaying: Boolean? = null,
    val muteOnLaunch: Boolean? = null,
    val brightnessPercent: Int? = null,
) {
    companion object {
        fun defaultFor(packageName: String) = GameProfile(packageName = packageName)
    }
}

@Serializable
data class GameProfileMap(
    val profiles: Map<String, GameProfile> = emptyMap(),
)
