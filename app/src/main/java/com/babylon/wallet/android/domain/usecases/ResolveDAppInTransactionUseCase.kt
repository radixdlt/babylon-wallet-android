package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.DAppWithResources
import rdx.works.core.then
import javax.inject.Inject

class ResolveDAppInTransactionUseCase @Inject constructor(
    private val stateRepository: StateRepository,
    private val getDAppWithResourcesUseCase: GetDAppWithResourcesUseCase
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
    ): Result<DAppWithResources> = stateRepository.getDAppsDetails(
        definitionAddresses = listOf(componentAddress),
        skipCache = true
    ).then { components ->
        val dAppDefinitionAddress = components.firstOrNull()?.definitionAddresses?.firstOrNull()
        if (dAppDefinitionAddress != null) {
            getDAppWithResourcesUseCase(
                definitionAddress = dAppDefinitionAddress,
                needMostRecentData = true
            ).map {
                it.copy(verified = it.dApp.claimedEntities.contains(componentAddress))
            }
        } else {
            Result.failure(RadixWalletException.DappVerificationException.WrongAccountType)
        }
    }
}
