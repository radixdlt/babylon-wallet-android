package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.domain.model.DAppWithResources
import com.babylon.wallet.android.domain.model.resources.Resource
import com.radixdlt.ret.Address
import javax.inject.Inject

class GetDAppWithResourcesUseCase @Inject constructor(
    private val stateRepository: StateRepository
) {

    suspend operator fun invoke(
        definitionAddress: String,
        needMostRecentData: Boolean
    ): Result<DAppWithResources> = stateRepository.getDAppsDetails(
        definitionAddresses = listOf(definitionAddress),
        skipCache = needMostRecentData
    ).mapCatching { dApps ->
        val dApp = dApps.first()
        val claimedResources = dApp.claimedEntities.filter { Address(it).isGlobalResourceManager() }
        val resources = stateRepository.getResources(
            addresses = claimedResources.toSet(),
            underAccountAddress = null,
            withDetails = false
        ).getOrNull().orEmpty()

        DAppWithResources(
            dApp = dApp,
            fungibleResources = resources.filterIsInstance<Resource.FungibleResource>(),
            nonFungibleResources = resources.filterIsInstance<Resource.NonFungibleResource>()
        )
    }
}
