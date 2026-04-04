package com.example.voltflow.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private val Context.userPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "voltflow_user_prefs")

class UserPreferencesStore(context: Context) {
    private val appContext = context.applicationContext
    private val dataStore = appContext.userPrefsDataStore

    private val darkModeKey = booleanPreferencesKey("dark_mode")
    private val lastInteractionKey = longPreferencesKey("last_interaction")
    private val pushTokenKey = stringPreferencesKey("push_token")

    val darkModeFlow: Flow<Boolean?> = dataStore.data.map { prefs ->
        prefs[darkModeKey]
    }

    val lastInteractionFlow: Flow<Long?> = dataStore.data.map { prefs ->
        prefs[lastInteractionKey]
    }

    val pushTokenFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[pushTokenKey]
    }

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[darkModeKey] = enabled }
    }

    suspend fun setLastInteraction(timestamp: Long) {
        dataStore.edit { prefs -> prefs[lastInteractionKey] = timestamp }
    }

    suspend fun setPushToken(token: String) {
        dataStore.edit { prefs -> prefs[pushTokenKey] = token }
    }

    suspend fun getPushToken(): String? = dataStore.data.map { prefs -> prefs[pushTokenKey] }.firstOrNull()
}
