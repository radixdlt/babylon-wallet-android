package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.DApp
import rdx.works.core.then
import javax.inject.Inject

class ResolveDAppInTransactionUseCase @Inject constructor(
    private val stateRepository: StateRepository
) {

    /**
     * Each component is validated as follows:
     * - get dAppDefinitionAddress from component metadata
     * - get metadata for that address
     * - validate that account_type is "dapp definition"
     * - check if componentAddress is within claimed_entities metadata of dAppDefinitionAddress metadata
     */
    suspend operator fun invoke(
        componentAddress: String
    ): Result<Pair<DApp, Boolean>> = stateRepository.getDAppsDetails(
        definitionAddresses = listOf(componentAddress),
        skipCache = true
    ).then { components ->
        val dAppDefinitionAddress = components.firstOrNull()?.definitionAddresses?.firstOrNull()
        if (dAppDefinitionAddress != null) {
            stateRepository.getDAppsDetails(
                definitionAddresses = listOf(dAppDefinitionAddress),
                skipCache = true
            ).mapCatching { dApps ->
                val dApp = dApps.first()
                dApp to dApp.claimedEntities.contains(componentAddress)
            }
        } else {
            Result.failure(RadixWalletException.DappVerificationException.WrongAccountType)
        }
    }
}
