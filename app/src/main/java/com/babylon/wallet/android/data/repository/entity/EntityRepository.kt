package com.babylon.wallet.android.data.repository.entity

import android.net.Uri
import com.babylon.wallet.android.data.gateway.apis.StateApi
import com.babylon.wallet.android.data.gateway.extensions.allResourceAddresses
import com.babylon.wallet.android.data.gateway.extensions.asMetadataItems
import com.babylon.wallet.android.data.gateway.extensions.calculateResourceBehaviours
import com.babylon.wallet.android.data.gateway.extensions.claimTokenResourceAddress
import com.babylon.wallet.android.data.gateway.extensions.divisibility
import com.babylon.wallet.android.data.gateway.extensions.getXRDVaultAmount
import com.babylon.wallet.android.data.gateway.extensions.stakeUnitResourceAddress
import com.babylon.wallet.android.data.gateway.extensions.totalSupply
import com.babylon.wallet.android.data.gateway.extensions.xrdVaultAddress
import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollection
import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollectionItemVaultAggregated
import com.babylon.wallet.android.data.gateway.generated.models.LedgerStateSelector
import com.babylon.wallet.android.data.gateway.generated.models.MetadataValueType
import com.babylon.wallet.android.data.gateway.generated.models.NonFungibleResourcesCollection
import com.babylon.wallet.android.data.gateway.generated.models.NonFungibleResourcesCollectionItemVaultAggregated
import com.babylon.wallet.android.data.gateway.generated.models.ResourceAggregationLevel
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsOptIns
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponse
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseFungibleResourceDetails
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItem
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseNonFungibleResourceDetails
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityFungiblesPageRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityFungiblesPageResponse
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityNonFungibleIdsPageRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityNonFungiblesPageRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityNonFungiblesPageResponse
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleDataRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleDetailsResponseItem
import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.data.repository.cache.CacheParameters
import com.babylon.wallet.android.data.repository.cache.HttpCache
import com.babylon.wallet.android.data.repository.cache.TimeoutDuration
import com.babylon.wallet.android.data.repository.cache.TimeoutDuration.NO_CACHE
import com.babylon.wallet.android.data.repository.execute
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.common.map
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.common.switchMap
import com.babylon.wallet.android.domain.common.value
import com.babylon.wallet.android.domain.model.AccountWithResources
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.Resources
import com.babylon.wallet.android.domain.model.ValidatorDetail
import com.babylon.wallet.android.domain.model.ValidatorWithStakeResources
import com.babylon.wallet.android.domain.model.ValidatorsWithStakeResources
import com.babylon.wallet.android.domain.model.metadata.ClaimAmountMetadataItem
import com.babylon.wallet.android.domain.model.metadata.DescriptionMetadataItem
import com.babylon.wallet.android.domain.model.metadata.IconUrlMetadataItem
import com.babylon.wallet.android.domain.model.metadata.MetadataItem
import com.babylon.wallet.android.domain.model.metadata.MetadataItem.Companion.consume
import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem
import com.babylon.wallet.android.domain.model.metadata.OwnerKeyHashesMetadataItem
import com.babylon.wallet.android.domain.model.metadata.StringMetadataItem
import rdx.works.profile.data.model.pernetwork.Network
import timber.log.Timber
import java.io.IOException
import java.math.BigDecimal
import javax.inject.Inject

interface EntityRepository {

    suspend fun getAccountsWithResources(
        accounts: List<Network.Account>,
        // we pass a combination of fungible AND non fungible explicit metadata keys
        explicitMetadataForAssets: Set<ExplicitMetadataKey> = ExplicitMetadataKey.forAssets,
        isNftItemDataNeeded: Boolean = true,
        isRefreshing: Boolean = true
    ): Result<List<AccountWithResources>>

    suspend fun getResources(
        resourceAddresses: List<String>,
        explicitMetadataForAssets: Set<ExplicitMetadataKey> = ExplicitMetadataKey.forAssets,
        isRefreshing: Boolean = true
    ): kotlin.Result<List<Resource>>

    suspend fun getEntityOwnerKeyHashes(
        entityAddress: String,
        isRefreshing: Boolean = false
    ): Result<OwnerKeyHashesMetadataItem?>
}

@Suppress("TooManyFunctions", "LongMethod", "LargeClass")
class EntityRepositoryImpl @Inject constructor(
    private val stateApi: StateApi,
    private val cache: HttpCache
) : EntityRepository {

    override suspend fun getAccountsWithResources(
        accounts: List<Network.Account>,
        explicitMetadataForAssets: Set<ExplicitMetadataKey>,
        isNftItemDataNeeded: Boolean,
        isRefreshing: Boolean
    ): Result<List<AccountWithResources>> {
        if (accounts.isEmpty()) return Result.Success(emptyList())

        val listOfEntityDetailsResponsesResult = getStateEntityDetailsResponse(
            addresses = accounts.map { it.address },
            explicitMetadata = explicitMetadataForAssets,
            isRefreshing = isRefreshing
        )

        return listOfEntityDetailsResponsesResult.switchMap { accountDetailsResponses ->
            val stateVersion = accountDetailsResponses.firstOrNull()?.ledgerState?.stateVersion ?: return Result.Error()

            val resourceAddresses = accountDetailsResponses.map { stateEntityDetailsResponse ->
                stateEntityDetailsResponse.items.map { stateEntityDetailsResponseItem ->
                    stateEntityDetailsResponseItem.allResourceAddresses
                }.flatten()
            }.flatten()
            val resourcesDetails = getDetailsForResources(
                resourceAddresses = resourceAddresses,
                stateVersion = stateVersion
            )
            val mapOfAccountsWithMetadata = buildMapOfAccountsAddressesWithMetadata(accountDetailsResponses)
            var mapOfAccountsWithFungibleResources =
                buildMapOfAccountsWithFungibles(accountDetailsResponses, resourcesDetails, stateVersion)
            var mapOfAccountsWithNonFungibleResources = buildMapOfAccountsWithNonFungibles(
                accountDetailsResponses = accountDetailsResponses,
                resourcesDetails = resourcesDetails,
                isNftItemDataNeeded = isNftItemDataNeeded,
                isRefreshing = isRefreshing,
                stateVersion = stateVersion
            )
            val validatorDetails =
                getAllValidatorDetails(
                    mapOfAccountsWithFungibleResources = mapOfAccountsWithFungibleResources,
                    mapOfAccountsWithNonFungibleResources = mapOfAccountsWithNonFungibleResources,
                    isRefreshing = isRefreshing,
                    stateVersion = stateVersion
                )
            val poolsList = getPoolDetails(
                mapOfAccountsWithFungibleResources = mapOfAccountsWithFungibleResources,
                isRefreshing = isRefreshing,
                stateVersion = stateVersion
            )
            val validatorResourceAddresses = validatorDetails.map { item ->
                listOfNotNull(item.details?.stakeUnitResourceAddress(), item.details?.claimTokenResourceAddress)
            }.flatten().toSet()
            val poolAddresses = poolsList.map { item -> item.address }.toSet()

            val mapOfAccountsWithLiquidStakeUnitResources = mapOfAccountsWithFungibleResources.mapValues { fungibleResources ->
                fungibleResources.value.filter { validatorResourceAddresses.contains(it.resourceAddress) }.map {
                    Resource.LiquidStakeUnitResource(it)
                }
            }.filter { it.value.isNotEmpty() }
            val mapOfAccountsWithStakeClaimNFT = mapOfAccountsWithNonFungibleResources.mapValues { nftResource ->
                nftResource.value.filter {
                    validatorResourceAddresses.contains(it.resourceAddress)
                }.map { Resource.StakeClaimResource(it) }
            }.filter { it.value.isNotEmpty() }
            val mapOfAccountsWithPoolUnits = mapOfAccountsWithFungibleResources.mapValues { fungibleResources ->
                fungibleResources.value.filter { poolAddresses.contains(it.poolAddress) }.map { poolUnitResource ->
                    val poolDetails = poolsList.find { it.address == poolUnitResource.poolAddress }
                    val poolResources = poolDetails?.fungibleResources?.items?.mapNotNull { poolResource ->
                        (poolResource as? FungibleResourcesCollectionItemVaultAggregated)?.let {
                            mapToFungibleResource(it)
                        }
                    }.orEmpty()
                    Resource.PoolUnitResource(poolUnitResource, poolResources)
                }
            }.filter { it.value.isNotEmpty() }

            mapOfAccountsWithFungibleResources = mapOfAccountsWithFungibleResources.mapValues { fungibleResources ->
                fungibleResources.value.filter {
                    !validatorResourceAddresses.contains(it.resourceAddress) && !poolAddresses.contains(it.poolAddress)
                }
            }

            mapOfAccountsWithNonFungibleResources = mapOfAccountsWithNonFungibleResources.mapValues { nonFungibleResources ->
                nonFungibleResources.value.filter { !validatorResourceAddresses.contains(it.resourceAddress) }
            }

            val liquidStakeCollectionPerAccountAddress = buildMapOfAccountAddressesWithLiquidStakeResources(
                mapOfAccountsWithLiquidStakeUnitResources,
                mapOfAccountsWithStakeClaimNFT,
                validatorDetails
            )

            // build result list of accounts with resources
            val listOfAccountsWithResources = accounts.map { account ->
                val metaDataItems = mapOfAccountsWithMetadata[account.address].orEmpty().toMutableList()
                AccountWithResources(
                    account = account,
                    accountTypeMetadataItem = metaDataItems.consume(),
                    resources = Resources(
                        fungibleResources = mapOfAccountsWithFungibleResources[account.address].orEmpty().sorted(),
                        nonFungibleResources = mapOfAccountsWithNonFungibleResources[account.address].orEmpty().sorted(),
                        validatorsWithStakeResources = liquidStakeCollectionPerAccountAddress[account.address]
                            ?: ValidatorsWithStakeResources(),
                        poolUnits = mapOfAccountsWithPoolUnits[account.address].orEmpty()
                    )
                )
            }

            Result.Success(listOfAccountsWithResources)
        }
    }

    override suspend fun getResources(
        resourceAddresses: List<String>,
        explicitMetadataForAssets: Set<ExplicitMetadataKey>,
        isRefreshing: Boolean
    ): kotlin.Result<List<Resource>> {
        return kotlin.Result.success(
            getDetailsForResources(resourceAddresses).mapNotNull { resourceDetails ->
                when (resourceDetails.details) {
                    is StateEntityDetailsResponseFungibleResourceDetails -> {
                        mapToFungibleResource(resourceDetails)
                    }

                    is StateEntityDetailsResponseNonFungibleResourceDetails -> {
                        mapToNonFungibleResource(resourceDetails)
                    }

                    else -> null
                }
            }
        )
    }

    private suspend fun getAllValidatorDetails(
        mapOfAccountsWithFungibleResources: Map<String, List<Resource.FungibleResource>>,
        mapOfAccountsWithNonFungibleResources: Map<String, List<Resource.NonFungibleResource>>,
        isRefreshing: Boolean,
        stateVersion: Long?
    ): List<StateEntityDetailsResponseItem> {
        val allValidatorAddresses = (
            mapOfAccountsWithFungibleResources.map { accountWithFungibles ->
                accountWithFungibles.value.mapNotNull { it.validatorAddress }
            }.flatten() + mapOfAccountsWithNonFungibleResources.map { accountWithNonFungibles ->
                accountWithNonFungibles.value.mapNotNull { stakeClaimNtf -> stakeClaimNtf.validatorAddress }
            }.flatten()
            ).toSet()
        if (allValidatorAddresses.isEmpty()) return emptyList()
        return getStateEntityDetailsResponse(
            addresses = allValidatorAddresses,
            explicitMetadata = ExplicitMetadataKey.forValidatorsAndPools,
            isRefreshing = isRefreshing,
            stateVersion = stateVersion
        ).value().orEmpty().map {
            it.items
        }.flatten()
    }

    private suspend fun getPoolDetails(
        mapOfAccountsWithFungibleResources: Map<String, List<Resource.FungibleResource>>,
        isRefreshing: Boolean,
        stateVersion: Long?
    ): List<StateEntityDetailsResponseItem> {
        val poolAddresses =
            mapOfAccountsWithFungibleResources.map { accountWithFungibles ->
                accountWithFungibles.value.mapNotNull { it.poolAddress }
            }.flatten()
                .toSet()
        if (poolAddresses.isEmpty()) return emptyList()
        return getStateEntityDetailsResponse(
            addresses = poolAddresses,
            explicitMetadata = ExplicitMetadataKey.forValidatorsAndPools,
            isRefreshing = isRefreshing,
            stateVersion = stateVersion
        ).value().orEmpty().map {
            it.items
        }.flatten()
    }

    private fun buildMapOfAccountAddressesWithLiquidStakeResources(
        accountAddressToLiquidStakeUnits: Map<String, List<Resource.LiquidStakeUnitResource>>,
        accountAddressToStakeClaimNtfs: Map<String, List<Resource.StakeClaimResource>>,
        validatorDetailsList: List<StateEntityDetailsResponseItem>,
    ): Map<String, ValidatorsWithStakeResources> {
        if (validatorDetailsList.isEmpty()) return emptyMap()
        val accountAddresses = accountAddressToLiquidStakeUnits.keys + accountAddressToStakeClaimNtfs.keys
        return accountAddresses.associateWith { address ->
            val allValidatorAddresses = (
                accountAddressToLiquidStakeUnits[address].orEmpty().map { it.validatorAddress } +
                    accountAddressToStakeClaimNtfs[address].orEmpty().map { it.validatorAddress }
                ).toSet()
            val lsuPerValidator = accountAddressToLiquidStakeUnits[address].orEmpty().groupBy {
                it.validatorAddress
            }
            val nftPerValidator = accountAddressToStakeClaimNtfs[address].orEmpty().groupBy {
                it.validatorAddress
            }
            val accountValidators = allValidatorAddresses
                .map { validatorAddress ->
                    val validatorDetails = validatorDetailsList.firstOrNull { it.address == validatorAddress }
                    val totalXrdStake = validatorDetails?.details
                        ?.xrdVaultAddress()
                        ?.let {
                            validatorDetails.getXRDVaultAmount(it)
                        }
                    val validatorMetadata = validatorDetails?.explicitMetadata?.asMetadataItems().orEmpty().toMutableList()
                    val nameMetadata: NameMetadataItem? = validatorMetadata.consume()
                    val descriptionMetadataItem: DescriptionMetadataItem? = validatorMetadata.consume()
                    val iconUrlMetadataItem: IconUrlMetadataItem? = validatorMetadata.consume()
                    ValidatorWithStakeResources(
                        validatorDetail = ValidatorDetail(
                            address = validatorAddress,
                            name = nameMetadata?.name.orEmpty(),
                            description = descriptionMetadataItem?.description.orEmpty(),
                            url = iconUrlMetadataItem?.url,
                            totalXrdStake = totalXrdStake
                        ),
                        liquidStakeUnits = lsuPerValidator[validatorAddress].orEmpty(),
                        stakeClaimNft = nftPerValidator[validatorAddress].orEmpty().firstOrNull()
                    )
                }
            ValidatorsWithStakeResources(
                validators = accountValidators
            )
        }
    }

    private fun buildMapOfAccountsAddressesWithMetadata(
        entityDetailsResponses: List<StateEntityDetailsResponse>
    ): Map<String, List<MetadataItem>> {
        return entityDetailsResponses.map { entityDetailsResponse ->
            entityDetailsResponse.items
                .groupingBy { entityDetailsResponseItem ->
                    entityDetailsResponseItem.address // this is account address
                }
                .foldTo(mutableMapOf(), listOf<MetadataItem>()) { _, entityItem ->
                    entityItem.metadata.asMetadataItems()
                }
        }.flatMap { map ->
            map.asSequence()
        }.associate { map ->
            map.key to map.value
        }
    }

    private suspend fun buildMapOfAccountsWithFungibles(
        entityDetailsResponses: List<StateEntityDetailsResponse>,
        resourcesDetails: List<StateEntityDetailsResponseItem>,
        stateVersion: Long?
    ): Map<String, List<Resource.FungibleResource>> {
        return entityDetailsResponses.map { entityDetailsResponse ->
            entityDetailsResponse.items
                .groupingBy { entityDetailsResponseItem ->
                    entityDetailsResponseItem.address // this is account address
                }
                .foldTo(mutableMapOf(), listOf<Resource.FungibleResource>()) { _, entityItem ->

                    val fungibleResourcesItemsList = if (entityItem.fungibleResources != null) {
                        getFungibleResourcesCollectionItemsForAccount(
                            accountAddress = entityItem.address,
                            fungibleResources = entityItem.fungibleResources,
                            stateVersion = stateVersion
                        )
                    } else {
                        emptyList()
                    }
                    fungibleResourcesItemsList.mapNotNull { fungibleResourcesItem ->
                        val fungibleDetails = resourcesDetails.find {
                            it.address == fungibleResourcesItem.resourceAddress
                        }
                        mapToFungibleResource(fungibleResourcesItem, fungibleDetails)
                    }
                }
        }.flatMap { map ->
            map.asSequence()
        }.associate { map ->
            map.key to map.value
        }
    }

    private fun mapToFungibleResource(
        fungibleResourcesItem: FungibleResourcesCollectionItemVaultAggregated,
        fungibleDetails: StateEntityDetailsResponseItem? = null
    ): Resource.FungibleResource? {
        val resourceBehaviours = fungibleDetails?.details?.calculateResourceBehaviours().orEmpty()
        val currentSupply = fungibleDetails?.details?.totalSupply()?.toBigDecimal()
        val metaDataItems = (fungibleDetails?.explicitMetadata ?: fungibleResourcesItem.explicitMetadata)?.asMetadataItems().orEmpty()
        val ownedAmount = fungibleResourcesItem.vaults.items.first().amount.toBigDecimal()
        if (ownedAmount == BigDecimal.ZERO) return null
        return Resource.FungibleResource(
            resourceAddress = fungibleResourcesItem.resourceAddress,
            ownedAmount = ownedAmount,
            nameMetadataItem = metaDataItems.toMutableList().consume(),
            symbolMetadataItem = metaDataItems.toMutableList().consume(),
            descriptionMetadataItem = metaDataItems.toMutableList().consume(),
            iconUrlMetadataItem = metaDataItems.toMutableList().consume(),
            tagsMetadataItem = metaDataItems.toMutableList().consume(),
            behaviours = resourceBehaviours,
            currentSupply = currentSupply,
            validatorMetadataItem = metaDataItems.toMutableList().consume(),
            poolMetadataItem = metaDataItems.toMutableList().consume(),
            divisibility = fungibleDetails?.details?.divisibility()
        )
    }

    private fun mapToFungibleResource(
        fungibleDetails: StateEntityDetailsResponseItem
    ): Resource.FungibleResource {
        val resourceBehaviours = fungibleDetails.details?.calculateResourceBehaviours().orEmpty()
        val currentSupply = fungibleDetails.details?.totalSupply()?.toBigDecimal()
        val metaDataItems = fungibleDetails.explicitMetadata?.asMetadataItems().orEmpty()
        return Resource.FungibleResource(
            resourceAddress = fungibleDetails.address,
            ownedAmount = null,
            nameMetadataItem = metaDataItems.toMutableList().consume(),
            symbolMetadataItem = metaDataItems.toMutableList().consume(),
            descriptionMetadataItem = metaDataItems.toMutableList().consume(),
            iconUrlMetadataItem = metaDataItems.toMutableList().consume(),
            tagsMetadataItem = metaDataItems.toMutableList().consume(),
            behaviours = resourceBehaviours,
            currentSupply = currentSupply,
            validatorMetadataItem = metaDataItems.toMutableList().consume(),
            poolMetadataItem = metaDataItems.toMutableList().consume(),
            divisibility = fungibleDetails.details?.divisibility()
        )
    }

    private fun mapToNonFungibleResource(nonFungibleDetails: StateEntityDetailsResponseItem): Resource {
        val resourceBehaviours = nonFungibleDetails.details?.calculateResourceBehaviours().orEmpty()
        val currentSupply = nonFungibleDetails.details?.totalSupply()?.toIntOrNull()

        val metaDataItems = nonFungibleDetails.explicitMetadata?.asMetadataItems().orEmpty().toMutableList()

        return Resource.NonFungibleResource(
            resourceAddress = nonFungibleDetails.address,
            amount = 0,
            nameMetadataItem = metaDataItems.consume(),
            descriptionMetadataItem = metaDataItems.consume(),
            iconMetadataItem = metaDataItems.consume(),
            behaviours = resourceBehaviours,
            items = emptyList(),
            currentSupply = currentSupply,
            validatorMetadataItem = metaDataItems.consume()
        )
    }

    private suspend fun buildMapOfAccountsWithNonFungibles(
        accountDetailsResponses: List<StateEntityDetailsResponse>,
        resourcesDetails: List<StateEntityDetailsResponseItem>,
        isNftItemDataNeeded: Boolean = true,
        isRefreshing: Boolean,
        stateVersion: Long?
    ): Map<String, List<Resource.NonFungibleResource>> {
        return accountDetailsResponses.map { accountDetailsResponse ->
            accountDetailsResponse.items
                .groupingBy { entityDetailsResponseItem ->
                    entityDetailsResponseItem.address // this is account address
                }
                .foldTo(mutableMapOf(), listOf<Resource.NonFungibleResource>()) { _, entityItem ->
                    val nonFungibleResourcesItemsList = if (entityItem.nonFungibleResources != null) {
                        getNonFungibleResourcesCollectionItemsForAccount(
                            accountAddress = entityItem.address,
                            nonFungibleResources = entityItem.nonFungibleResources,
                            stateVersion = stateVersion
                        )
                    } else {
                        emptyList()
                    }
                    nonFungibleResourcesItemsList.mapNotNull { nonFungibleResourcesItem ->
                        val nonFungibleDetails = resourcesDetails.find {
                            it.address == nonFungibleResourcesItem.resourceAddress
                        }

                        val resourceBehaviours = nonFungibleDetails?.details?.calculateResourceBehaviours().orEmpty()
                        val currentSupply = nonFungibleDetails?.details?.totalSupply()?.toIntOrNull()

                        val metaDataItems = nonFungibleDetails?.explicitMetadata?.asMetadataItems().orEmpty().toMutableList()

                        val nftItems = if (isNftItemDataNeeded) {
                            getNonFungibleDataForAccount(
                                accountAddress = entityItem.address,
                                vaultAddress = nonFungibleResourcesItem.vaults.items.first().vaultAddress,
                                resourceAddress = nonFungibleResourcesItem.resourceAddress,
                                stateVersion = stateVersion,
                                isRefreshing = isRefreshing,
                            ).value().orEmpty().sorted()
                        } else {
                            getNonFungibleIdsForAccount(
                                accountAddress = entityItem.address,
                                vaultAddress = nonFungibleResourcesItem.vaults.items.first().vaultAddress,
                                resourceAddress = nonFungibleResourcesItem.resourceAddress,
                                stateVersion = stateVersion,
                                isRefreshing = isRefreshing
                            ).first.map {
                                Resource.NonFungibleResource.Item(
                                    collectionAddress = nonFungibleResourcesItem.resourceAddress,
                                    localId = Resource.NonFungibleResource.Item.ID.from(it)
                                )
                            }
                        }

                        if (nftItems.isEmpty()) return@mapNotNull null

                        Resource.NonFungibleResource(
                            resourceAddress = nonFungibleResourcesItem.resourceAddress,
                            amount = nonFungibleResourcesItem.vaults.items.first().totalCount,
                            nameMetadataItem = metaDataItems.consume(),
                            descriptionMetadataItem = metaDataItems.consume(),
                            iconMetadataItem = metaDataItems.consume(),
                            tagsMetadataItem = metaDataItems.consume(),
                            behaviours = resourceBehaviours,
                            items = nftItems,
                            currentSupply = currentSupply,
                            validatorMetadataItem = metaDataItems.consume()
                        )
                    }
                }
        }.flatMap { map ->
            map.asSequence()
        }.associate { map ->
            map.key to map.value
        }
    }

    private suspend fun getStateEntityDetailsResponse(
        addresses: Collection<String>,
        explicitMetadata: Set<ExplicitMetadataKey>,
        isRefreshing: Boolean,
        stateVersion: Long? = null
    ): Result<List<StateEntityDetailsResponse>> {
        val responses = addresses
            .chunked(CHUNK_SIZE_OF_ITEMS)
            .map { chunkedAddresses ->
                stateApi.entityDetails(
                    StateEntityDetailsRequest(
                        addresses = chunkedAddresses,
                        aggregationLevel = ResourceAggregationLevel.vault,
                        optIns = StateEntityDetailsOptIns(
                            explicitMetadata = explicitMetadata.map { it.key }
                        ),
                        atLedgerState = stateVersion?.let { LedgerStateSelector(stateVersion = it) }
                    )
                ).execute(
                    cacheParameters = CacheParameters(
                        httpCache = cache,
                        timeoutDuration = if (isRefreshing) NO_CACHE else TimeoutDuration.FIVE_MINUTES
                    ),
                    map = {
                        it
                    }
                )
            }

        // if you find any error response in the list of StateEntityDetailsResponses then return error
        return if (responses.any { response -> response is Result.Error }) {
            val errorResponse = responses.first { response -> response is Result.Error }.map {
                listOf(it)
            }
            Timber.e("Found errors when requesting entity details")
            errorResponse
        } else { // otherwise all StateEntityDetailsResponses are success so return the list
            Result.Success(responses.mapNotNull { it.value() })
        }
    }

    private suspend fun getFungibleResourcesCollectionItemsForAccount(
        accountAddress: String,
        fungibleResources: FungibleResourcesCollection,
        stateVersion: Long?
    ): List<FungibleResourcesCollectionItemVaultAggregated> {
        val allFungibles: MutableList<FungibleResourcesCollectionItemVaultAggregated> = mutableListOf()
        allFungibles.addAll(
            fungibleResources.items.map {
                it as FungibleResourcesCollectionItemVaultAggregated
            }
        )

        var nextCursor = fungibleResources.nextCursor
        while (nextCursor != null) {
            val stateEntityFungiblesPageResponse = nextFungiblesPage(
                accountAddress = accountAddress,
                nextCursor = nextCursor,
                stateVersion = stateVersion
            )
            stateEntityFungiblesPageResponse.map {
                allFungibles.addAll(
                    it.items.map { fungibleResourcesCollectionItem ->
                        fungibleResourcesCollectionItem as FungibleResourcesCollectionItemVaultAggregated
                    }
                )
                nextCursor = it.nextCursor
            }
        }
        return allFungibles
    }

    private suspend fun getNonFungibleResourcesCollectionItemsForAccount(
        accountAddress: String,
        nonFungibleResources: NonFungibleResourcesCollection,
        stateVersion: Long?
    ): List<NonFungibleResourcesCollectionItemVaultAggregated> {
        val allNonFungibles: MutableList<NonFungibleResourcesCollectionItemVaultAggregated> = mutableListOf()
        allNonFungibles.addAll(
            nonFungibleResources.items.map {
                it as NonFungibleResourcesCollectionItemVaultAggregated
            }
        )
        var nextCursor = nonFungibleResources.nextCursor
        while (nextCursor != null) {
            val stateEntityFungiblesPageResponse = nextNonFungiblesPage(
                accountAddress = accountAddress,
                nextCursor = nextCursor,
                stateVersion = stateVersion
            )
            stateEntityFungiblesPageResponse.map {
                allNonFungibles.addAll(
                    it.items.map { nonFungibleResourcesCollectionItem ->
                        nonFungibleResourcesCollectionItem as NonFungibleResourcesCollectionItemVaultAggregated
                    }
                )
                nextCursor = it.nextCursor
            }
        }
        return allNonFungibles
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private suspend fun getNonFungibleDataForAccount(
        accountAddress: String,
        vaultAddress: String,
        resourceAddress: String,
        stateVersion: Long?,
        isRefreshing: Boolean = false
    ): Result<List<Resource.NonFungibleResource.Item>> {
        val nftIdsForAccount = getNonFungibleIdsForAccount(
            accountAddress = accountAddress,
            vaultAddress = vaultAddress,
            resourceAddress = resourceAddress,
            stateVersion = stateVersion,
            isRefreshing = isRefreshing
        )
        val nftIds = nftIdsForAccount.first
        val latestEpoch = nftIdsForAccount.second

        val nonFungibleDataResponsesListResult = nftIds.chunked(CHUNK_SIZE_OF_ITEMS).map { ids ->
            stateApi.nonFungibleData(
                StateNonFungibleDataRequest(
                    resourceAddress = resourceAddress,
                    nonFungibleIds = ids,
                    atLedgerState = stateVersion?.let { LedgerStateSelector(stateVersion = it) }
                )
            ).execute(
                cacheParameters = CacheParameters(
                    httpCache = cache,
                    timeoutDuration = if (isRefreshing) NO_CACHE else TimeoutDuration.FIVE_MINUTES
                ),
                map = {
                    it
                }
            )
        }

        // if you find any error response in the list of StateNonFungibleDataResponse then return error
        return if (nonFungibleDataResponsesListResult.any { response -> response is Result.Error }) {
            Timber.e("Found errors when requesting NonFungibleIds")
            Result.Error(IOException("Failed to fetch the nonFungibleData"))
        } else {
            val nonFungibleResourceItemsList =
                nonFungibleDataResponsesListResult.mapNotNull { nonFungibleDataResponse ->
                    nonFungibleDataResponse.map {
                        it.nonFungibleIds.map { stateNonFungibleDetailsResponseItem ->
                            val claimEpoch = stateNonFungibleDetailsResponseItem.claimEpoch()?.toLong()
                            val ledgerEpoch = latestEpoch
                            Resource.NonFungibleResource.Item(
                                collectionAddress = resourceAddress,
                                localId = Resource.NonFungibleResource.Item.ID.from(
                                    stateNonFungibleDetailsResponseItem.nonFungibleId
                                ),
                                nameMetadataItem = stateNonFungibleDetailsResponseItem.data
                                    ?.programmaticJson?.fields?.find { field ->
                                        field.field_name == ExplicitMetadataKey.NAME.key
                                    }?.value?.let { name -> NameMetadataItem(name = name) },
                                iconMetadataItem = stateNonFungibleDetailsResponseItem.data
                                    ?.programmaticJson?.fields?.find { field ->
                                        field.field_name == ExplicitMetadataKey.KEY_IMAGE_URL.key
                                    }?.value?.let { imageUrl -> IconUrlMetadataItem(url = Uri.parse(imageUrl)) },
                                readyToClaim = claimEpoch != null && ledgerEpoch != null && ledgerEpoch >= claimEpoch,
                                claimAmountMetadataItem = stateNonFungibleDetailsResponseItem.claimAmount()
                                    ?.let { claimAmount -> ClaimAmountMetadataItem(claimAmount) },
                                remainingMetadata = stateNonFungibleDetailsResponseItem.data?.programmaticJson?.fields
                                    ?.filterNot { field ->
                                        field.field_name == ExplicitMetadataKey.NAME.key ||
                                            field.field_name == ExplicitMetadataKey.KEY_IMAGE_URL.key
                                    }?.mapNotNull { field ->
                                        val fieldName = field.field_name.orEmpty()
                                        val value = field.valueContent.orEmpty()
                                        if (fieldName.isNotEmpty() && value.isNotBlank()) {
                                            StringMetadataItem(fieldName, value)
                                        } else {
                                            null
                                        }
                                    }.orEmpty()
                            )
                        }
                    }.value()
                }.flatten()
            Result.Success(nonFungibleResourceItemsList)
        }
    }

    private suspend fun getNonFungibleIdsForAccount(
        accountAddress: String,
        vaultAddress: String,
        resourceAddress: String,
        stateVersion: Long?,
        isRefreshing: Boolean = false
    ): Pair<List<String>, Long?> {
        val nftIds = mutableListOf<String>()
        var nextIdsCursor: String? = null
        var latestEpoch: Long? = null
        do {
            stateApi.entityNonFungibleIdsPage(
                StateEntityNonFungibleIdsPageRequest(
                    address = accountAddress,
                    vaultAddress = vaultAddress,
                    resourceAddress = resourceAddress,
                    cursor = nextIdsCursor,
                    atLedgerState = stateVersion?.let { LedgerStateSelector(stateVersion = it) }
                )
            ).execute(
                cacheParameters = CacheParameters(
                    httpCache = cache,
                    timeoutDuration = if (isRefreshing) NO_CACHE else TimeoutDuration.FIVE_MINUTES
                ),
                map = { it }
            ).onValue { page ->
                nftIds.addAll(page.items)
                nextIdsCursor = page.nextCursor
                latestEpoch = page.ledgerState.epoch
            }
        } while (nextIdsCursor != null)

        return Pair(nftIds, latestEpoch)
    }

    private fun StateNonFungibleDetailsResponseItem.claimAmount(): String? = data?.programmaticJson?.fields?.find { element ->
        element.kind == MetadataValueType.decimal.value
    }?.value

    private fun StateNonFungibleDetailsResponseItem.claimEpoch(): String? = data?.programmaticJson?.fields?.find { element ->
        element.kind == MetadataValueType.u64.value
    }?.value

    private suspend fun nextFungiblesPage(
        accountAddress: String,
        nextCursor: String,
        stateVersion: Long?
    ): Result<StateEntityFungiblesPageResponse> {
        return stateApi.entityFungiblesPage(
            stateEntityFungiblesPageRequest = StateEntityFungiblesPageRequest(
                address = accountAddress,
                cursor = nextCursor,
                aggregationLevel = ResourceAggregationLevel.vault,
                atLedgerState = stateVersion?.let { LedgerStateSelector(stateVersion = it) }
            )
        ).execute(
            cacheParameters = CacheParameters(
                httpCache = cache,
                timeoutDuration = NO_CACHE
            ),
            map = { it }
        )
    }

    private suspend fun nextNonFungiblesPage(
        accountAddress: String,
        nextCursor: String,
        stateVersion: Long?
    ): Result<StateEntityNonFungiblesPageResponse> {
        return stateApi.entityNonFungiblesPage(
            stateEntityNonFungiblesPageRequest = StateEntityNonFungiblesPageRequest(
                address = accountAddress,
                cursor = nextCursor,
                aggregationLevel = ResourceAggregationLevel.vault,
                atLedgerState = stateVersion?.let { LedgerStateSelector(stateVersion = it) }
            )
        ).execute(
            cacheParameters = CacheParameters(
                httpCache = cache,
                timeoutDuration = NO_CACHE
            ),
            map = { it }
        )
    }

    override suspend fun getEntityOwnerKeyHashes(
        entityAddress: String,
        isRefreshing: Boolean
    ): Result<OwnerKeyHashesMetadataItem?> {
        return stateApi.entityDetails(
            StateEntityDetailsRequest(
                addresses = listOf(entityAddress),
                optIns = StateEntityDetailsOptIns(
                    explicitMetadata = ExplicitMetadataKey.forEntities.map { it.key }
                )
            )
        ).execute(
            cacheParameters = CacheParameters(
                httpCache = cache,
                timeoutDuration = if (isRefreshing) NO_CACHE else TimeoutDuration.FIVE_MINUTES
            ),
            map = { response ->
                response.items.first()
                    .explicitMetadata
                    ?.asMetadataItems()
                    ?.filterIsInstance<OwnerKeyHashesMetadataItem>()
                    ?.firstOrNull()
            },
        )
    }

    private suspend fun getDetailsForResources(
        resourceAddresses: List<String>,
        stateVersion: Long? = null
    ): List<StateEntityDetailsResponseItem> = getStateEntityDetailsResponse(
        addresses = resourceAddresses,
        explicitMetadata = ExplicitMetadataKey.forResources,
        isRefreshing = false,
        stateVersion = stateVersion
    ).value().orEmpty().map {
        it.items
    }.flatten()

    companion object {
        const val CHUNK_SIZE_OF_ITEMS = 20
    }
}
