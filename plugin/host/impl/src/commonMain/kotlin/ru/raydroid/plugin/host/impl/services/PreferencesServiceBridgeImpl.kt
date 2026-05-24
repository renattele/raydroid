package ru.raydroid.plugin.host.impl.services

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.raydroid.plugin.api.host.transport.PreferencesServiceBridge

internal class PreferencesServiceBridgeImpl(
    private val dataStore: DataStore<Preferences>,
) : PreferencesServiceBridge {
    override fun get(key: String): Flow<String?> =
        dataStore.data.map { preferences ->
            preferences[stringPreferencesKey(key)]
        }

    override suspend fun set(
        key: String,
        value: String,
    ) {
        dataStore.updateData {
            it.toMutablePreferences().also { preferences ->
                preferences[stringPreferencesKey(key)] = value
            }
        }
    }
}
