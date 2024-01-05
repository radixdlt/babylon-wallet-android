package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.data.repository.dappmetadata.DAppRepository
import com.babylon.wallet.android.domain.model.DAppWithResources
import com.babylon.wallet.android.domain.model.resources.metadata.Metadata
import com.babylon.wallet.android.domain.model.resources.metadata.MetadataType
import com.babylon.wallet.android.presentation.model.ActionableAddress
import rdx.works.core.mapWhen
import javax.inject.Inject

class GetDAppWithMetadataAndAssociatedResourcesUseCase @Inject constructor(
    private val dAppRepository: DAppRepository
) {

    suspend operator fun invoke(
        definitionAddress: String,
        needMostRecentData: Boolean,
        claimedEntityValidation: ClaimedEntityValidation = ClaimedEntityValidation.None
    ): Result<DAppWithResources> = dAppRepository.getDAppMetadata(
        definitionAddress = definitionAddress,
        needMostRecentData = false
    ).mapCatching { dAppMetadata ->
        // If well known file verification fails, we dont want claimed websites
        val validWebsites = dAppMetadata.claimedWebsites.filter {
            dAppRepository.verifyDapp(
                origin = it,
                dAppDefinitionAddress = dAppMetadata.dAppAddress,
                wellKnownFileCheck = false
            ).getOrThrow()
        }

        val updatedDAppMetadata = dAppMetadata.copy(
            metadata = dAppMetadata.metadata.mapWhen(
                predicate = { it.key == ExplicitMetadataKey.CLAIMED_WEBSITES.key },
                mutation = {
                    Metadata.Collection(
                        key = ExplicitMetadataKey.CLAIMED_WEBSITES.key,
                        values = validWebsites.map { website ->
                            Metadata.Primitive(
                                key = ExplicitMetadataKey.CLAIMED_WEBSITES.key,
                                value = website,
                                valueType = MetadataType.Url
                            )
                        }
                    )
                }
            )
        )
        val verified = when (claimedEntityValidation) {
            is ClaimedEntityValidation.PerformFor -> {
                dAppMetadata.claimedEntities.contains(claimedEntityValidation.entityAddress)
            }

            ClaimedEntityValidation.None -> true
        }

        val componentAddresses = dAppMetadata.claimedEntities.filter {
            ActionableAddress.Type.from(it) == ActionableAddress.Type.Global.COMPONENT
        }

        dAppRepository.getDAppResources(dAppMetadata = updatedDAppMetadata, needMostRecentData)
            .map { resources ->
                DAppWithResources(
                    dApp = updatedDAppMetadata,
                    resources = resources,
                    verified = verified,
                    componentAddresses = if (verified) emptyList() else componentAddresses
                )
            }.getOrThrow()
    }
}

sealed interface ClaimedEntityValidation {
    data object None : ClaimedEntityValidation
    data class PerformFor(val entityAddress: String) : ClaimedEntityValidation
}
