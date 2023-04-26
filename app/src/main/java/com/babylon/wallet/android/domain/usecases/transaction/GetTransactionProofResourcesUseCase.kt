package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.repository.dappmetadata.DappMetadataRepository
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.presentation.transaction.PresentingProofUiModel
import com.radixdlt.toolkit.models.address.EntityAddress
import javax.inject.Inject

class GetTransactionProofResourcesUseCase @Inject constructor(
    private val dappMetadataRepository: DappMetadataRepository
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
        val metadataResults = dappMetadataRepository.getDappsMetadata(
            defitnionAddresses = proofAddresses,
            needMostRecentData = false
        )
        metadataResults.onValue { dAppsMetadata ->
            dAppsMetadata.forEach { dAppMetadata ->
                proofs.add(
                    PresentingProofUiModel(
                        iconUrl = dAppMetadata.getImageUrl().orEmpty(),
                        title = dAppMetadata.getName().orEmpty()
                    )
                )
            }
        }
        return proofs
    }
}
