package com.venom.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.gameStore by preferencesDataStore(name = "venom_games")

/** Play-session history. Readable from a background service, so no DI. */
class GameStore(private val context: Context) {

    val sessions: Flow<List<GameSession>> = context.gameStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            prefs[KEY]?.let { raw ->
                runCatching { VenomJson.Json.decodeFromString<GameSessions>(raw) }.getOrNull()
            }?.sessions ?: emptyList()
        }

    suspend fun current(): List<GameSession> = sessions.first()

    suspend fun add(session: GameSession) {
        context.gameStore.edit { prefs ->
            val next = (current() + session).takeLast(400)
            prefs[KEY] = VenomJson.Json.encodeToString(GameSessions(next))
        }
    }

    suspend fun clear() {
        context.gameStore.edit { it.clear() }
    }

    private companion object {
        val KEY = stringPreferencesKey("sessions_json")
    }
}

private fun emptyPreferences(): Preferences =
    androidx.datastore.preferences.core.emptyPreferences()
