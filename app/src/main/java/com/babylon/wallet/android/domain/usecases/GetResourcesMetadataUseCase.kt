package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.entity.EntityRepository
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.model.metadata.MetadataItem
import javax.inject.Inject

class GetResourcesMetadataUseCase @Inject constructor(
    private val entityRepository: EntityRepository
) {
    suspend fun invoke(resourceAddresses: List<String>, isRefreshing: Boolean): Result<Map<String, List<MetadataItem>>> {
        return entityRepository.getResourcesMetadata(
            resourceAddresses = resourceAddresses,
            isRefreshing = isRefreshing
        )
    }
}
