package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.gateway.extensions.asMetadataStringMap
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItemDetailsType
import com.babylon.wallet.android.data.repository.entity.EntityRepository
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.model.MetadataConstants.KEY_ICON
import com.babylon.wallet.android.domain.model.MetadataConstants.KEY_NAME
import com.babylon.wallet.android.domain.model.MetadataConstants.KEY_SYMBOL
import com.babylon.wallet.android.presentation.transaction.TransactionAccountItemUiModel
import com.radixdlt.toolkit.models.address.EntityAddress
import com.radixdlt.toolkit.models.request.CreatedEntities
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork
import javax.inject.Inject

class GetTransactionComponentResourcesUseCase @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val entityRepository: EntityRepository
) {
    @Suppress("LongMethod")
    suspend fun invoke(
        componentAddress: String,
        resourceAddress: String,
        createdEntities: CreatedEntities,
        amount: String
    ): Result<TransactionAccountItemUiModel> {
        val createdEntitiesAddresses = createdEntities
            .resourceAddresses.filterIsInstance<EntityAddress.ResourceAddress>()
            .map { resAddress ->
                resAddress.address
            }

        // do not ask gateway for it, lets skip this address
        val createdEntity = createdEntitiesAddresses.contains(resourceAddress)

        val validatedAddresses = if (createdEntity) {
            listOf(
                componentAddress
            )
        } else {
            listOf(
                componentAddress,
                resourceAddress
            )
        }

        val entityDetailsResponse = entityRepository.stateEntityDetails(
            addresses = validatedAddresses,
            isRefreshing = false
        )
        var account: Result<TransactionAccountItemUiModel> = Result.Error()

        entityDetailsResponse.onValue { stateEntityDetailsResponse ->
            val accountItem = stateEntityDetailsResponse.items.find {
                it.details?.type == StateEntityDetailsResponseItemDetailsType.component
            }

            var tokenSymbol = "Unknown"
            var iconUrl = ""
            var isTokenAmountVisible = true

            if (!createdEntity) {
                val fungibleResourceItem = stateEntityDetailsResponse.items.find {
                    it.details?.type == StateEntityDetailsResponseItemDetailsType.fungibleResource
                }
                val nonFungibleResourceItem = stateEntityDetailsResponse.items.find {
                    it.details?.type == StateEntityDetailsResponseItemDetailsType.nonFungibleResource
                }

                // Do not display amount if its empty AND its NFT
                val amountHidden = nonFungibleResourceItem != null && amount.isEmpty()
                isTokenAmountVisible = !amountHidden

                fungibleResourceItem?.metadata?.asMetadataStringMap()?.let {
                    if (it.containsKey(KEY_SYMBOL)) {
                        tokenSymbol = it.getValue(KEY_SYMBOL)
                    }
                    if (it.containsKey(KEY_ICON)) {
                        iconUrl = it.getValue(KEY_ICON)
                    }
                }

                nonFungibleResourceItem?.metadata?.asMetadataStringMap()?.let {
                    if (it.containsKey(KEY_NAME)) {
                        tokenSymbol = it.getValue(KEY_NAME)
                    }
                    if (it.containsKey(KEY_ICON)) {
                        iconUrl = it.getValue(KEY_ICON)
                    }
                }
            }

            val accountOnProfile = getProfileUseCase
                .accountOnCurrentNetwork(accountItem?.address.orEmpty())
            val accountDisplayName = accountOnProfile?.displayName.orEmpty()
            val accountAppearanceId = accountOnProfile?.appearanceID ?: 1

            account = Result.Success(
                TransactionAccountItemUiModel(
                    address = accountItem?.address.orEmpty(),
                    displayName = accountDisplayName,
                    tokenSymbol = tokenSymbol,
                    tokenQuantity = amount,
                    fiatAmount = "$1234.12",
                    appearanceID = accountAppearanceId,
                    iconUrl = iconUrl,
                    isTokenAmountVisible = isTokenAmountVisible
                )
            )
        }
        return account
    }
}
