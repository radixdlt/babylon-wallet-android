package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.radixdlt.sargon.ResourceAddress
import rdx.works.core.domain.resources.Badge
import javax.inject.Inject

class GetTransactionBadgesUseCase @Inject constructor(
    private val stateRepository: StateRepository
) {

    suspend operator fun invoke(
        addresses: Set<ResourceAddress>
    ): Result<List<Badge>> = stateRepository.getResources(
        addresses = addresses,
        underAccountAddress = null,
        withDetails = false
    ).mapCatching { resources ->
        resources.map {
            Badge(
                address = it.address,
                metadata = it.metadata
            )
        }
    }
}
