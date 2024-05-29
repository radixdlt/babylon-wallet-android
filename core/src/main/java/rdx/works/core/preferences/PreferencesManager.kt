package rdx.works.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Epoch
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceIdFromHash
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.extensions.hex
import com.radixdlt.sargon.extensions.hexToBagOfBytes
import com.radixdlt.sargon.extensions.init
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
import rdx.works.core.domain.cloudbackup.LastCloudBackupEvent
import java.time.Instant
import javax.inject.Inject

@Suppress("TooManyFunctions")
interface PreferencesManager {
    val surveyUuid: Flow<String>
    val lastCloudBackupEvent: Flow<LastCloudBackupEvent?>
    val lastManualBackupInstant: Flow<Instant?>
    val firstPersonaCreated: Flow<Boolean>
    val isImportFromOlympiaSettingDismissed: Flow<Boolean>
    val isDeviceRootedDialogShown: Flow<Boolean>
    val isCrashReportingEnabled: Flow<Boolean>
    val isRadixBannerVisible: Flow<Boolean>
    val isLinkConnectionStatusIndicatorEnabled: Flow<Boolean>
    val lastNPSSurveyInstant: Flow<Instant?>
    val transactionCompleteCounter: Flow<Int>
    val lastSyncedAccountsWithCE: Flow<String?>
    val showRelinkConnectorsAfterUpdate: Flow<Boolean?>
    val showRelinkConnectorsAfterProfileRestore: Flow<Boolean>

    suspend fun updateLastCloudBackupEvent(lastCloudBackupEvent: LastCloudBackupEvent)

    suspend fun removeLastCloudBackupEvent()

    suspend fun updateLastManualBackupInstant(lastManualBackupInstant: Instant)

    suspend fun isUsingDeprecatedCloudBackup(): Boolean

    suspend fun clearDeprecatedCloudBackupIndicator()

    suspend fun markFirstPersonaCreated()

    suspend fun markImportFromOlympiaComplete()

    fun getBackedUpFactorSourceIds(): Flow<Set<FactorSourceId.Hash>>

    suspend fun markFactorSourceBackedUp(id: FactorSourceId.Hash)

    suspend fun enableCrashReporting(enabled: Boolean)

    suspend fun setRadixBannerVisibility(isVisible: Boolean)

    fun getLastUsedEpochFlow(address: AccountAddress): Flow<Epoch?>

    suspend fun updateEpoch(account: AccountAddress, epoch: Epoch)

    suspend fun markDeviceRootedDialogShown()

    suspend fun setLinkConnectionStatusIndicator(isEnabled: Boolean)

    suspend fun incrementTransactionCompleteCounter()

    suspend fun updateLastNPSSurveyInstant(npsSurveyInstant: Instant)

    suspend fun updateLastSyncedAccountsWithCE(accountsHash: String)

    suspend fun removeLastSyncedAccountsWithCE()

    suspend fun setShowRelinkConnectorsAfterUpdate(show: Boolean)

    suspend fun setShowRelinkConnectorsAfterProfileRestore(show: Boolean)

    suspend fun clearShowRelinkConnectors()

    suspend fun clear(): Preferences
}

@Suppress("TooManyFunctions") // TODO maybe break it into two or more classes
class PreferencesManagerImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : PreferencesManager {

    override val surveyUuid: Flow<String>
        get() = dataStore.data.map { preferences ->
            preferences[KEY_SURVEY_UUID]
        }.onStart {
            val existingUUID = dataStore.data.map { it[KEY_SURVEY_UUID] }.firstOrNull()
            if (existingUUID.isNullOrEmpty()) {
                dataStore.edit { preferences ->
                    preferences[KEY_SURVEY_UUID] = UUIDGenerator.uuid().toString()
                }
            }
        }.filterNotNull()

    override val lastCloudBackupEvent: Flow<LastCloudBackupEvent?> = dataStore.data
        .map { preferences ->
            preferences[KEY_LAST_CLOUD_BACKUP_EVENT]?.let { Json.decodeFromString(it) }
        }
    override val lastManualBackupInstant: Flow<Instant?> = dataStore.data
        .map { preferences ->
            preferences[KEY_LAST_MANUAL_BACKUP_INSTANT]?.let {
                Instant.parse(it)
            }
        }

    override val showRelinkConnectorsAfterUpdate: Flow<Boolean?> = dataStore.data
        .map { preferences ->
            preferences[KEY_SHOW_RELINK_CONNECTORS_AFTER_UPDATE]
        }

    override val showRelinkConnectorsAfterProfileRestore: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEY_SHOW_RELINK_CONNECTORS_AFTER_PROFILE_RESTORE] ?: false
        }

    override suspend fun updateLastCloudBackupEvent(lastCloudBackupEvent: LastCloudBackupEvent) {
        dataStore.edit { preferences ->
            preferences[KEY_LAST_CLOUD_BACKUP_EVENT] = Json.encodeToString(lastCloudBackupEvent)
        }
    }

    override suspend fun removeLastCloudBackupEvent() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_LAST_CLOUD_BACKUP_EVENT)
        }
    }

    override suspend fun updateLastManualBackupInstant(lastManualBackupInstant: Instant) {
        dataStore.edit { preferences ->
            preferences[KEY_LAST_MANUAL_BACKUP_INSTANT] = lastManualBackupInstant.toString()
        }
    }

    override suspend fun isUsingDeprecatedCloudBackup(): Boolean = dataStore.data
        .map { preferences ->
            preferences[KEY_DEPRECATED_BACKUP_SYSTEM_INDICATOR] != null
        }.firstOrNull() ?: false

    override suspend fun clearDeprecatedCloudBackupIndicator() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_DEPRECATED_BACKUP_SYSTEM_INDICATOR)
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

    override val lastSyncedAccountsWithCE: Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[KEY_LAST_SYNCED_ACCOUNTS_WITH_CE]
        }

    override suspend fun markImportFromOlympiaComplete() {
        dataStore.edit { preferences ->
            preferences[KEY_IMPORT_OLYMPIA_WALLET_SETTING_DISMISSED] = true
        }
    }

    override fun getBackedUpFactorSourceIds(): Flow<Set<FactorSourceId.Hash>> {
        return dataStore.data.map { preferences ->
            preferences[KEY_BACKED_UP_FACTOR_SOURCE_IDS]?.split(",").orEmpty().map { hex ->
                FactorSourceId.Hash(
                    FactorSourceIdFromHash(kind = FactorSourceKind.DEVICE, body = Exactly32Bytes.init(hex.hexToBagOfBytes()))
                )
            }.toSet()
        }
    }

    override val isCrashReportingEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEY_CRASH_REPORTING_ENABLED] ?: BuildConfig.CRASH_REPORTING_ENABLED
        }

    override suspend fun markFactorSourceBackedUp(id: FactorSourceId.Hash) {
        dataStore.edit { preferences ->
            val current = preferences[KEY_BACKED_UP_FACTOR_SOURCE_IDS]
            val idHex = id.value.body.hex
            if (current == null) {
                preferences[KEY_BACKED_UP_FACTOR_SOURCE_IDS] = idHex
            } else {
                preferences[KEY_BACKED_UP_FACTOR_SOURCE_IDS] = listOf(current, idHex).joinToString(",")
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

    override suspend fun updateLastSyncedAccountsWithCE(accountsHash: String) {
        dataStore.edit { preferences ->
            preferences[KEY_LAST_SYNCED_ACCOUNTS_WITH_CE] = accountsHash
        }
    }

    override suspend fun removeLastSyncedAccountsWithCE() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_LAST_SYNCED_ACCOUNTS_WITH_CE)
        }
    }

    override suspend fun setShowRelinkConnectorsAfterUpdate(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_SHOW_RELINK_CONNECTORS_AFTER_UPDATE] = show
        }
    }

    override suspend fun setShowRelinkConnectorsAfterProfileRestore(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_SHOW_RELINK_CONNECTORS_AFTER_PROFILE_RESTORE] = show
        }
    }

    override suspend fun clearShowRelinkConnectors() {
        dataStore.edit { preferences ->
            preferences[KEY_SHOW_RELINK_CONNECTORS_AFTER_UPDATE] = false
            preferences[KEY_SHOW_RELINK_CONNECTORS_AFTER_PROFILE_RESTORE] = false
        }
    }

    override suspend fun clear() = dataStore.edit { it.clear() }

    companion object {
        val KEY_CRASH_REPORTING_ENABLED = booleanPreferencesKey("crash_reporting_enabled")
        val KEY_FIRST_PERSONA_CREATED = booleanPreferencesKey("first_persona_created")
        val KEY_RADIX_BANNER_VISIBLE = booleanPreferencesKey("radix_banner_visible")
        val KEY_ACCOUNT_TO_EPOCH_MAP = stringPreferencesKey("account_to_epoch_map")
        val KEY_LAST_CLOUD_BACKUP_EVENT = stringPreferencesKey("last_cloud_backup_event")
        val KEY_DEPRECATED_BACKUP_SYSTEM_INDICATOR = stringPreferencesKey("last_backup_instant")
        val KEY_LAST_MANUAL_BACKUP_INSTANT = stringPreferencesKey("last_manual_backup_instant")
        val KEY_BACKED_UP_FACTOR_SOURCE_IDS = stringPreferencesKey("backed_up_factor_source_ids")
        val KEY_IMPORT_OLYMPIA_WALLET_SETTING_DISMISSED = booleanPreferencesKey("import_olympia_wallet_setting_dismissed")
        val KEY_DEVICE_ROOTED_DIALOG_SHOWN = booleanPreferencesKey("device_rooted_dialog_shown")
        val KEY_LINK_CONNECTION_STATUS_INDICATOR = booleanPreferencesKey("link_connection_status_indicator")
        val KEY_TRANSACTIONS_COMPLETE_COUNT = intPreferencesKey("transaction_complete_count")
        val KEY_SHOW_NPS_SURVEY_INSTANT = stringPreferencesKey("show_nps_survey_instant")
        val KEY_SURVEY_UUID = stringPreferencesKey("uuid")
        val KEY_LAST_SYNCED_ACCOUNTS_WITH_CE = stringPreferencesKey("last_synced_accounts_with_ce")
        val KEY_SHOW_RELINK_CONNECTORS_AFTER_UPDATE = booleanPreferencesKey("show_relink_connectors_after_update")
        val KEY_SHOW_RELINK_CONNECTORS_AFTER_PROFILE_RESTORE = booleanPreferencesKey("show_relink_connectors_after_profile_restore")
    }
}
