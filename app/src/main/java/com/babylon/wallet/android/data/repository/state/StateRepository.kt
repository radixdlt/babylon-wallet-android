package com.babylon.wallet.android.data.repository.state

import com.babylon.wallet.android.data.gateway.apis.StateApi
import com.babylon.wallet.android.data.gateway.extensions.amount
import com.babylon.wallet.android.data.gateway.extensions.amountDecimal
import com.babylon.wallet.android.data.gateway.extensions.asMetadataItems
import com.babylon.wallet.android.data.gateway.extensions.divisibility
import com.babylon.wallet.android.data.gateway.extensions.extractBehaviours
import com.babylon.wallet.android.data.gateway.extensions.totalSupply
import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollectionItem
import com.babylon.wallet.android.data.gateway.generated.models.LedgerState
import com.babylon.wallet.android.data.gateway.generated.models.LedgerStateSelector
import com.babylon.wallet.android.data.gateway.generated.models.NonFungibleResourcesCollectionItem
import com.babylon.wallet.android.data.gateway.generated.models.ResourceAggregationLevel
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsOptIns
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItem
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityFungiblesPageRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityNonFungiblesPageRequest
import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey.*
import com.babylon.wallet.android.data.repository.executeSafe
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.Resources
import com.babylon.wallet.android.domain.model.metadata.MetadataItem.Companion.consume
import rdx.works.profile.data.model.pernetwork.Network
import java.lang.RuntimeException
import javax.inject.Inject

interface StateRepository {

    suspend fun getAccountsState(accounts: List<Network.Account>): Result<Map<Network.Account, Resources>>

}

class StateRepositoryImpl @Inject constructor(
    private val stateApi: StateApi
): StateRepository {

    override suspend fun getAccountsState(accounts: List<Network.Account>): Result<Map<Network.Account, Resources>> {
        val state = mutableMapOf<Network.Account, Resources>()
        accounts.chunked(ENTITY_DETAILS_PAGE_LIMIT).map { accountsChunked ->
            stateApi.entityDetails(
                StateEntityDetailsRequest(
                    addresses = accountsChunked.map { it.address },
                    aggregationLevel = ResourceAggregationLevel.vault,
                    optIns = StateEntityDetailsOptIns(
                        explicitMetadata = listOf(
                            ACCOUNT_TYPE,

                            NAME,
                            SYMBOL,
                            DESCRIPTION,
                            RELATED_WEBSITES,
                            ICON_URL,
                            INFO_URL,
                            VALIDATOR,
                            POOL,
                            TAGS,
                            DAPP_DEFINITIONS,
                        ).map { it.key }
                    )
                )
            ).executeSafe().mapCatching { response ->
                accountsChunked.associateWith { account ->
                    val stateForAccount = response.items.find { it.address == account.address }

                    Resources(
                        fungibles = stateForAccount?.getAllFungibles(atLedgerState = response.ledgerState).orEmpty(),
                        nonFungibles = stateForAccount?.getAllNonFungibles(atLedgerState = response.ledgerState).orEmpty()
                    )
                }
            }.fold(
                onSuccess = { state.putAll(it) },
                onFailure = {
                    return Result.failure(it)
                }
            )
        }

        return Result.success(state)
    }

    private suspend fun StateEntityDetailsResponseItem.getAllFungibles(
        atLedgerState: LedgerState
    ): List<Resource.FungibleResource> {
        val result = mutableListOf<Resource.FungibleResource>()
        result.addAll(fungibleResources?.items?.map { it.asFungibleResource() }.orEmpty())

        var nextCursor: String? = fungibleResources?.nextCursor
        while (nextCursor != null) {
            val pageResponse = stateApi.entityFungiblesPage(
                StateEntityFungiblesPageRequest(
                    address = address,
                    cursor = nextCursor,
                    aggregationLevel = ResourceAggregationLevel.vault,
                    atLedgerState = LedgerStateSelector(stateVersion = atLedgerState.stateVersion)
                )
            ).executeSafe().getOrThrow()

            nextCursor = pageResponse.nextCursor
            result.addAll(pageResponse.items.map { it.asFungibleResource() })
        }

        return result
    }

    private suspend fun StateEntityDetailsResponseItem.getAllNonFungibles(
        atLedgerState: LedgerState
    ): List<Resource.NonFungibleResource> {
        val result = mutableListOf<Resource.NonFungibleResource>()
        result.addAll(nonFungibleResources?.items?.map { it.asNonFungibleResource() }.orEmpty())

        var nextCursor: String? = nonFungibleResources?.nextCursor
        while (nextCursor != null) {
            val pageResponse = stateApi.entityNonFungiblesPage(
                StateEntityNonFungiblesPageRequest(
                    address = address,
                    cursor = nextCursor,
                    aggregationLevel = ResourceAggregationLevel.vault,
                    atLedgerState = LedgerStateSelector(stateVersion = atLedgerState.stateVersion)
                )
            ).executeSafe().getOrThrow()

            nextCursor = pageResponse.nextCursor
            result.addAll(pageResponse.items.map { it.asNonFungibleResource() })
        }

        return result
    }

    companion object {
        private const val ENTITY_DETAILS_PAGE_LIMIT = 20

        fun FungibleResourcesCollectionItem.asFungibleResource(): Resource.FungibleResource {
            val metaDataItems = explicitMetadata?.asMetadataItems().orEmpty().toMutableList()
            return Resource.FungibleResource(
                resourceAddress = resourceAddress,
                ownedAmount = amountDecimal,
                nameMetadataItem = metaDataItems.consume(),
                symbolMetadataItem = metaDataItems.consume(),
                descriptionMetadataItem = metaDataItems.consume(),
                iconUrlMetadataItem = metaDataItems.consume(),
                tagsMetadataItem = metaDataItems.consume(),
                behaviours = emptySet(),
                currentSupply = null,
                validatorMetadataItem = metaDataItems.consume(),
                poolMetadataItem = metaDataItems.consume(),
                divisibility = null
            )
        }

        fun NonFungibleResourcesCollectionItem.asNonFungibleResource(): Resource.NonFungibleResource {
            val metaDataItems = explicitMetadata?.asMetadataItems().orEmpty().toMutableList()
            return Resource.NonFungibleResource(
                resourceAddress = resourceAddress,
                amount = amount,
                nameMetadataItem = metaDataItems.consume(),
                descriptionMetadataItem = metaDataItems.consume(),
                iconMetadataItem = metaDataItems.consume(),
                tagsMetadataItem = metaDataItems.consume(),
                behaviours = emptySet(),
                items = emptyList(),
                currentSupply = null,
                validatorMetadataItem = metaDataItems.consume(),
                dAppDefinitionsMetadataItem = metaDataItems.consume(),
            )
        }

        fun StateEntityDetailsResponseItem.asFungibleResource(): Resource.FungibleResource {
            val resourceBehaviours = details?.extractBehaviours().orEmpty()
            val currentSupply = details?.totalSupply()?.toBigDecimal()
            val metaDataItems = explicitMetadata?.asMetadataItems().orEmpty().toMutableList()
            return Resource.FungibleResource(
                resourceAddress = address,
                ownedAmount = null,
                nameMetadataItem = metaDataItems.consume(),
                symbolMetadataItem = metaDataItems.consume(),
                descriptionMetadataItem = metaDataItems.consume(),
                iconUrlMetadataItem = metaDataItems.consume(),
                tagsMetadataItem = metaDataItems.consume(),
                behaviours = resourceBehaviours,
                currentSupply = currentSupply,
                validatorMetadataItem = metaDataItems.consume(),
                poolMetadataItem = metaDataItems.consume(),
                divisibility = details?.divisibility()
            )
        }

        fun StateEntityDetailsResponseItem.asNonFungibleResource(): Resource.NonFungibleResource {
            val resourceBehaviours = details?.extractBehaviours().orEmpty()
            val currentSupply = details?.totalSupply()?.toIntOrNull()
            val metaDataItems = explicitMetadata?.asMetadataItems().orEmpty().toMutableList()
            return Resource.NonFungibleResource(
                resourceAddress = address,
                amount = 0,
                nameMetadataItem = metaDataItems.consume(),
                descriptionMetadataItem = metaDataItems.consume(),
                iconMetadataItem = metaDataItems.consume(),
                tagsMetadataItem = metaDataItems.consume(),
                behaviours = resourceBehaviours,
                items = emptyList(),
                currentSupply = currentSupply,
                validatorMetadataItem = metaDataItems.consume(),
                dAppDefinitionsMetadataItem = metaDataItems.consume(),
            )
        }
    }

}
