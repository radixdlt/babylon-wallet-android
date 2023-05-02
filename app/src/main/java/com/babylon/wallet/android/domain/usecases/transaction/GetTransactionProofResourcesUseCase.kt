package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.repository.dappmetadata.DappMetadataRepository
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.presentation.transaction.PresentingProofUiModel
import javax.inject.Inject

class GetTransactionProofResourcesUseCase @Inject constructor(
    private val dappMetadataRepository: DappMetadataRepository
) {

    suspend operator fun invoke(
        accountProofResources: Array<String>
    ): List<PresentingProofUiModel> {
        if (accountProofResources.isEmpty()) return emptyList()

        val proofs = mutableListOf<PresentingProofUiModel>()

        val metadataResults = dappMetadataRepository.getDappsMetadata(
            defitnionAddresses = accountProofResources.toList(),
            needMostRecentData = false
        )
        metadataResults.onValue { dAppsMetadata ->
            dAppsMetadata.forEach { dAppMetadata ->
                proofs.add(
                    PresentingProofUiModel(
                        iconUrl = dAppMetadata.iconUrl?.toString().orEmpty(),
                        title = dAppMetadata.name.orEmpty()
                    )
                )
            }
        }
        return proofs
    }
}
