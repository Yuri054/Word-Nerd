package com.wordnerd.dictionary.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class DailyNotificationSettings(
    val enabled: Boolean = false,
    val hour: Int = 9,
    val minute: Int = 0,
)

class SettingsRepository(private val context: Context) {
    private val enabledKey = booleanPreferencesKey("daily_enabled")
    private val hourKey = intPreferencesKey("daily_hour")
    private val minuteKey = intPreferencesKey("daily_minute")

    val daily: Flow<DailyNotificationSettings> = context.dataStore.data.map {
        DailyNotificationSettings(
            enabled = it[enabledKey] ?: false,
            hour = it[hourKey] ?: 9,
            minute = it[minuteKey] ?: 0,
        )
    }

    suspend fun current(): DailyNotificationSettings = daily.first()

    suspend fun setEnabled(enabled: Boolean) {
        context.dataStore.edit { it[enabledKey] = enabled }
    }

    suspend fun setTime(hour: Int, minute: Int) {
        context.dataStore.edit { it[hourKey] = hour; it[minuteKey] = minute }
    }

    // ── App updates ──
    private val autoUpdateKey = booleanPreferencesKey("auto_update_check")
    private val lastUpdateCheckKey = longPreferencesKey("last_update_check")

    val autoUpdateCheck: Flow<Boolean> = context.dataStore.data.map { it[autoUpdateKey] ?: true }

    suspend fun setAutoUpdateCheck(enabled: Boolean) {
        context.dataStore.edit { it[autoUpdateKey] = enabled }
    }

    suspend fun lastUpdateCheck(): Long = context.dataStore.data.first()[lastUpdateCheckKey] ?: 0L

    suspend fun setLastUpdateCheck(time: Long) {
        context.dataStore.edit { it[lastUpdateCheckKey] = time }
    }
}
