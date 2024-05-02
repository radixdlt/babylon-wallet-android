package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.radixdlt.sargon.Account
import rdx.works.core.domain.resources.Resource
import javax.inject.Inject

class GetNextNFTsPageUseCase @Inject constructor(
    private val repository: StateRepository
) {

    suspend operator fun invoke(account: Account, resource: Resource.NonFungibleResource) =
        repository.getNextNFTsPage(account, resource)
}
