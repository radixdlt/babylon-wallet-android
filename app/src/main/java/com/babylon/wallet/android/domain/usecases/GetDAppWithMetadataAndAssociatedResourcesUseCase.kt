package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.dappmetadata.DAppRepository
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.common.map
import com.babylon.wallet.android.domain.common.switchMap
import com.babylon.wallet.android.domain.common.value
import com.babylon.wallet.android.domain.model.DAppWithMetadataAndAssociatedResources
import com.babylon.wallet.android.domain.model.metadata.ClaimedWebsitesMetadataItem
import javax.inject.Inject

class GetDAppWithMetadataAndAssociatedResourcesUseCase @Inject constructor(
    private val dAppRepository: DAppRepository
) {

    suspend operator fun invoke(
        definitionAddress: String,
        needMostRecentData: Boolean
    ): Result<DAppWithMetadataAndAssociatedResources> = dAppRepository.getDAppMetadata(
        definitionAddress = definitionAddress,
        needMostRecentData = false
    ).switchMap { dAppMetadata ->
        // If well known file verification fails, we dont want claimed websites
        val websites = dAppMetadata.claimedWebsites.map {
            val isWebsiteAuthentic = dAppRepository.verifyDapp(
                origin = it,
                dAppDefinitionAddress = dAppMetadata.dAppAddress
            ).value() == true

            it to isWebsiteAuthentic
        }.filterNot { !it.second }

        val updatedDAppMetadata = dAppMetadata.copy(
            claimedWebsitesItem = ClaimedWebsitesMetadataItem(websites = websites.map { it.first })
        )

        dAppRepository.getDAppResources(dAppMetadata = updatedDAppMetadata, needMostRecentData)
            .map { resources ->
                DAppWithMetadataAndAssociatedResources(
                    dAppWithMetadata = updatedDAppMetadata,
                    resources = resources
                )
            }
    }
}
