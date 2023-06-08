package com.babylon.wallet.android.domain.usecases.transaction

import androidx.core.net.toUri
import com.babylon.wallet.android.data.gateway.extensions.asMetadataStringMap
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItemDetailsType
import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.data.repository.entity.EntityRepository
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.common.value
import com.babylon.wallet.android.domain.model.MetadataConstants.KEY_ICON
import com.babylon.wallet.android.domain.model.MetadataConstants.KEY_SYMBOL
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.metadata.DescriptionMetadataItem
import com.babylon.wallet.android.domain.model.metadata.IconUrlMetadataItem
import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem
import com.babylon.wallet.android.domain.model.metadata.SymbolMetadataItem
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
        ids: List<String>,
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

            var tokenSymbol: String? = null
            var iconUrl = ""

            var fungibleResource: Resource.FungibleResource? = null
            var nonFungibleResources: List<Resource.NonFungibleResource.Item> = emptyList()

            when (resourceRequest) {
                is ResourceRequest.Existing -> {
                    val fungibleResourceItem = stateEntityDetailsResponse.items.find {
                        it.details?.type == StateEntityDetailsResponseItemDetailsType.fungibleResource
                    }
                    val nonFungibleResourceItem = stateEntityDetailsResponse.items.find {
                        it.details?.type == StateEntityDetailsResponseItemDetailsType.nonFungibleResource
                    }

                    fungibleResource = fungibleResourceItem?.metadata?.asMetadataStringMap()?.let { metadataMap ->
                        Resource.FungibleResource(
                            resourceAddress = fungibleResourceItem.address,
                            amount = amount.toBigDecimal(),
                            nameMetadataItem = metadataMap[ExplicitMetadataKey.NAME.key]?.let { NameMetadataItem(it) },
                            symbolMetadataItem = metadataMap[ExplicitMetadataKey.SYMBOL.key]?.let {
                                SymbolMetadataItem(
                                    it
                                )
                            },
                            descriptionMetadataItem = metadataMap[ExplicitMetadataKey.DESCRIPTION.key]?.let {
                                DescriptionMetadataItem(
                                    it
                                )
                            },
                            iconUrlMetadataItem = metadataMap[ExplicitMetadataKey.ICON_URL.key]?.let {
                                IconUrlMetadataItem(
                                    it.toUri()
                                )
                            }
                        )
                    }

                    nonFungibleResources = nonFungibleResourceItem?.let { nftItem ->
                        entityRepository.nonFungibleData(
                            ids = ids,
                            resourceAddress = nftItem.address
                        ).value()
                    } ?: emptyList()
                }
                is ResourceRequest.NewlyCreated -> {
                    tokenSymbol = when (val entry = resourceRequest.metadata.firstOrNull { it.key == KEY_SYMBOL }?.value) {
                        is MetadataEntry.Value -> when (val value = entry.value) {
                            is MetadataValue.String -> value.value
                            else -> "Unknown"
                        }
                        else -> "Unknown"
                    }
                    iconUrl = when (val entry = resourceRequest.metadata.firstOrNull { it.key == KEY_ICON }?.value) {
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
            val guaranteedAmount = if (createdEntity || !includesGuarantees) null else amount

            account = Result.Success(
                TransactionAccountItemUiModel(
                    address = accountItem?.address.orEmpty(),
                    displayName = accountDisplayName,
                    appearanceID = accountAppearanceId,
                    tokenSymbol = tokenSymbol,
                    tokenQuantity = amount,
                    iconUrl = iconUrl,
                    shouldPromptForGuarantees = shouldPromptForGuarantees,
                    guaranteedQuantity = guaranteedAmount,
                    instructionIndex = instructionIndex,
                    resourceAddress = resourceAddress,
                    index = index,
                    fungibleResource = fungibleResource,
                    nonFungibleResourceItems = nonFungibleResources
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
