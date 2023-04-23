package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.repository.dappmetadata.DappMetadataRepository
import com.babylon.wallet.android.domain.common.map
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.presentation.transaction.ConnectedDAppsUiModel
import com.radixdlt.toolkit.models.address.EntityAddress
import javax.inject.Inject

class GetValidDAppMetadataUseCase @Inject constructor(
    private val dappMetadataRepository: DappMetadataRepository
) {

    suspend operator fun invoke(
        componentAddresses: List<EntityAddress.ComponentAddress>
    ): List<ConnectedDAppsUiModel> {
        if (componentAddresses.isEmpty()) return emptyList()

        val encounteredAddresses = mutableListOf<ConnectedDAppsUiModel>()

        dappMetadataRepository.getDappsMetadata(
            defitnionAddresses = componentAddresses.map { it.address },
            needMostRecentData = false
        ).map { metadataList ->
            val dAppDefinitionAddresses = metadataList.map { metadata ->
                metadata.getDappDefinition()
            }

            val validEncounteredAddresses = dAppDefinitionAddresses.filter { it.isNotEmpty() }
            val emptyEncounteredAddresses = dAppDefinitionAddresses.filter { it.isEmpty() }

            if (validEncounteredAddresses.isNotEmpty()) {
                val dAppsResults =
                    dappMetadataRepository.getDappsMetadata(
                        defitnionAddresses = validEncounteredAddresses,
                        needMostRecentData = false
                    )
                dAppsResults.onValue { dAppsMetadata ->
                    dAppsMetadata.forEach { metadata ->
                        encounteredAddresses.add(
                            ConnectedDAppsUiModel(
                                iconUrl = metadata.getImageUrl().orEmpty(),
                                title = metadata.getName().orEmpty()
                            )
                        )
                    }
                }
            }

            emptyEncounteredAddresses.forEach { _ ->
                encounteredAddresses.add(
                    ConnectedDAppsUiModel(
                        iconUrl = "",
                        title = ""
                    )
                )
            }
        }
        return encounteredAddresses
    }
}
