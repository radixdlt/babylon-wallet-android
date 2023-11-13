package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.state.StateRepository
import javax.inject.Inject

class GetResourcesUseCase @Inject constructor(
    private val stateRepository: StateRepository
) {

    suspend operator fun invoke(addresses: Set<String>, withDetails: Boolean = false) =
        stateRepository.getResources(addresses = addresses, underAccountAddress = null, withDetails = withDetails)
}
