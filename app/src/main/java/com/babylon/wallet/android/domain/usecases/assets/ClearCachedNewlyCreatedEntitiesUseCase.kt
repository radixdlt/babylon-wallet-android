package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.repository.state.StateRepository
import rdx.works.core.domain.resources.Resource
import javax.inject.Inject

class ClearCachedNewlyCreatedEntitiesUseCase @Inject constructor(
    private val stateRepository: StateRepository
) {

    suspend operator fun invoke(nfts: List<Resource.NonFungibleResource.Item>) {
        stateRepository.clearCachedNewlyCreatedNFTItems(nfts)
    }
}
