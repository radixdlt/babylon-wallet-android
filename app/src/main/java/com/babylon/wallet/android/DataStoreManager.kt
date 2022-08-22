package com.babylon.wallet.android

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

fun <T> DataStore<Preferences>.get(
    key: Preferences.Key<T>,
    defaultValue: T
): T = runBlocking {
    data.first()[key] ?: defaultValue
}
fun <T> DataStore<Preferences>.set(
    key: Preferences.Key<T>,
    value: T?
) = runBlocking<Unit> {
    edit {
        if (value == null) {
            it.remove(key)
        } else {
            it[key] = value
        }
    }
}

class DataStoreManager(
    private val dataStore: DataStore<Preferences>
) {
    fun showOnboarding(): Boolean = dataStore.get(SHOW_ONBOARDING, true)

    fun setShowOnboarding(showOnboarding: Boolean) {
        dataStore.set(SHOW_ONBOARDING, showOnboarding)
    }

    companion object {
        private val SHOW_ONBOARDING = booleanPreferencesKey("show_onboarding")
    }
}
