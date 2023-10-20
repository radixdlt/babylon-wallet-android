package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.entity.EntityRepository
import com.babylon.wallet.android.domain.model.resources.Resource
import javax.inject.Inject

class GetResourcesUseCase @Inject constructor(
    private val entityRepository: EntityRepository
) {

    suspend operator fun invoke(addresses: List<String>): List<Resource> {
        return entityRepository.getResources(addresses).getOrNull().orEmpty()
    }
}
