package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.dappmetadata.DappMetadataRepository
import com.babylon.wallet.android.data.repository.entity.EntityRepository
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.common.map
import com.babylon.wallet.android.domain.common.switchMap
import com.babylon.wallet.android.domain.common.value
import com.babylon.wallet.android.domain.model.DAppWithMetadataAndAssociatedResources
import com.babylon.wallet.android.domain.model.metadata.ClaimedWebsiteMetadataItem
import javax.inject.Inject

class GetDAppWithMetadataAndAssociatedResourcesUseCase @Inject constructor(
    private val entityRepository: EntityRepository,
    private val dAppMetadataRepository: DappMetadataRepository,
) {
    suspend operator fun invoke(
        definitionAddress: String,
        needMostRecentData: Boolean
    ): Result<DAppWithMetadataAndAssociatedResources> = dAppMetadataRepository.getDAppMetadata(
        definitionAddress = definitionAddress,
        needMostRecentData = false
    ).switchMap { dAppMetadata ->
        // If well known file verification fails, we dont want claimed websites
        val isWebsiteAuthentic = dAppMetadata.claimedWebsite?.let { website ->
            dAppMetadataRepository.verifyDapp(
                origin = website,
                dAppDefinitionAddress = dAppMetadata.dAppAddress
            ).value() == true
        } ?: false

        val updatedDAppMetadata = dAppMetadata.copy(
            claimedWebsiteItem = if (isWebsiteAuthentic) {
                dAppMetadata.claimedWebsite?.let {
                    ClaimedWebsiteMetadataItem(
                        it
                    )
                }
            } else {
                null
            }
        )

        entityRepository.getDAppResources(dAppMetadata = updatedDAppMetadata, needMostRecentData)
            .map { resources ->
                DAppWithMetadataAndAssociatedResources(
                    dAppWithMetadata = updatedDAppMetadata,
                    resources = resources
                )
            }
    }
}
