package com.babylon.wallet.android.fakes

import androidx.datastore.preferences.core.Preferences
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Epoch
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.extensions.forNetwork
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.samples.sample
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import rdx.works.core.domain.cloudbackup.LastCloudBackupEvent
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.allEntitiesOnCurrentNetwork
import rdx.works.core.sargon.changeGateway
import rdx.works.core.sargon.factorSourceId
import java.time.Instant

class FakePreferenceManager : PreferencesManager {

    private val _transactionCompleteCounter = MutableStateFlow(0)

    private val _lastNPSSurveyInstant = MutableStateFlow<Instant?>(null)
    override val surveyUuid: Flow<String>
        get() = TODO("Not yet implemented")
    override val lastCloudBackupEvent: Flow<LastCloudBackupEvent?>
        get() = TODO("Not yet implemented")
    override val lastManualBackupInstant: Flow<Instant?>
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

    override suspend fun updateLastCloudBackupEvent(lastCloudBackupEvent: LastCloudBackupEvent) {
        TODO("Not yet implemented")
    }

    override suspend fun removeLastCloudBackupEvent() {
        TODO("Not yet implemented")
    }

    override suspend fun updateLastManualBackupInstant(lastManualBackupInstant: Instant) {
        TODO("Not yet implemented")
    }

    override suspend fun isUsingDeprecatedCloudBackup(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun clearDeprecatedCloudBackupIndicator() {
        TODO("Not yet implemented")
    }

    override suspend fun markFirstPersonaCreated() {
        TODO("Not yet implemented")
    }

    override suspend fun markImportFromOlympiaComplete() {
        TODO("Not yet implemented")
    }

    override fun getBackedUpFactorSourceIds(): Flow<Set<FactorSourceId.Hash>> {
        val profile = Profile.sample().changeGateway(Gateway.forNetwork(NetworkId.MAINNET))
        return flowOf(
            profile.factorSources
                .filter {
                    it.id is FactorSourceId.Hash
                }.map {
                    it.id as FactorSourceId.Hash
                }
                .toSet()
        )
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

    override val mobileConnectDelaySeconds: Flow<Int>
        get() = TODO("Not yet implemented")

    override suspend fun updateMobileConnectDelaySeconds(seconds: Int) {
        TODO("Not yet implemented")
    }

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