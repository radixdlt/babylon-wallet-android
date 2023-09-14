package rdx.works.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    val lastBackupInstant: Flow<Instant?> = dataStore.data
        .map { preferences ->
            preferences[KEY_LAST_BACKUP_INSTANT]?.let {
                Instant.parse(it)
            }
        }

    suspend fun updateLastBackupInstant(backupInstant: Instant) {
        dataStore.edit { preferences ->
            preferences[KEY_LAST_BACKUP_INSTANT] = backupInstant.toString()
        }
    }

    suspend fun removeLastBackupInstant() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_LAST_BACKUP_INSTANT)
        }
    }

    val firstPersonaCreated: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEY_FIRST_PERSONA_CREATED] ?: false
        }

    val isImportFromOlympiaSettingDismissed: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEY_IMPORT_OLYMPIA_WALLET_SETTING_DISMISSED] ?: false
        }

    suspend fun markFactorSourceBackedUp(id: String) {
        dataStore.edit { preferences ->
            val current = preferences[KEY_BACKED_UP_FACTOR_SOURCE_IDS]
            if (current == null) {
                preferences[KEY_BACKED_UP_FACTOR_SOURCE_IDS] = id
            } else {
                preferences[KEY_BACKED_UP_FACTOR_SOURCE_IDS] = listOf(current, id).joinToString(",")
            }
        }
    }

    fun getBackedUpFactorSourceIds(): Flow<Set<String>> {
        return dataStore.data.map { preferences ->
            preferences[KEY_BACKED_UP_FACTOR_SOURCE_IDS]?.split(",").orEmpty().toSet()
        }
    }

    suspend fun markFirstPersonaCreated() {
        dataStore.edit { preferences ->
            preferences[KEY_FIRST_PERSONA_CREATED] = true
        }
    }

    suspend fun markImportFromOlympiaComplete() {
        dataStore.edit { preferences ->
            preferences[KEY_IMPORT_OLYMPIA_WALLET_SETTING_DISMISSED] = true
        }
    }

    fun getLastUsedEpochFlow(address: String): Flow<Long?> {
        return dataStore.data
            .map { preferences ->
                val mapString = preferences[KEY_ACCOUNT_TO_EPOCH_MAP]
                val map = mapString?.let {
                    Json.decodeFromString<Map<String, Long>>(it)
                }.orEmpty()
                map[address]
            }
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

    suspend fun clear() = dataStore.edit { it.clear() }

    companion object {
        private val KEY_FIRST_PERSONA_CREATED = booleanPreferencesKey("first_persona_created")
        private val KEY_ACCOUNT_TO_EPOCH_MAP = stringPreferencesKey("account_to_epoch_map")
        private val KEY_LAST_BACKUP_INSTANT = stringPreferencesKey("last_backup_instant")
        private val KEY_BACKED_UP_FACTOR_SOURCE_IDS = stringPreferencesKey("backed_up_factor_source_ids")
        private val KEY_IMPORT_OLYMPIA_WALLET_SETTING_DISMISSED =
            booleanPreferencesKey("import_olympia_wallet_setting_dismissed")
    }
}
