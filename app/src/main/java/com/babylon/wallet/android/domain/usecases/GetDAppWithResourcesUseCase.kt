package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.domain.model.DAppWithResources
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.extensions.init
import rdx.works.core.domain.resources.Resource
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

        DAppWithResources(
            dApp = dApp,
            fungibleResources = resources.filterIsInstance<Resource.FungibleResource>(),
            nonFungibleResources = resources.filterIsInstance<Resource.NonFungibleResource>()
        )
    }
}
