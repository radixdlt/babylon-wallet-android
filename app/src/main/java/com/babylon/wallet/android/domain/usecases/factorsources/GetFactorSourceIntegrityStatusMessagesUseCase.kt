package com.babylon.wallet.android.domain.usecases.factorsources

import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceStatusMessage
import com.radixdlt.sargon.EntitiesLinkedToFactorSource
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceIntegrity
import com.radixdlt.sargon.extensions.id
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import rdx.works.core.preferences.PreferencesManager
import javax.inject.Inject

class GetFactorSourceIntegrityStatusMessagesUseCase @Inject constructor(
    private val getEntitiesLinkedToDeviceFactorSourceUseCase: GetEntitiesLinkedToDeviceFactorSourceUseCase,
    private val preferencesManager: PreferencesManager,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    suspend fun forDeviceFactorSources(
        deviceFactorSources: List<FactorSource.Device>
    ): Map<FactorSourceId, List<FactorSourceStatusMessage>> = withContext(defaultDispatcher) {
        deviceFactorSources.mapNotNull { deviceFactorSource ->
            val entitiesLinkedToDeviceFactorSource = getEntitiesLinkedToDeviceFactorSourceUseCase(deviceFactorSource)
                ?: return@mapNotNull null
            deviceFactorSource.id to forDeviceFactorSource(
                deviceFactorSourceId = deviceFactorSource.id,
                entitiesLinkedToDeviceFactorSource = entitiesLinkedToDeviceFactorSource
            )
        }.toMap()
    }

    suspend fun forDeviceFactorSource(
        deviceFactorSourceId: FactorSourceId,
        entitiesLinkedToDeviceFactorSource: EntitiesLinkedToFactorSource
    ): List<FactorSourceStatusMessage> {
        val isDeviceFactorSourceLinkedToAnyEntities = listOf(
            entitiesLinkedToDeviceFactorSource.accounts,
            entitiesLinkedToDeviceFactorSource.personas,
            entitiesLinkedToDeviceFactorSource.hiddenAccounts,
            entitiesLinkedToDeviceFactorSource.hiddenPersonas
        ).any { it.isNotEmpty() }

        val backedUpFactorSourceIds = preferencesManager.getBackedUpFactorSourceIds().firstOrNull().orEmpty()

        return if (isDeviceFactorSourceLinkedToAnyEntities) {
            val deviceFactorSourceIntegrity = entitiesLinkedToDeviceFactorSource.integrity as FactorSourceIntegrity.Device
            deviceFactorSourceIntegrity.toMessages().toPersistentList()
        } else if (backedUpFactorSourceIds.contains(deviceFactorSourceId)) { // if not linked entities we can't check
            // the integrity, but we can check if the user backed up the seed phrase
            listOf(FactorSourceStatusMessage.NoSecurityIssues)
        } else {
            // otherwise we don't show any warnings
            emptyList()
        }
    }

    private fun FactorSourceIntegrity.Device.toMessages(): List<FactorSourceStatusMessage> {
        val securityMessages = listOfNotNull(
            FactorSourceStatusMessage.SecurityPrompt.WriteDownSeedPhrase.takeIf {
                this.v1.isMnemonicMarkedAsBackedUp.not()
            },
            FactorSourceStatusMessage.SecurityPrompt.RecoveryRequired.takeIf {
                this.v1.isMnemonicPresentInSecureStorage.not()
            }
        )
        return securityMessages.ifEmpty {
            listOf(FactorSourceStatusMessage.NoSecurityIssues)
        }
    }
}
