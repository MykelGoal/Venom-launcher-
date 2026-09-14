package com.venom.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Charge Lab keeps its two hot settings in the [ChargePrefsCache] mirror so the
 * battery broadcast receiver can read them without blocking.
 */
private val Context.chargeStore by preferencesDataStore(name = "venom_charge")

object ChargePrefs {

    private val LIMIT_ENABLED = booleanPreferencesKey("limit_enabled")
    private val LIMIT_PERCENT = intPreferencesKey("limit_percent")

    var limitEnabled: Boolean
        get() = ChargePrefsCache.enabled
        private set(v) { ChargePrefsCache.enabled = v }

    var limitPercent: Int
        get() = ChargePrefsCache.percent
        private set(v) { ChargePrefsCache.percent = v }

    fun observe(context: Context): Flow<Pair<Boolean, Int>> =
        context.chargeStore.data.map { p ->
            (p[LIMIT_ENABLED] ?: false) to (p[LIMIT_PERCENT] ?: 85)
        }

    suspend fun prime(context: Context) {
        val (enabled, percent) = observe(context).first()
        ChargePrefsCache.enabled = enabled
        ChargePrefsCache.percent = percent
    }

    suspend fun set(context: Context, enabled: Boolean, percent: Int) {
        limitEnabled = enabled
        limitPercent = percent
        context.chargeStore.edit {
            it[LIMIT_ENABLED] = enabled
            it[LIMIT_PERCENT] = percent
        }
    }
}
