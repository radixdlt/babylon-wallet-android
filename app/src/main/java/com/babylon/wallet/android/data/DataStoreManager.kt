package com.babylon.wallet.android.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    suspend fun setShowOnboarding(showOnboarding: Boolean) {
        dataStore.edit { preferences ->
            preferences[SHOW_ONBOARDING] = showOnboarding
        }
    }

    suspend fun showOnboarding(): Boolean = dataStore.data
        .map { preferences ->
            preferences[SHOW_ONBOARDING] ?: false
        }.first()

    companion object {
        private val SHOW_ONBOARDING = booleanPreferencesKey("show_onboarding")
    }
}
