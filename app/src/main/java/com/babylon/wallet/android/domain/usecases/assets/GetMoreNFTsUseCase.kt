package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.model.resources.Resource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import rdx.works.profile.data.model.pernetwork.Network
import javax.inject.Inject

class GetMoreNFTsUseCase @Inject constructor(
    private val repository: StateRepository,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(account: Network.Account, resource: Resource.NonFungibleResource) = withContext(dispatcher) {
        repository.getMoreNFTs(account, resource)
    }

}
