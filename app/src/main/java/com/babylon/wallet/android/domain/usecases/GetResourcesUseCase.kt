package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.radixdlt.sargon.ResourceAddress
import javax.inject.Inject

class GetResourcesUseCase @Inject constructor(
    private val stateRepository: StateRepository
) {

    suspend operator fun invoke(
        addresses: Set<ResourceAddress>,
        withDetails: Boolean = false
    ) = stateRepository.getResources(
        addresses = addresses,
        underAccountAddress = null,
        withDetails = withDetails,
        withAllMetadata = false
    )
}
