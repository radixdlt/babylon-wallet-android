package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.dappmetadata.DAppRepository
import com.babylon.wallet.android.domain.model.DAppWithMetadataAndAssociatedResources
import com.babylon.wallet.android.domain.model.metadata.ClaimedWebsitesMetadataItem
import javax.inject.Inject

class GetDAppWithMetadataAndAssociatedResourcesUseCase @Inject constructor(
    private val dAppRepository: DAppRepository
) {

    suspend operator fun invoke(
        definitionAddress: String,
        needMostRecentData: Boolean,
        claimedEntityValidation: ClaimedEntityValidation = ClaimedEntityValidation.None
    ): Result<DAppWithMetadataAndAssociatedResources> = dAppRepository.getDAppMetadata(
        definitionAddress = definitionAddress,
        needMostRecentData = false
    ).mapCatching { dAppMetadata ->
        // If well known file verification fails, we dont want claimed websites
        val websites = dAppMetadata.claimedWebsites.map {
            val validDapp = dAppRepository.verifyDapp(
                origin = it,
                dAppDefinitionAddress = dAppMetadata.dAppAddress,
                wellKnownFileCheck = false
            ).getOrThrow()

            it to validDapp
        }.filterNot { !it.second }

        val updatedDAppMetadata = dAppMetadata.copy(
            claimedWebsitesItem = ClaimedWebsitesMetadataItem(websites = websites.map { it.first })
        )
        val verified = when (claimedEntityValidation) {
            is ClaimedEntityValidation.PerformFor -> {
                dAppMetadata.claimedEntities.contains(claimedEntityValidation.entityAddress)
            }
            ClaimedEntityValidation.None -> true
        }

        dAppRepository.getDAppResources(dAppMetadata = updatedDAppMetadata, needMostRecentData)
            .map { resources ->
                DAppWithMetadataAndAssociatedResources(
                    dAppWithMetadata = updatedDAppMetadata,
                    resources = resources,
                    verified = verified
                )
            }.getOrThrow()
    }
}

sealed interface ClaimedEntityValidation {
    data object None : ClaimedEntityValidation
    data class PerformFor(val entityAddress: String) : ClaimedEntityValidation
}
