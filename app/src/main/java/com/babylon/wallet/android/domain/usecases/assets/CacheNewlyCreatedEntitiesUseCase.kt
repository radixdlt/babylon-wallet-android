package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.repository.state.StateRepository
import rdx.works.core.domain.resources.Resource
import javax.inject.Inject

class CacheNewlyCreatedEntitiesUseCase @Inject constructor(
    private val stateRepository: StateRepository
) {

    suspend fun forResources(resources: List<Resource>): Result<Unit> {
        return stateRepository.cacheNewlyCreatedResources(resources)
    }

    suspend fun forNFTs(newNfts: List<Resource.NonFungibleResource.Item>): Result<Unit> {
        return stateRepository.cacheNewlyCreatedNFTItems(newNfts)
    }
}
