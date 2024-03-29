package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import rdx.works.core.domain.resources.Badge
import javax.inject.Inject

class GetTransactionBadgesUseCase @Inject constructor(
    private val stateRepository: StateRepository
) {

    suspend operator fun invoke(
        addresses: Set<ResourceAddress>
    ): Result<List<Badge>> = stateRepository.getResources(
        addresses = addresses.map { it.string }.toSet(),
        underAccountAddress = null,
        withDetails = false
    ).mapCatching { resources ->
        resources.map {
            Badge(
                address = ResourceAddress.init(it.resourceAddress),
                metadata = it.metadata
            )
        }
    }
}
