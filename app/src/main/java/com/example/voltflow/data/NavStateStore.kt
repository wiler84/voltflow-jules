package com.example.voltflow.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.navDataStore by preferencesDataStore(name = "nav_state")

class NavStateStore(private val context: Context) {
    private val lastRouteKey = stringPreferencesKey("last_route")
    private val lastUserIdKey = stringPreferencesKey("last_user_id")

    suspend fun readRoute(userId: String?): String? {
        if (userId == null) return null
        return context.navDataStore.data
            .map { prefs ->
                if (prefs[lastUserIdKey] == userId) prefs[lastRouteKey] else null
            }
            .first()
    }

    suspend fun saveRoute(userId: String, route: String) {
        context.navDataStore.edit { prefs ->
            prefs[lastUserIdKey] = userId
            prefs[lastRouteKey] = route
        }
    }

    suspend fun clear() {
        context.navDataStore.edit { prefs ->
            prefs.remove(lastRouteKey)
            prefs.remove(lastUserIdKey)
        }
    }
}
