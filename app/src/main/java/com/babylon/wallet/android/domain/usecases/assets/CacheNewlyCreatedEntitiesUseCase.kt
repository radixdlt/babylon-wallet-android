package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.domain.model.resources.Resource
import javax.inject.Inject

class CacheNewlyCreatedEntitiesUseCase @Inject constructor(
    private val stateRepository: StateRepository
) {

    suspend operator fun invoke(resources: List<Resource>): Result<Unit> {
        return stateRepository.cacheNewlyCreatedResources(resources)
    }
}
