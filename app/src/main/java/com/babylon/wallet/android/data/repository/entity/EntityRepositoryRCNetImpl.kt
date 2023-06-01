//package com.babylon.wallet.android.data.repository.entity
//
//import android.net.Uri
//import androidx.core.net.toUri
//import com.babylon.wallet.android.data.gateway.apis.StateApi
//import com.babylon.wallet.android.data.gateway.extensions.allResourceAddresses
//import com.babylon.wallet.android.data.gateway.extensions.amount
//import com.babylon.wallet.android.data.gateway.extensions.amountDecimal
//import com.babylon.wallet.android.data.gateway.extensions.asMetadataStringMap
//import com.babylon.wallet.android.data.gateway.extensions.nonFungibleResourceAddresses
//import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollectionItem
//import com.babylon.wallet.android.data.gateway.generated.models.NonFungibleResourcesCollectionItem
//import com.babylon.wallet.android.data.gateway.generated.models.ResourceAggregationLevel
//import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsRequest
//import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponse
//import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItem
//import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItemDetailsType
//import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleDataRequest
//import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleDetailsResponseItem
//import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleIdsRequest
//import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
//import com.babylon.wallet.android.data.repository.cache.CacheParameters
//import com.babylon.wallet.android.data.repository.cache.HttpCache
//import com.babylon.wallet.android.data.repository.cache.TimeoutDuration
//import com.babylon.wallet.android.data.repository.cache.TimeoutDuration.NO_CACHE
//import com.babylon.wallet.android.data.repository.execute
//import com.babylon.wallet.android.domain.common.Result
//import com.babylon.wallet.android.domain.common.map
//import com.babylon.wallet.android.domain.common.value
//import com.babylon.wallet.android.domain.model.AccountWithResources
//import com.babylon.wallet.android.domain.model.DAppResources
//import com.babylon.wallet.android.domain.model.DAppWithMetadata
//import com.babylon.wallet.android.domain.model.Resource
//import com.babylon.wallet.android.domain.model.Resources
//import com.babylon.wallet.android.domain.model.metadata.DescriptionMetadataItem
//import com.babylon.wallet.android.domain.model.metadata.IconUrlMetadataItem
//import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem
//import com.babylon.wallet.android.domain.model.metadata.SymbolMetadataItem
//import com.babylon.wallet.android.presentation.model.ActionableAddress
//import rdx.works.profile.data.model.pernetwork.Network
//import java.math.BigDecimal
//import javax.inject.Inject
//
//class EntityRepositoryRCNetImpl @Inject constructor(
//    private val stateApi: StateApi,
//    private val cache: HttpCache
//) : EntityRepository {
//
//    override suspend fun getAccountsWithResources(
//        accounts: List<Network.Account>,
//        explicitMetadataForAssets: Set<ExplicitMetadataKey>, // Not needed for RCNet. Metadata are dynamic and not explicit
//        isRefreshing: Boolean
//    ): Result<List<AccountWithResources>> = stateEntityDetails(
//        addresses = accounts.map { it.address },
//        isRefreshing = isRefreshing
//    ).map { response ->
//        val allResourceAddressesForAccounts = response.items.map { resourceItem ->
//            resourceItem.allResourceAddresses
//        }.flatten()
//
//        val allResources = stateEntityDetails(
//            addresses = allResourceAddressesForAccounts,
//            isRefreshing = isRefreshing
//        ).value()?.items.orEmpty()
//
//        val nonFungibleAddresses = response.items.map { it.nonFungibleResourceAddresses }.flatten()
//        // A map of <Non fungible ResourceAddress, List of NFT items>
//        val nonFungiblesWithData = resolveNonFungibleData(nonFungibleAddresses, isRefreshing)
//
//        accounts.mapNotNull { profileAccount ->
//            val accountOnGateway = response.items.find { it.address == profileAccount.address } ?: return@mapNotNull null
//
//            AccountWithResources(
//                account = profileAccount,
//                resources = Resources(
//                    fungibleResources = fungibleResources(
//                        accountOnGateway.fungibleResources?.items.orEmpty(),
//                        allResources
//                    ),
//                    nonFungibleResources = nonFungibleResources(
//                        accountOnGateway.nonFungibleResources?.items.orEmpty(),
//                        allResources,
//                        nonFungiblesWithData
//                    )
//                )
//            )
//        }
//    }
//
//    override suspend fun getDAppResources(
//        dAppMetadata: DAppWithMetadata,
//        isRefreshing: Boolean
//    ): Result<DAppResources> {
//        val claimedResources = dAppMetadata.claimedEntities.filter {
//            ActionableAddress.Type.from(it) == ActionableAddress.Type.RESOURCE
//        }
//        return stateEntityDetails(
//            claimedResources
//        ).map { response ->
//            val allResources = response.items
//
//            val fungibleItems = allResources.filter {
//                it.details?.type == StateEntityDetailsResponseItemDetailsType.fungibleResource
//            }
//            val nonFungibleItems = allResources.filter {
//                it.details?.type == StateEntityDetailsResponseItemDetailsType.nonFungibleResource
//            }
//
////            // A map of <Non fungible ResourceAddress, List of NFT items>
////            val nonFungiblesWithData = resolveNonFungibleData(nonFungibleItems.map { it.address }, isRefreshing)
//
//            val fungibleResources = fungibleItems.map { fungibleItem ->
//                val metadataMap = fungibleItem.metadata.asMetadataStringMap()
//                Resource.FungibleResource(
//                    resourceAddress = fungibleItem.address,
//                    amount = BigDecimal.ZERO, // No amount given in metadata
//                    nameMetadataItem = metadataMap[ExplicitMetadataKey.NAME.key]?.let { NameMetadataItem(it) },
//                    symbolMetadataItem = metadataMap[ExplicitMetadataKey.SYMBOL.key]?.let { SymbolMetadataItem(it) },
//                    descriptionMetadataItem = metadataMap[ExplicitMetadataKey.DESCRIPTION.key]?.let { DescriptionMetadataItem(it) },
//                    iconUrlMetadataItem = metadataMap[ExplicitMetadataKey.ICON_URL.key]?.let { IconUrlMetadataItem(it.toUri()) }
//                )
//            }
//
////            // A map of <Non fungible ResourceAddress, List of NFT items>
//            val nonFungiblesWithData = resolveNonFungibleData(nonFungibleItems.map { it.address }, isRefreshing)
//
////            val nonFungibleResource = nonFungibleItems.map { nonFungibleItem ->
////
////                val metadataMap = nonFungibleItem.metadata.asMetadataStringMap()
////                Resource.NonFungibleResource(
////                    resourceAddress = nonFungibleItem.address,
////                    amount = 0L, // No amount given in metadata
////                    nameMetadataItem = metadataMap[ExplicitMetadataKey.NAME.key]?.let { NameMetadataItem(it) },
////                    descriptionMetadataItem = metadataMap[ExplicitMetadataKey.DESCRIPTION.key]?.let {
////                        DescriptionMetadataItem(
////                            it
////                        )
////                    },
////                    iconMetadataItem = metadataMap[ExplicitMetadataKey.ICON_URL.key]?.let { IconUrlMetadataItem(it.toUri()) },
////                    items = nonFungiblesWithData[nonFungibleItem.address].orEmpty()
////                )
////            }
//            val nonFungibleResources = nonFungibleResources(
//                nonFungibleItems.map { it.nonFungibleResources?.items.orEmpty() }.flatten(),
//                allResources,
//                nonFungiblesWithData
//            )
//
//
////            val nonFungibleResources = nonFungiblesWithData[nonFungibleItems.map { it.address }].orEmpty()
////            val nonFungibleResources = nonFungibleItems.map { nonFungibleItem ->
////                nonFungiblesWithData[nonFungibleItem.address]
////            }
////                val metadataMap = nonFungibleItem.metadata.asMetadataStringMap()
////                Resource.NonFungibleResource.Item(
////                    localId = "",
////                    iconMetadataItem = metadataMap[ExplicitMetadataKey.ICON_URL.key]?.let {
////                        IconUrlMetadataItem(it.toUri())
////                    },
////                )
////                Resource.NonFungibleResource(
////                    resourceAddress = nonFungibleItem.address,
////                    amount = 0L, // No amount given in metadata
////                    nameMetadataItem = metadataMap[ExplicitMetadataKey.NAME.key]?.let { NameMetadataItem(it) },
////                    descriptionMetadataItem = metadataMap[ExplicitMetadataKey.DESCRIPTION.key]?.let {
////                        DescriptionMetadataItem(
////                            it
////                        )
////                    },
////                    iconMetadataItem = metadataMap[ExplicitMetadataKey.ICON_URL.key]?.let { IconUrlMetadataItem(it.toUri()) },
////                    items = nonFungiblesWithData[nonFungibleItem.address].orEmpty()
////                )
////            }
//            DAppResources(
//                fungibleResources = fungibleResources,
//                nonFungibleResources = nonFungibleResources,
////                nonFungibleItems = nonFungiblesWithData[nonFungibleItem.address].orEmpty()
//            )
//        }
//    }
//
//    private fun fungibleResources(
//        fungibleResources: List<FungibleResourcesCollectionItem>,
//        allResources: List<StateEntityDetailsResponseItem>
//    ) = fungibleResources.mapNotNull { item ->
//        val resourceDetails = allResources.find { resource ->
//            resource.address == item.resourceAddress
//        } ?: return@mapNotNull null
//
//        val metadataMap = resourceDetails.metadata.asMetadataStringMap()
//
//        Resource.FungibleResource(
//            resourceAddress = item.resourceAddress,
//            amount = item.amountDecimal,
//            nameMetadataItem = metadataMap[ExplicitMetadataKey.NAME.key]?.let { NameMetadataItem(it) },
//            symbolMetadataItem = metadataMap[ExplicitMetadataKey.SYMBOL.key]?.let { SymbolMetadataItem(it) },
//            descriptionMetadataItem = metadataMap[ExplicitMetadataKey.DESCRIPTION.key]?.let { DescriptionMetadataItem(it) },
//            iconUrlMetadataItem = metadataMap[ExplicitMetadataKey.ICON_URL.key]?.let { IconUrlMetadataItem(it.toUri()) }
//        )
//    }
//
//    private fun nonFungibleResources(
//        nonFungibleResources: List<NonFungibleResourcesCollectionItem>,
//        allResources: List<StateEntityDetailsResponseItem>,
//        nonFungiblesWithData: Map<String, List<Resource.NonFungibleResource.Item>>
//    ) = nonFungibleResources.mapNotNull { item ->
//        val resourceDetails = allResources.find { resource ->
//            resource.address == item.resourceAddress
//        } ?: return@mapNotNull null
//
//        val metadataMap = resourceDetails.metadata.asMetadataStringMap()
//
//        Resource.NonFungibleResource(
//            resourceAddress = item.resourceAddress,
//            amount = item.amount,
//            nameMetadataItem = metadataMap[ExplicitMetadataKey.NAME.key]?.let { NameMetadataItem(it) },
//            descriptionMetadataItem = metadataMap[ExplicitMetadataKey.DESCRIPTION.key]?.let { DescriptionMetadataItem(it) },
//            iconMetadataItem = metadataMap[ExplicitMetadataKey.ICON_URL.key]?.let { IconUrlMetadataItem(it.toUri()) },
//            items = nonFungiblesWithData[item.resourceAddress].orEmpty()
//        )
//    }
//
//    private suspend fun resolveNonFungibleData(
//        nonFungibleAddresses: List<String>,
//        isRefreshing: Boolean
//    ): Map<String, List<Resource.NonFungibleResource.Item>> {
//        val nonFungibleAddressesWithIds = nonFungibleAddresses
//            .associateWith { nonFungibleResourceAddress ->
//                stateApi.nonFungibleIds(StateNonFungibleIdsRequest(nonFungibleResourceAddress)).execute(
//                    cacheParameters = CacheParameters(
//                        httpCache = cache,
//                        timeoutDuration = if (isRefreshing) NO_CACHE else TimeoutDuration.FIVE_MINUTES
//                    ),
//                    map = { nonFungibleIdsResponse ->
//                        nonFungibleIdsResponse.nonFungibleIds.items.map { it }
//                    }
//                ).value().orEmpty()
//            }
//
//        // A map of <Non fungible ResourceAddress, List of NFT data>
//        return nonFungibleAddresses.associateWith { nonFungibleResourceAddress ->
//            stateApi.nonFungibleData(
//                StateNonFungibleDataRequest(
//                    resourceAddress = nonFungibleResourceAddress,
//                    nonFungibleIds = nonFungibleAddressesWithIds[nonFungibleResourceAddress].orEmpty()
//                )
//            ).execute(
//                cacheParameters = CacheParameters(
//                    httpCache = cache,
//                    timeoutDuration = if (isRefreshing) NO_CACHE else TimeoutDuration.ONE_MINUTE
//                ),
//                map = { response ->
//                    response.nonFungibleIds.map {
//                        Resource.NonFungibleResource.Item(
//                            collectionAddress = nonFungibleResourceAddress,
//                            localId = it.nonFungibleId,
//                            iconMetadataItem = it.nftImage()?.let { imageUrl -> IconUrlMetadataItem(url = imageUrl) }
//                        )
//                    }
//                }
//            ).value().orEmpty()
//        }
//    }
//
//    override suspend fun stateEntityDetails(addresses: List<String>, isRefreshing: Boolean): Result<StateEntityDetailsResponse> {
//        return stateApi.entityDetails(
//            StateEntityDetailsRequest(
//                addresses = addresses,
//                aggregationLevel = ResourceAggregationLevel.global
//            )
//        ).execute(
//            cacheParameters = CacheParameters(
//                httpCache = cache,
//                timeoutDuration = if (isRefreshing) NO_CACHE else TimeoutDuration.ONE_MINUTE
//            ),
//            map = { it }
//        )
//    }
//
//    private fun StateNonFungibleDetailsResponseItem.nftImage(): Uri? = data.rawJson.fields.find { element ->
//        val value = element.value
//        value.contains("https")
//    }?.value?.toUri()
//}
