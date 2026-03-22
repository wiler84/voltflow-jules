package com.example.voltflow.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "voltflow_user_prefs")

class UserPreferencesStore(context: Context) {
    private val appContext = context.applicationContext
    private val dataStore = appContext.userPrefsDataStore

    private val darkModeKey = booleanPreferencesKey("dark_mode")

    val darkModeFlow: Flow<Boolean?> = dataStore.data.map { prefs ->
        prefs[darkModeKey]
    }

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[darkModeKey] = enabled }
    }
}
