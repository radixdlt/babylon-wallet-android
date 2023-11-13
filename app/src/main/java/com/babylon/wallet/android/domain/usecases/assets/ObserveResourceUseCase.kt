package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.domain.model.resources.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Fetches details regarding this fungible/non-fungible by looking it up through its address.
 * If an account address is provided, the repository will try to also fetch amount information.
 */
class ObserveResourceUseCase @Inject constructor(
    private val stateRepository: StateRepository
) {

    operator fun invoke(resourceAddress: String, accountAddress: String?): Flow<Resource> = flow {
        val resource = stateRepository.getResources(
            addresses = setOf(resourceAddress),
            underAccountAddress = accountAddress,
            withDetails = false
        ).getOrThrow().first()

        emit(resource)

        if (!resource.isDetailsAvailable) {
            val resourceWithDetails = stateRepository.getResources(
                addresses = setOf(resourceAddress),
                underAccountAddress = accountAddress,
                withDetails = true
            ).getOrThrow().first()

            emit(resourceWithDetails)
        }
    }
}
