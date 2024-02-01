package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.domain.model.DApp
import rdx.works.core.then
import javax.inject.Inject

class ResolveComponentAddressesUseCase @Inject constructor(
    private val stateRepository: StateRepository
) {

    /**
     * Each component is validated as follows:
     * - get dAppDefinitionAddress from component metadata
     * - get metadata for that address
     * - validate that account_type is "dapp definition"
     * - check if componentAddress is within claimed_entities metadata of dAppDefinitionAddress metadata
     */
    suspend fun invoke(
        componentAddress: String
    ): Result<Pair<String, DApp?>> = stateRepository.getDAppsDetails(
        definitionAddresses = listOf(componentAddress),
        isRefreshing = true
    ).then { components ->
        val dAppDefinitionAddress = components.firstOrNull()?.definitionAddress
        if (dAppDefinitionAddress != null) {
            stateRepository.getDAppsDetails(
                definitionAddresses = listOf(dAppDefinitionAddress),
                isRefreshing = true
            ).mapCatching { dApps ->
                val dApp = dApps.first()
                componentAddress to dApp
            }
        } else {
            Result.success(componentAddress to null)
        }
    }
}
