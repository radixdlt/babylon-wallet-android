package com.babylon.wallet.android.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    suspend fun setShowOnboarding(showOnboarding: Boolean) {
        dataStore.edit { preferences ->
            preferences[SHOW_ONBOARDING] = showOnboarding
        }
    }

    val showOnboarding: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[SHOW_ONBOARDING] ?: false
        }

    suspend fun getLastUsedEpoch(address: String): Long? {
        return dataStore.data
            .map { preferences ->
                val mapString = preferences[KEY_ACCOUNT_TO_EPOCH_MAP]
                val map = mapString?.let {
                    Json.decodeFromString<Map<String, Long>>(it)
                } ?: emptyMap()
                map[address]
            }.firstOrNull()
    }

    suspend fun updateEpoch(account: String, epoch: Long) {
        dataStore.edit { preferences ->
            val mapString = preferences[KEY_ACCOUNT_TO_EPOCH_MAP]
            val map = mapString?.let {
                Json.decodeFromString<Map<String, Long>>(it)
            }?.toMutableMap() ?: mutableMapOf()
            map[account] = epoch
            preferences[KEY_ACCOUNT_TO_EPOCH_MAP] = Json.encodeToString<Map<String, Long>>(map)
        }
    }

    companion object {
        private val SHOW_ONBOARDING = booleanPreferencesKey("show_onboarding")
        private val KEY_ACCOUNT_TO_EPOCH_MAP = stringPreferencesKey("account_to_epoch_map")
    }
}
