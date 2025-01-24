package com.babylon.wallet.android.domain.usecases.factorsources

import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.model.SecurityProblem
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceStatusMessage
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.os.SargonOsManager
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetFactorSourceStatusMessagesUseCase @Inject constructor(
    private val getFactorSourcesOfTypeUseCase: GetFactorSourcesOfTypeUseCase,
    private val sargonOsManager: SargonOsManager,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    operator fun invoke(
        factorSourceKind: FactorSourceKind,
        securityProblems: Set<SecurityProblem>
    ): List<FactorSourceStatusMessage> {
//        val isDeviceFactorSourceLinkedToAnyEntities = listOf(
//            entitiesLinkedToDeviceFactorSource.accounts,
//            entitiesLinkedToDeviceFactorSource.personas,
//            entitiesLinkedToDeviceFactorSource.hiddenAccounts,
//            entitiesLinkedToDeviceFactorSource.hiddenPersonas
//        ).any { it.isNotEmpty() }
//
//        val backedUpFactorSourceIds = preferencesManager.getBackedUpFactorSourceIds().firstOrNull().orEmpty()
//
//        return if (isDeviceFactorSourceLinkedToAnyEntities) {
//            val deviceFactorSourceIntegrity =
//                entitiesLinkedToDeviceFactorSource.integrity as FactorSourceIntegrity.Device
//            deviceFactorSourceIntegrity.toMessages().toPersistentList()
//        } else if (backedUpFactorSourceIds.contains(deviceFactorSourceId)) { // if not linked entities we can't check
//            // the integrity, but we can check if the user backed up the seed phrase
//            persistentListOf(FactorSourceStatusMessage.NoSecurityIssues)
//        } else {
//            // otherwise we don't show any warnings
//            persistentListOf()
//        }

        return if (factorSourceKind == FactorSourceKind.DEVICE) {
            securityProblems.mapNotNull { problem ->
                when (problem) {
                    is SecurityProblem.EntitiesNotRecoverable -> FactorSourceStatusMessage.SecurityPrompt.EntitiesNotRecoverable
                    is SecurityProblem.SeedPhraseNeedRecovery -> FactorSourceStatusMessage.SecurityPrompt.SeedPhraseNeedRecovery
                    else -> null
                }
            }.toPersistentList()
        } else {
            persistentListOf()
        }
    }
}
