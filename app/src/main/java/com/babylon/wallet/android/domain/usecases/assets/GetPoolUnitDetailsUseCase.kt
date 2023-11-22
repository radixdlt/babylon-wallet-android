package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.domain.model.assets.PoolUnit
import com.babylon.wallet.android.domain.model.resources.Resource
import rdx.works.core.then
import javax.inject.Inject

/**
 * Returns details regarding this NFT's local id included in a collection's resource address.
 */
class GetPoolUnitDetailsUseCase @Inject constructor(
    private val stateRepository: StateRepository
) {

    suspend operator fun invoke(resourceAddress: String, accountAddress: String): Result<PoolUnit> = stateRepository.getResources(
        addresses = setOf(resourceAddress),
        underAccountAddress = accountAddress,
        withDetails = true
    ).mapCatching {
        it.first() as Resource.FungibleResource
    }.then { poolResource ->
        val poolAddress = poolResource.poolAddress ?: return@then runCatching {
            error("Resource $resourceAddress is not associated with a pool")
        }
        stateRepository.getPool(poolAddress).map {
            PoolUnit(
                stake = poolResource,
                pool = it
            )
        }
    }
}
