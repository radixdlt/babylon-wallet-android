package com.babylon.wallet.android.domain.usecases.factorsources

import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceStatusMessage
import com.radixdlt.sargon.EntitiesLinkedToFactorSource
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceIntegrity
import com.radixdlt.sargon.extensions.id
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetFactorSourceIntegrityStatusMessagesUseCase @Inject constructor(
    private val getEntitiesLinkedToFactorSourceUseCase: GetEntitiesLinkedToFactorSourceUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    suspend fun forDeviceFactorSources(
        deviceFactorSources: List<FactorSource.Device>,
        includeNoIssuesMessage: Boolean,
        checkIntegrityOnlyIfAnyEntitiesLinked: Boolean
    ): Map<FactorSourceId, List<FactorSourceStatusMessage>> = withContext(defaultDispatcher) {
        deviceFactorSources.mapNotNull { deviceFactorSource ->
            val entitiesLinkedToDeviceFactorSource = getEntitiesLinkedToFactorSourceUseCase(deviceFactorSource)
                ?: return@mapNotNull null
            deviceFactorSource.id to forDeviceFactorSource(
                entitiesLinkedToDeviceFactorSource = entitiesLinkedToDeviceFactorSource,
                includeNoIssuesStatus = includeNoIssuesMessage,
                checkIntegrityOnlyIfAnyEntitiesLinked = checkIntegrityOnlyIfAnyEntitiesLinked
            )
        }.toMap()
    }

    fun forDeviceFactorSource(
        entitiesLinkedToDeviceFactorSource: EntitiesLinkedToFactorSource,
        includeNoIssuesStatus: Boolean,
        checkIntegrityOnlyIfAnyEntitiesLinked: Boolean
    ): List<FactorSourceStatusMessage> {
        val isLinkedToAnyEntity = with(entitiesLinkedToDeviceFactorSource) {
            accounts.isNotEmpty() ||
                personas.isNotEmpty() ||
                hiddenAccounts.isNotEmpty() ||
                hiddenPersonas.isNotEmpty()
        }

        val shouldCheckIntegrity = !checkIntegrityOnlyIfAnyEntitiesLinked || isLinkedToAnyEntity

        return if (shouldCheckIntegrity) {
            listOfNotNull(
                (entitiesLinkedToDeviceFactorSource.integrity as FactorSourceIntegrity.Device)
                    .toMessage(includeNoIssuesStatus)
            )
        } else {
            emptyList()
        }
    }

    suspend fun forFactorSource(
        factorSource: FactorSource,
        includeNoIssuesStatus: Boolean,
        checkIntegrityOnlyIfAnyEntitiesLinked: Boolean
    ): List<FactorSourceStatusMessage> = withContext(defaultDispatcher) {
        (factorSource as? FactorSource.Device)?.let { deviceFactorSource ->
            forDeviceFactorSources(
                deviceFactorSources = listOf(deviceFactorSource),
                includeNoIssuesMessage = includeNoIssuesStatus,
                checkIntegrityOnlyIfAnyEntitiesLinked = checkIntegrityOnlyIfAnyEntitiesLinked
            )[deviceFactorSource.id]
        }.orEmpty()
    }

    private fun FactorSourceIntegrity.Device.toMessage(includeNoIssuesStatus: Boolean): FactorSourceStatusMessage? =
        when {
            this.v1.isMnemonicPresentInSecureStorage.not() -> FactorSourceStatusMessage.SecurityPrompt.LostFactorSource
            this.v1.isMnemonicMarkedAsBackedUp.not() -> FactorSourceStatusMessage.SecurityPrompt.WriteDownSeedPhrase
            includeNoIssuesStatus -> FactorSourceStatusMessage.NoSecurityIssues
            else -> null
        }
}
