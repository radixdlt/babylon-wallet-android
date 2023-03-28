package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.gateway.extensions.asMetadataStringMap
import com.babylon.wallet.android.data.repository.entity.EntityRepository
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.presentation.transaction.PresentingProofUiModel
import com.radixdlt.toolkit.models.address.EntityAddress
import javax.inject.Inject

class GetTransactionProofResourcesUseCase @Inject constructor(
    private val entityRepository: EntityRepository
) {

    suspend operator fun invoke(
        accountProofResources: Array<EntityAddress>
    ): List<PresentingProofUiModel> {
        if (accountProofResources.isEmpty()) return emptyList()

        val proofs = mutableListOf<PresentingProofUiModel>()

        val proofAddresses = mutableListOf<String>()
        accountProofResources.forEach { proofAddress ->
            when (proofAddress) {
                is EntityAddress.ResourceAddress -> {
                    proofAddresses.add(proofAddress.address)
                }
                is EntityAddress.ComponentAddress -> {
                    proofAddresses.add(proofAddress.address)
                }
                is EntityAddress.PackageAddress -> {
                    proofAddresses.add(proofAddress.address)
                }
            }
        }
        val proofResults = entityRepository.stateEntityDetails(
            addresses = proofAddresses
        )
        proofResults.onValue { proofDetailsResponse ->
            proofDetailsResponse.items.forEach { proofItem ->
                var name = ""
                var iconUrl = ""
                if (proofItem.metadata.asMetadataStringMap().containsKey("name")) {
                    name = proofItem.metadata.asMetadataStringMap().getValue("name")
                }
                if (proofItem.metadata.asMetadataStringMap().containsKey("url")) {
                    iconUrl = proofItem.metadata.asMetadataStringMap().getValue("url")
                }

                proofs.add(
                    PresentingProofUiModel(
                        iconUrl = iconUrl,
                        title = name
                    )
                )
            }
        }
        return proofs
    }
}
