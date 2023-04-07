package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.gateway.extensions.asMetadataStringMap
import com.babylon.wallet.android.data.repository.entity.EntityRepository
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.model.MetadataConstants
import com.babylon.wallet.android.presentation.transaction.EncounteredAddressesUiModel
import com.radixdlt.toolkit.models.address.EntityAddress
import javax.inject.Inject

class GetTransactionEncounteredAddressesUseCase @Inject constructor(
    private val entityRepository: EntityRepository
) {

    suspend operator fun invoke(
        componentAddresses: List<EntityAddress.ComponentAddress>
    ): List<EncounteredAddressesUiModel> {
        if (componentAddresses.isEmpty()) return emptyList()

        val encounteredAddresses = mutableListOf<EncounteredAddressesUiModel>()

        val encounteredDAppsResults = entityRepository.stateEntityDetails(
            addresses = componentAddresses.map { it.address },
            isRefreshing = false
        )
        encounteredDAppsResults.onValue { encounteredDetailsResponse ->
            val dAppDefinitionAddresses = encounteredDetailsResponse.items.map { encounteredDetailsResponseItem ->
                val metadataMap = encounteredDetailsResponseItem.metadata.asMetadataStringMap()
                metadataMap.getOrDefault(D_APP_DEFINITION_KEY, "")
            }

            val validEncounteredAddresses = dAppDefinitionAddresses.filter { it.isNotEmpty() }
            val emptyEncounteredAddresses = dAppDefinitionAddresses.filter { it.isEmpty() }

            if (validEncounteredAddresses.isNotEmpty()) {
                val dAppsResults = entityRepository.stateEntityDetails(
                    addresses = validEncounteredAddresses,
                    isRefreshing = false
                )
                dAppsResults.onValue { dAppResponse ->
                    dAppResponse.items.forEach { dAppResponseItem ->
                        val metadataMap = dAppResponseItem.metadata.asMetadataStringMap()
                        var iconUrl = ""
                        var name = ""
                        if (metadataMap.containsKey(MetadataConstants.KEY_IMAGE_URL)) {
                            iconUrl = metadataMap.getValue(MetadataConstants.KEY_IMAGE_URL)
                        }
                        if (metadataMap.containsKey(MetadataConstants.KEY_NAME)) {
                            name = metadataMap.getValue(MetadataConstants.KEY_NAME)
                        }

                        encounteredAddresses.add(
                            EncounteredAddressesUiModel(
                                iconUrl = iconUrl,
                                title = name
                            )
                        )
                    }
                }
            }

            emptyEncounteredAddresses.forEach { _ ->
                encounteredAddresses.add(
                    EncounteredAddressesUiModel(
                        iconUrl = "",
                        title = ""
                    )
                )
            }
        }
        return encounteredAddresses
    }

    companion object {
        private const val D_APP_DEFINITION_KEY = "dapp_definition"
    }
}
