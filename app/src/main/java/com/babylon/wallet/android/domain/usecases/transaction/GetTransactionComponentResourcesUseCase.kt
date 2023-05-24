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
import com.radixdlt.toolkit.models.request.MetadataEntry
import com.radixdlt.toolkit.models.request.MetadataKeyValue
import com.radixdlt.toolkit.models.request.MetadataValue
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork
import javax.inject.Inject

class GetTransactionComponentResourcesUseCase @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val entityRepository: EntityRepository
) {
    @Suppress("LongMethod", "LongParameterList", "CyclomaticComplexMethod")
    suspend fun invoke(
        componentAddress: String,
        resourceRequest: ResourceRequest,
        amount: String,
        instructionIndex: Int? = null,
        includesGuarantees: Boolean,
        index: Int? = null
    ): Result<TransactionAccountItemUiModel> {
        // do not ask gateway for it, lets skip this address
        val createdEntity = resourceRequest is ResourceRequest.NewlyCreated
        val resourceAddress = (resourceRequest as? ResourceRequest.Existing)?.address.orEmpty()
        val validatedAddresses = if (createdEntity) {
            listOf(
                componentAddress
            )
        } else {
            listOf(
                componentAddress,
                (resourceRequest as ResourceRequest.Existing).address
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

            when (resourceRequest) {
                is ResourceRequest.Existing -> {
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
                is ResourceRequest.NewlyCreated -> {
                    tokenSymbol = when (val entry = resourceRequest.metadata.firstOrNull { it.key == KEY_SYMBOL }?.value) {
                        is MetadataEntry.Value -> when (val value = entry.value) {
                            is MetadataValue.String -> value.value
                            else -> "Unknown"
                        }
                        else -> "Unknown"
                    }
                    iconUrl = when (val entry = resourceRequest.metadata.firstOrNull { it.key == KEY_SYMBOL }?.value) {
                        is MetadataEntry.Value -> when (val value = entry.value) {
                            is MetadataValue.Url -> value.value
                            is MetadataValue.String -> value.value
                            else -> ""
                        }
                        else -> ""
                    }
                }
            }

            val accountOnProfile = getProfileUseCase
                .accountOnCurrentNetwork(accountItem?.address.orEmpty())
            val accountDisplayName = accountOnProfile?.displayName.orEmpty()
            val accountAppearanceId = accountOnProfile?.appearanceID ?: 1

            // If its newlyCreatedEntity do not ask for guarantees
            val shouldPromptForGuarantees = if (createdEntity) false else includesGuarantees

            // If its newlyCreatedEntity OR don't include guarantee, do not ask for guarantees
            val guaranteedQuantity = if (createdEntity || !includesGuarantees) null else amount

            account = Result.Success(
                TransactionAccountItemUiModel(
                    address = accountItem?.address.orEmpty(),
                    displayName = accountDisplayName,
                    tokenSymbol = tokenSymbol,
                    tokenQuantity = amount,
                    appearanceID = accountAppearanceId,
                    iconUrl = iconUrl,
                    isTokenAmountVisible = isTokenAmountVisible,
                    shouldPromptForGuarantees = shouldPromptForGuarantees,
                    guaranteedQuantity = guaranteedQuantity,
                    instructionIndex = instructionIndex,
                    resourceAddress = resourceAddress,
                    index = index
                )
            )
        }
        return account
    }
}

sealed interface ResourceRequest {
    data class Existing(val address: String) : ResourceRequest
    data class NewlyCreated(val metadata: Array<MetadataKeyValue>) : ResourceRequest
}
