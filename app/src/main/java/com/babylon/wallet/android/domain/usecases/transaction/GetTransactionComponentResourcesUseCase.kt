package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.repository.entity.EntityRepository
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.model.MetadataConstants.KEY_ICON
import com.babylon.wallet.android.domain.model.MetadataConstants.KEY_SYMBOL
import com.babylon.wallet.android.domain.model.Resource
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

        var account: Result<TransactionAccountItemUiModel> = Result.Error()

        // If its newlyCreatedEntity do not ask for guarantees
        val shouldPromptForGuarantees = if (createdEntity) false else includesGuarantees

        // If its newlyCreatedEntity OR don't include guarantee, do not ask for guarantees
        val guaranteedAmount = if (createdEntity || !includesGuarantees) null else amount

        var tokenSymbol: String? = null
        var iconUrl = ""

        var fungibleResource: Resource.FungibleResource? = null
        var nonFungibleResources: List<Resource.NonFungibleResource.Item> = emptyList()

        entityRepository.getResources(
            addresses = validatedAddresses,
            ids = ids,
            amount = amount,
            guaranteedAmount = guaranteedAmount,
            isRefreshing = false
        ).onValue { response ->
            when (resourceRequest) {
                is ResourceRequest.Existing -> {
                    // todo
                    fungibleResource = response.first
                    nonFungibleResources = response.second
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
                .accountOnCurrentNetwork(componentAddress)
            val accountDisplayName = accountOnProfile?.displayName.orEmpty()
            val accountAppearanceId = accountOnProfile?.appearanceID ?: 1

            account = Result.Success(
                TransactionAccountItemUiModel(
                    address = componentAddress,
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
