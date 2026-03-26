package com.example.voltflow.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.security.MessageDigest

private val Context.userPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "voltflow_user_prefs")

class UserPreferencesStore(context: Context) {
    private val appContext = context.applicationContext
    private val dataStore = appContext.userPrefsDataStore

    private val darkModeKey = booleanPreferencesKey("dark_mode")
    private val lastInteractionKey = longPreferencesKey("last_interaction")
    private val pinPrefs by lazy { createSecurePrefs(appContext) }
    private val pinHashKey = "pin_hash"

    val darkModeFlow: Flow<Boolean?> = dataStore.data.map { prefs ->
        prefs[darkModeKey]
    }

    val lastInteractionFlow: Flow<Long?> = dataStore.data.map { prefs ->
        prefs[lastInteractionKey]
    }

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[darkModeKey] = enabled }
    }

    suspend fun setLastInteraction(timestamp: Long) {
        dataStore.edit { prefs -> prefs[lastInteractionKey] = timestamp }
    }

    suspend fun setPin(pin: String) {
        withContext(Dispatchers.IO) {
            pinPrefs.edit().putString(pinHashKey, hashPin(pin)).apply()
        }
    }

    suspend fun clearPin() {
        withContext(Dispatchers.IO) {
            pinPrefs.edit().remove(pinHashKey).apply()
        }
    }

    suspend fun hasPin(): Boolean = withContext(Dispatchers.IO) {
        !pinPrefs.getString(pinHashKey, null).isNullOrBlank()
    }

    suspend fun verifyPin(pin: String): Boolean = withContext(Dispatchers.IO) {
        val stored = pinPrefs.getString(pinHashKey, null) ?: return@withContext false
        stored == hashPin(pin)
    }

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun createSecurePrefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        "voltflow_secure_prefs",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}
