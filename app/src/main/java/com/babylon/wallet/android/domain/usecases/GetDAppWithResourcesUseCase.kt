package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.domain.model.DAppWithResources
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.metadata.dAppDefinition
import rdx.works.core.domain.resources.metadata.dAppDefinitions
import javax.inject.Inject

class GetDAppWithResourcesUseCase @Inject constructor(
    private val stateRepository: StateRepository
) {

    suspend operator fun invoke(
        definitionAddress: AccountAddress,
        needMostRecentData: Boolean
    ): Result<DAppWithResources> = stateRepository.getDAppsDetails(
        definitionAddresses = listOf(definitionAddress),
        isRefreshing = needMostRecentData
    ).mapCatching { dApps ->
        val dApp = dApps.first()
        val claimedResources = dApp.claimedEntities.mapNotNull {
            runCatching { ResourceAddress.init(it) }.getOrNull()
        }
        val resources = stateRepository.getResources(
            addresses = claimedResources.toSet(),
            underAccountAddress = null,
            withDetails = false,
            withAllMetadata = false
        ).getOrNull().orEmpty()
        val verifiedResources = resources.filter {
            it.metadata.dAppDefinitions().contains(definitionAddress.string) ||
                it.metadata.dAppDefinition() == definitionAddress.string
        }

        DAppWithResources(
            dApp = dApp,
            fungibleResources = verifiedResources.filterIsInstance<Resource.FungibleResource>(),
            nonFungibleResources = verifiedResources.filterIsInstance<Resource.NonFungibleResource>()
        )
    }
}
