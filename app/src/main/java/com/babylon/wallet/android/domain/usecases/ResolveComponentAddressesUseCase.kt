package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.radixdlt.sargon.ManifestEncounteredComponentAddress
import com.radixdlt.sargon.extensions.string
import rdx.works.core.domain.DApp
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
    suspend operator fun invoke(
        componentAddresses: List<ManifestEncounteredComponentAddress>
    ): Result<LinkedHashMap<ManifestEncounteredComponentAddress, DApp?>> = stateRepository.getDAppDefinitions(
        componentAddresses = componentAddresses
    ).mapCatching { dAppDefinitionPerComponent ->
        val distinctDAppDefinitionAddresses = dAppDefinitionPerComponent.values.filterNotNull().toSet()

        val dAppsPerDAppDefinition = if (distinctDAppDefinitionAddresses.isNotEmpty()) {
            stateRepository.getDAppsDetails(
                definitionAddresses = distinctDAppDefinitionAddresses.toList(),
                isRefreshing = true
            ).mapCatching { dApps ->
                dApps.associateBy { it.dAppAddress }
            }.getOrThrow()
        } else {
            emptyMap()
        }

        val dAppsPerComponentAddress = LinkedHashMap<ManifestEncounteredComponentAddress, DApp?>()

        componentAddresses.forEach { componentAddress ->
            val associatedDApp = dAppDefinitionPerComponent[componentAddress]?.let { dAppDefinition ->
                dAppsPerDAppDefinition[dAppDefinition]?.takeIf {
                    it.claimedEntities.contains(componentAddress.string)
                }
            }

            dAppsPerComponentAddress[componentAddress] = associatedDApp
        }

        dAppsPerComponentAddress
    }
}
