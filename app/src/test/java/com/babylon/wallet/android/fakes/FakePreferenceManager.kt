package com.babylon.wallet.android.fakes

import androidx.datastore.preferences.core.Preferences
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Epoch
import com.radixdlt.sargon.FactorSourceId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import rdx.works.core.preferences.PreferencesManager
import java.time.Instant

class FakePreferenceManager : PreferencesManager {

    private val _transactionCompleteCounter = MutableStateFlow(0)

    private val _lastNPSSurveyInstant = MutableStateFlow<Instant?>(null)
    override val uuid: Flow<String>
        get() = TODO("Not yet implemented")
    override val lastBackupInstant: Flow<Instant?>
        get() = TODO("Not yet implemented")
    override val firstPersonaCreated: Flow<Boolean>
        get() = TODO("Not yet implemented")
    override val isImportFromOlympiaSettingDismissed: Flow<Boolean>
        get() = TODO("Not yet implemented")
    override val isDeviceRootedDialogShown: Flow<Boolean>
        get() = TODO("Not yet implemented")
    override val isCrashReportingEnabled: Flow<Boolean>
        get() = TODO("Not yet implemented")
    override val isRadixBannerVisible: Flow<Boolean>
        get() = TODO("Not yet implemented")
    override val isLinkConnectionStatusIndicatorEnabled: Flow<Boolean>
        get() = TODO("Not yet implemented")
    override val lastNPSSurveyInstant: Flow<Instant?>
        get() = _lastNPSSurveyInstant
    override val lastSyncedAccountsWithCE: Flow<String?>
        get() = TODO("Not yet implemented")
    override val showRelinkConnectorsAfterUpdate: Flow<Boolean?>
        get() = TODO("Not yet implemented")
    override val showRelinkConnectorsAfterProfileRestore: Flow<Boolean>
        get() = TODO("Not yet implemented")

    override suspend fun updateLastBackupInstant(backupInstant: Instant) {
        TODO("Not yet implemented")
    }

    override suspend fun removeLastBackupInstant() {
        TODO("Not yet implemented")
    }

    override suspend fun markFirstPersonaCreated() {
        TODO("Not yet implemented")
    }

    override suspend fun markImportFromOlympiaComplete() {
        TODO("Not yet implemented")
    }

    override fun getBackedUpFactorSourceIds(): Flow<Set<FactorSourceId.Hash>> {
        TODO("Not yet implemented")
    }

    override suspend fun markFactorSourceBackedUp(id: FactorSourceId.Hash) {
        TODO("Not yet implemented")
    }

    override suspend fun enableCrashReporting(enabled: Boolean) {
        TODO("Not yet implemented")
    }

    override suspend fun setRadixBannerVisibility(isVisible: Boolean) {
        TODO("Not yet implemented")
    }

    override fun getLastUsedEpochFlow(address: AccountAddress): Flow<Epoch?> {
        TODO("Not yet implemented")
    }

    override suspend fun updateEpoch(account: AccountAddress, epoch: Epoch) {
        TODO("Not yet implemented")
    }

    override suspend fun markDeviceRootedDialogShown() {
        TODO("Not yet implemented")
    }

    override suspend fun setLinkConnectionStatusIndicator(isEnabled: Boolean) {
        TODO("Not yet implemented")
    }

    override val transactionCompleteCounter: Flow<Int>
        get() = _transactionCompleteCounter

    override suspend fun incrementTransactionCompleteCounter() {
        _transactionCompleteCounter.emit(_transactionCompleteCounter.value + 1)
    }

    override suspend fun updateLastNPSSurveyInstant(npsSurveyInstant: Instant) {
        _lastNPSSurveyInstant.emit(npsSurveyInstant)
    }

    override suspend fun updateLastSyncedAccountsWithCE(accountsHash: String) {
        TODO("Not yet implemented")
    }

    override suspend fun removeLastSyncedAccountsWithCE() {
        TODO("Not yet implemented")
    }

    override suspend fun setShowRelinkConnectorsAfterUpdate(show: Boolean) {
        TODO("Not yet implemented")
    }

    override suspend fun setShowRelinkConnectorsAfterProfileRestore(show: Boolean) {
        TODO("Not yet implemented")
    }

    override suspend fun clearShowRelinkConnectors() {
        TODO("Not yet implemented")
    }

    override suspend fun clear(): Preferences {
        TODO("Not yet implemented")
    }

}