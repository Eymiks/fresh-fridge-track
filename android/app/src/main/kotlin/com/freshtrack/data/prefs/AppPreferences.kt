package com.freshtrack.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferences @Inject constructor(private val dataStore: DataStore<Preferences>) {

    companion object {
        val KEY_GUEST_MODE = booleanPreferencesKey("freshtrack-guest-mode")
        val KEY_THEME_MODE = stringPreferencesKey("frigo-theme-mode")
        val KEY_ACCENT_COLOR = stringPreferencesKey("frigo-accent-color")
        val KEY_DENSITY = stringPreferencesKey("frigo-density")
        val KEY_REDUCE_MOTION = booleanPreferencesKey("frigo-reduce-motion")
        val KEY_NOTIF_ENABLED = booleanPreferencesKey("frigo-notif-enabled")
        val KEY_NOTIF_DAYS = intPreferencesKey("frigo-notif-days")
        val KEY_NOTIF_LAST_CHECK = stringPreferencesKey("frigo-notif-last-check")
        val KEY_NOTIF_DONE_IDS = stringSetPreferencesKey("frigo-notif-done-ids")
    }

    val isGuestMode: Flow<Boolean> = dataStore.data.map { it[KEY_GUEST_MODE] ?: false }
    val themeMode: Flow<String> = dataStore.data.map { it[KEY_THEME_MODE] ?: "system" }
    val accentColor: Flow<String> = dataStore.data.map { it[KEY_ACCENT_COLOR] ?: "green" }
    val density: Flow<String> = dataStore.data.map { it[KEY_DENSITY] ?: "normal" }
    val reduceMotion: Flow<Boolean> = dataStore.data.map { it[KEY_REDUCE_MOTION] ?: false }
    val notifEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_NOTIF_ENABLED] ?: true }
    val notifDays: Flow<Int> = dataStore.data.map { it[KEY_NOTIF_DAYS] ?: 3 }
    val notifLastCheck: Flow<String?> = dataStore.data.map { it[KEY_NOTIF_LAST_CHECK] }
    val notifDoneIds: Flow<Set<String>> = dataStore.data.map { it[KEY_NOTIF_DONE_IDS] ?: emptySet() }

    suspend fun setGuestMode(value: Boolean) = dataStore.edit { it[KEY_GUEST_MODE] = value }
    suspend fun setThemeMode(value: String) = dataStore.edit { it[KEY_THEME_MODE] = value }
    suspend fun setAccentColor(value: String) = dataStore.edit { it[KEY_ACCENT_COLOR] = value }
    suspend fun setDensity(value: String) = dataStore.edit { it[KEY_DENSITY] = value }
    suspend fun setReduceMotion(value: Boolean) = dataStore.edit { it[KEY_REDUCE_MOTION] = value }
    suspend fun setNotifEnabled(value: Boolean) = dataStore.edit { it[KEY_NOTIF_ENABLED] = value }
    suspend fun setNotifDays(value: Int) = dataStore.edit { it[KEY_NOTIF_DAYS] = value }
    suspend fun setNotifLastCheck(value: String) = dataStore.edit { it[KEY_NOTIF_LAST_CHECK] = value }
    suspend fun setNotifDoneIds(ids: Set<String>) = dataStore.edit { it[KEY_NOTIF_DONE_IDS] = ids }
    suspend fun addNotifDoneId(id: String) = dataStore.edit {
        val current = it[KEY_NOTIF_DONE_IDS] ?: emptySet()
        it[KEY_NOTIF_DONE_IDS] = current + id
    }
}
