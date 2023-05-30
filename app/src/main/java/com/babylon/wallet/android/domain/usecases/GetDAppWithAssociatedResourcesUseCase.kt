package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.dappmetadata.DappMetadataRepository
import com.babylon.wallet.android.data.repository.entity.EntityRepository
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.common.map
import com.babylon.wallet.android.domain.common.switchMap
import com.babylon.wallet.android.domain.model.DAppWithAssociatedResources
import javax.inject.Inject

class GetDAppWithAssociatedResourcesUseCase @Inject constructor(
    private val entityRepository: EntityRepository,
    private val dAppMetadataRepository: DappMetadataRepository,
) {
    suspend operator fun invoke(
        definitionAddress: String,
        needMostRecentData: Boolean
    ): Result<DAppWithAssociatedResources> = dAppMetadataRepository.getDAppMetadata(
        definitionAddress = definitionAddress,
        needMostRecentData = false
    ).switchMap { dAppMetadata ->
        entityRepository.getDAppResources(dAppMetadata = dAppMetadata, needMostRecentData)
            .map { resources ->
                DAppWithAssociatedResources(
                    dAppWithMetadata = dAppMetadata,
                    resources = resources
                )
            }
    }
}
