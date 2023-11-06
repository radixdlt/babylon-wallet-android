package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.domain.model.resources.Resource
import javax.inject.Inject

/**
 * Returns details regarding this NFT's local id included in a collection's resource address.
 */
class GetNFTDetailsUseCase @Inject constructor(
    private val stateRepository: StateRepository
) {

    suspend operator fun invoke(resourceAddress: String, localId: String): Result<Resource.NonFungibleResource.Item> =
        stateRepository.getNFTDetails(resourceAddress, localId)
}
