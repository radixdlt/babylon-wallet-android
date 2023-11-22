package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.domain.model.resources.Resource
import rdx.works.profile.data.model.pernetwork.Network
import javax.inject.Inject

class GetNextNFTsPageUseCase @Inject constructor(
    private val repository: StateRepository
) {

    suspend operator fun invoke(account: Network.Account, resource: Resource.NonFungibleResource) =
        repository.getNextNFTsPage(account, resource)
}
