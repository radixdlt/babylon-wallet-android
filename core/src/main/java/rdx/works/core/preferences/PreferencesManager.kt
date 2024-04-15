package rdx.works.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Epoch
import com.radixdlt.sargon.extensions.string
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rdx.works.core.BuildConfig
import rdx.works.core.UUIDGenerator
import java.time.Instant
import javax.inject.Inject

@Suppress("TooManyFunctions")
interface PreferencesManager {
    val uuid: Flow<String>
    val lastBackupInstant: Flow<Instant?>
    val firstPersonaCreated: Flow<Boolean>
    val isImportFromOlympiaSettingDismissed: Flow<Boolean>
    val isDeviceRootedDialogShown: Flow<Boolean>
    val isCrashReportingEnabled: Flow<Boolean>
    val isRadixBannerVisible: Flow<Boolean>
    val isLinkConnectionStatusIndicatorEnabled: Flow<Boolean>
    val lastNPSSurveyInstant: Flow<Instant?>
    val transactionCompleteCounter: Flow<Int>
    val connectorExtensionLinkPublicKey: Flow<String?>

    suspend fun updateLastBackupInstant(backupInstant: Instant)

    suspend fun removeLastBackupInstant()

    suspend fun markFirstPersonaCreated()

    suspend fun markImportFromOlympiaComplete()

    fun getBackedUpFactorSourceIds(): Flow<Set<String>>

    suspend fun markFactorSourceBackedUp(id: String)

    suspend fun enableCrashReporting(enabled: Boolean)

    suspend fun setRadixBannerVisibility(isVisible: Boolean)
    fun getLastUsedEpochFlow(address: AccountAddress): Flow<Epoch?>

    suspend fun updateEpoch(account: AccountAddress, epoch: Epoch)

    suspend fun markDeviceRootedDialogShown()

    suspend fun setLinkConnectionStatusIndicator(isEnabled: Boolean)
    suspend fun incrementTransactionCompleteCounter()

    suspend fun updateLastNPSSurveyInstant(npsSurveyInstant: Instant)

    suspend fun setConnectorExtensionLinkPublicKey(value: String)

    suspend fun clear(): Preferences
}

@Suppress("TooManyFunctions")
class PreferencesManagerImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : PreferencesManager {

    override val uuid: Flow<String>
        get() = dataStore.data.map { preferences ->
            preferences[KEY_UUID]
        }.onStart {
            val existingUUID = dataStore.data.map { it[KEY_UUID] }.firstOrNull()
            if (existingUUID.isNullOrEmpty()) {
                dataStore.edit { preferences ->
                    preferences[KEY_UUID] = UUIDGenerator.uuid().toString()
                }
            }
        }.filterNotNull()

    override val lastBackupInstant: Flow<Instant?> = dataStore.data
        .map { preferences ->
            preferences[KEY_LAST_BACKUP_INSTANT]?.let {
                Instant.parse(it)
            }
        }

    override suspend fun updateLastBackupInstant(backupInstant: Instant) {
        dataStore.edit { preferences ->
            preferences[KEY_LAST_BACKUP_INSTANT] = backupInstant.toString()
        }
    }

    override suspend fun removeLastBackupInstant() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_LAST_BACKUP_INSTANT)
        }
    }

    override val firstPersonaCreated: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEY_FIRST_PERSONA_CREATED] ?: false
        }

    override suspend fun markFirstPersonaCreated() {
        dataStore.edit { preferences ->
            preferences[KEY_FIRST_PERSONA_CREATED] = true
        }
    }

    override val isImportFromOlympiaSettingDismissed: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEY_IMPORT_OLYMPIA_WALLET_SETTING_DISMISSED] ?: false
        }

    override val isDeviceRootedDialogShown: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEY_DEVICE_ROOTED_DIALOG_SHOWN] ?: false
        }

    override suspend fun markImportFromOlympiaComplete() {
        dataStore.edit { preferences ->
            preferences[KEY_IMPORT_OLYMPIA_WALLET_SETTING_DISMISSED] = true
        }
    }

    override fun getBackedUpFactorSourceIds(): Flow<Set<String>> {
        return dataStore.data.map { preferences ->
            preferences[KEY_BACKED_UP_FACTOR_SOURCE_IDS]?.split(",").orEmpty().toSet()
        }
    }

    override val isCrashReportingEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEY_CRASH_REPORTING_ENABLED] ?: BuildConfig.CRASH_REPORTING_ENABLED
        }

    override suspend fun markFactorSourceBackedUp(id: String) {
        dataStore.edit { preferences ->
            val current = preferences[KEY_BACKED_UP_FACTOR_SOURCE_IDS]
            if (current == null) {
                preferences[KEY_BACKED_UP_FACTOR_SOURCE_IDS] = id
            } else {
                preferences[KEY_BACKED_UP_FACTOR_SOURCE_IDS] = listOf(current, id).joinToString(",")
            }
        }
    }

    override suspend fun enableCrashReporting(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_CRASH_REPORTING_ENABLED] = enabled
        }
    }

    override val isRadixBannerVisible: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEY_RADIX_BANNER_VISIBLE] ?: false
        }

    override suspend fun setRadixBannerVisibility(isVisible: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_RADIX_BANNER_VISIBLE] = isVisible
        }
    }

    override fun getLastUsedEpochFlow(address: AccountAddress): Flow<Epoch?> {
        return dataStore.data
            .map { preferences ->
                val mapString = preferences[KEY_ACCOUNT_TO_EPOCH_MAP]
                val map = mapString?.let {
                    Json.decodeFromString<Map<String, Long>>(it)
                }.orEmpty()
                map[address.string]?.toULong()
            }
    }

    override suspend fun updateEpoch(account: AccountAddress, epoch: Epoch) {
        dataStore.edit { preferences ->
            val mapString = preferences[KEY_ACCOUNT_TO_EPOCH_MAP]
            val map = mapString?.let {
                Json.decodeFromString<Map<String, Long>>(it)
            }?.toMutableMap() ?: mutableMapOf()
            map[account.string] = epoch.toLong()
            preferences[KEY_ACCOUNT_TO_EPOCH_MAP] = Json.encodeToString<Map<String, Long>>(map)
        }
    }

    override suspend fun markDeviceRootedDialogShown() {
        dataStore.edit { preferences ->
            preferences[KEY_DEVICE_ROOTED_DIALOG_SHOWN] = true
        }
    }

    override val isLinkConnectionStatusIndicatorEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEY_LINK_CONNECTION_STATUS_INDICATOR] ?: false
        }

    override suspend fun setLinkConnectionStatusIndicator(isEnabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_LINK_CONNECTION_STATUS_INDICATOR] = isEnabled
        }
    }

    override suspend fun incrementTransactionCompleteCounter() {
        dataStore.edit { preferences ->
            val oldValue = preferences[KEY_TRANSACTIONS_COMPLETE_COUNT] ?: 0
            preferences[KEY_TRANSACTIONS_COMPLETE_COUNT] = oldValue + 1
        }
    }

    override val lastNPSSurveyInstant: Flow<Instant?> = dataStore.data
        .map { preferences ->
            preferences[KEY_SHOW_NPS_SURVEY_INSTANT]?.let {
                Instant.parse(it)
            }
        }
    override val transactionCompleteCounter: Flow<Int>
        get() = dataStore.data.map { preferences ->
            preferences[KEY_TRANSACTIONS_COMPLETE_COUNT] ?: 0
        }

    override suspend fun updateLastNPSSurveyInstant(npsSurveyInstant: Instant) {
        dataStore.edit { preferences ->
            preferences[KEY_SHOW_NPS_SURVEY_INSTANT] = npsSurveyInstant.toString()
        }
    }

    override suspend fun setConnectorExtensionLinkPublicKey(value: String) {
        dataStore.edit { preferences ->
            preferences[KEY_CE_LINK_PUBLIC_KEY] = value
        }
    }

    override val connectorExtensionLinkPublicKey: Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[KEY_CE_LINK_PUBLIC_KEY]
        }

    override suspend fun clear() = dataStore.edit { it.clear() }

    companion object {
        val KEY_CRASH_REPORTING_ENABLED = booleanPreferencesKey("crash_reporting_enabled")
        val KEY_FIRST_PERSONA_CREATED = booleanPreferencesKey("first_persona_created")
        val KEY_RADIX_BANNER_VISIBLE = booleanPreferencesKey("radix_banner_visible")
        val KEY_ACCOUNT_TO_EPOCH_MAP = stringPreferencesKey("account_to_epoch_map")
        val KEY_LAST_BACKUP_INSTANT = stringPreferencesKey("last_backup_instant")
        val KEY_BACKED_UP_FACTOR_SOURCE_IDS = stringPreferencesKey("backed_up_factor_source_ids")
        val KEY_IMPORT_OLYMPIA_WALLET_SETTING_DISMISSED = booleanPreferencesKey("import_olympia_wallet_setting_dismissed")
        val KEY_DEVICE_ROOTED_DIALOG_SHOWN = booleanPreferencesKey("device_rooted_dialog_shown")
        val KEY_LINK_CONNECTION_STATUS_INDICATOR = booleanPreferencesKey("link_connection_status_indicator")
        val KEY_TRANSACTIONS_COMPLETE_COUNT = intPreferencesKey("transaction_complete_count")
        val KEY_SHOW_NPS_SURVEY_INSTANT = stringPreferencesKey("show_nps_survey_instant")
        val KEY_UUID = stringPreferencesKey("uuid")
        val KEY_CE_LINK_PUBLIC_KEY = stringPreferencesKey("KEY_CE_LINK_PUBLIC_KEY")
    }
}
