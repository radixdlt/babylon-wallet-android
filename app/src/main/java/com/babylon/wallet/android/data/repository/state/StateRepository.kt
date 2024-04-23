package com.babylon.wallet.android.data.repository.state

import com.babylon.wallet.android.data.gateway.apis.StateApi
import com.babylon.wallet.android.data.gateway.extensions.fetchPools
import com.babylon.wallet.android.data.gateway.extensions.fetchValidators
import com.babylon.wallet.android.data.gateway.extensions.getNextNftItems
import com.babylon.wallet.android.data.gateway.extensions.paginateDetails
import com.babylon.wallet.android.data.gateway.extensions.paginateNonFungibles
import com.babylon.wallet.android.data.gateway.extensions.toMetadata
import com.babylon.wallet.android.data.repository.cache.database.DAppEntity
import com.babylon.wallet.android.data.repository.cache.database.NFTEntity.Companion.asEntity
import com.babylon.wallet.android.data.repository.cache.database.PoolEntity.Companion.asPoolsResourcesJoin
import com.babylon.wallet.android.data.repository.cache.database.ResourceEntity
import com.babylon.wallet.android.data.repository.cache.database.ResourceEntity.Companion.asEntity
import com.babylon.wallet.android.data.repository.cache.database.StateDao
import com.babylon.wallet.android.data.repository.cache.database.StateDao.Companion.dAppsCacheValidity
import com.babylon.wallet.android.data.repository.cache.database.StateDao.Companion.resourcesCacheValidity
import com.babylon.wallet.android.data.repository.cache.database.SyncInfo
import com.babylon.wallet.android.data.repository.cache.database.ValidatorEntity.Companion.asValidatorEntity
import com.babylon.wallet.android.data.repository.cache.database.ValidatorEntity.Companion.asValidators
import com.babylon.wallet.android.data.repository.cache.database.getCachedPools
import com.babylon.wallet.android.data.repository.cache.database.storeAccountNFTsPortfolio
import com.babylon.wallet.android.data.repository.cache.database.updateResourceDetails
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.ComponentAddress
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.NonFungibleLocalId
import com.radixdlt.sargon.PoolAddress
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.ValidatorAddress
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.networkId
import com.radixdlt.sargon.extensions.string
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import rdx.works.core.InstantGenerator
import rdx.works.core.domain.DApp
import rdx.works.core.domain.assets.LiquidStakeUnit
import rdx.works.core.domain.assets.StakeClaim
import rdx.works.core.domain.assets.ValidatorWithStakes
import rdx.works.core.domain.resources.ExplicitMetadataKey
import rdx.works.core.domain.resources.Pool
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.Validator
import rdx.works.core.domain.resources.metadata.PublicKeyHash
import rdx.works.core.domain.resources.metadata.dAppDefinition
import rdx.works.core.domain.resources.metadata.ownerKeyHashes
import rdx.works.core.sargon.ProfileEntity
import rdx.works.core.sargon.currentGateway
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@Suppress("TooManyFunctions")
interface StateRepository {

    fun observeAccountsOnLedger(accounts: List<Account>, isRefreshing: Boolean): Flow<List<AccountWithAssets>>

    suspend fun getNextNFTsPage(account: Account, resource: Resource.NonFungibleResource): Result<Resource.NonFungibleResource>

    suspend fun updateLSUsInfo(account: Account, validatorsWithStakes: List<ValidatorWithStakes>): Result<List<ValidatorWithStakes>>

    suspend fun updateStakeClaims(account: Account, claims: List<StakeClaim>): Result<List<StakeClaim>>

    suspend fun getResources(
        addresses: Set<ResourceAddress>,
        underAccountAddress: AccountAddress?,
        withDetails: Boolean
    ): Result<List<Resource>>

    suspend fun getPools(poolAddresses: Set<PoolAddress>): Result<List<Pool>>

    suspend fun getValidators(validatorAddresses: Set<ValidatorAddress>): Result<List<Validator>>

    suspend fun getNFTDetails(
        resourceAddress: ResourceAddress,
        localIds: Set<NonFungibleLocalId>
    ): Result<List<Resource.NonFungibleResource.Item>>

    suspend fun getOwnedXRD(accounts: List<Account>): Result<Map<Account, Decimal192>>

    suspend fun getEntityOwnerKeys(entities: List<ProfileEntity>): Result<Map<ProfileEntity, List<PublicKeyHash>>>

    suspend fun getDAppsDetails(definitionAddresses: List<AccountAddress>, isRefreshing: Boolean): Result<List<DApp>>

    suspend fun getDAppDefinitions(componentAddresses: List<ComponentAddress>): Result<Map<ComponentAddress, AccountAddress?>>

    suspend fun cacheNewlyCreatedResources(newResources: List<Resource>): Result<Unit>

    suspend fun clearCachedState(): Result<Unit>

    sealed class Error(cause: Throwable) : Exception(cause) {
        data object NoMorePages : Error(RuntimeException("No more NFTs for this resource."))

        data object VaultAddressMissing : Error(RuntimeException("No vault address to fetch NFTs"))

        data object StateVersionMissing : Error(RuntimeException("State version missing for account."))
    }
}

@Suppress("TooManyFunctions")
class StateRepositoryImpl @Inject constructor(
    private val stateApi: StateApi,
    private val stateDao: StateDao,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
    private val accountsStateCache: AccountsStateCache,
    private val getProfileUseCase: GetProfileUseCase
) : StateRepository {

    override fun observeAccountsOnLedger(
        accounts: List<Account>,
        isRefreshing: Boolean
    ): Flow<List<AccountWithAssets>> = accountsStateCache.observeAccountsOnLedger(
        accounts = accounts,
        isRefreshing = isRefreshing
    )

    override suspend fun getNextNFTsPage(
        account: Account,
        resource: Resource.NonFungibleResource
    ): Result<Resource.NonFungibleResource> = withContext(dispatcher) {
        runCatching {
            // No more pages to return
            if (resource.amount.toInt() == resource.items.size) throw StateRepository.Error.NoMorePages

            val accountStateVersion = stateDao.getAccountStateVersion(accountAddress = account.address)
                ?: throw StateRepository.Error.StateVersionMissing

            val accountResourceJoin = stateDao.getAccountResourceJoin(
                accountAddress = account.address,
                resourceAddress = resource.address
            )

            val cachedNFTItems = stateDao.getOwnedNfts(
                accountAddress = account.address,
                resourceAddress = resource.address,
                stateVersion = accountStateVersion
            )

            // All items cached, return the result
            if (cachedNFTItems.size == resource.amount.toInt()) {
                return@runCatching resource.copy(items = cachedNFTItems.map { it.toItem() }.sorted())
            }

            val vaultAddress = accountResourceJoin?.vaultAddress ?: throw StateRepository.Error.VaultAddressMissing
            val nextCursor = accountResourceJoin.nextCursor

            val page = stateApi.getNextNftItems(
                accountAddress = account.address,
                resourceAddress = resource.address,
                vaultAddress = vaultAddress,
                nextCursor = nextCursor,
                stateVersion = accountStateVersion
            )
            val syncInfo = SyncInfo(synced = InstantGenerator(), accountStateVersion = accountStateVersion)

            val newItems = stateDao.storeAccountNFTsPortfolio(
                accountAddress = account.address,
                resourceAddress = resource.address,
                nextCursor = page.first,
                items = page.second,
                syncInfo = syncInfo
            )
            val currentItems = resource.items
            val allNewItems = (currentItems + newItems).distinctBy { it.localId }.sorted()

            resource.copy(items = allNewItems)
        }
    }

    @Suppress("LongMethod", "UnsafeCallOnNullableType")
    override suspend fun updateLSUsInfo(
        account: Account,
        validatorsWithStakes: List<ValidatorWithStakes>
    ) = withContext(dispatcher) {
        runCatching {
            val stateVersion = stateDao.getAccountStateVersion(account.address) ?: throw StateRepository.Error.StateVersionMissing

            var result = validatorsWithStakes

            val lsuEntities = mutableMapOf<ResourceAddress, ResourceEntity>()
            val lsuAddresses = result
                .filter {
                    val lsu = it.liquidStakeUnit
                    lsu != null && lsu.fungibleResource.isDetailsAvailable.not()
                }
                .map { it.liquidStakeUnit!!.resourceAddress }
                .toSet()
            if (lsuAddresses.isNotEmpty()) {
                stateApi.paginateDetails(
                    addresses = lsuAddresses.map { it.string }.toSet(),
                    metadataKeys = ExplicitMetadataKey.forAssets,
                    onPage = { response ->
                        val synced = InstantGenerator()
                        val newLSUs = response.items.map { it.asEntity(synced) }.associateBy { it.address }
                        lsuEntities.putAll(newLSUs)
                    }
                )

                result = result.map { item ->
                    val lsu = item.liquidStakeUnit
                    item.copy(
                        liquidStakeUnit = if (lsu != null && !lsu.fungibleResource.isDetailsAvailable) {
                            val newLsu = lsuEntities[lsu.resourceAddress]?.toResource(
                                lsu.fungibleResource.ownedAmount
                            ) as? Resource.FungibleResource

                            if (newLsu != null) {
                                LiquidStakeUnit(newLsu, item.validator)
                            } else {
                                item.liquidStakeUnit
                            }
                        } else {
                            item.liquidStakeUnit
                        }
                    )
                }
            }

            val claims = result.map { validatorWithStakes ->
                val stakeClaimCollection = validatorWithStakes.stakeClaimNft?.nonFungibleResource
                if (stakeClaimCollection != null && stakeClaimCollection.amount.toInt() != stakeClaimCollection.items.size) {
                    val resourcesInAccount = stateDao.getAccountResourceJoin(
                        resourceAddress = stakeClaimCollection.address,
                        accountAddress = account.address
                    )
                    if (resourcesInAccount?.vaultAddress != null) {
                        val nfts = stateApi.getNextNftItems(
                            accountAddress = account.address,
                            resourceAddress = stakeClaimCollection.address,
                            vaultAddress = resourcesInAccount.vaultAddress,
                            nextCursor = null,
                            stateVersion = stateVersion
                        ).second

                        val syncedAt = InstantGenerator()
                        nfts.map { it.asEntity(stakeClaimCollection.address, syncedAt) }
                    } else {
                        emptyList()
                    }
                } else {
                    emptyList()
                }
            }.flatten()

            stateDao.storeStakeDetails(
                accountAddress = account.address,
                stateVersion = stateVersion,
                lsuList = lsuEntities.values.toList(),
                claims = claims
            )

            val allClaimCollections = claims.map { it.toItem() }.groupBy { it.collectionAddress }

            result.map { item ->
                item.copy(
                    stakeClaimNft = item.stakeClaimNft?.let { claimCollection ->
                        val claimNFTs = allClaimCollections[claimCollection.resourceAddress]
                        if (claimNFTs != null) {
                            claimCollection.copy(nonFungibleResource = claimCollection.nonFungibleResource.copy(items = claimNFTs))
                        } else {
                            claimCollection
                        }
                    }
                )
            }
        }
    }

    override suspend fun updateStakeClaims(account: Account, claims: List<StakeClaim>): Result<List<StakeClaim>> =
        withContext(dispatcher) {
            runCatching {
                val stateVersion = stateDao.getAccountStateVersion(account.address) ?: throw StateRepository.Error.StateVersionMissing

                claims.map { claim ->
                    val claimNFTs = if (claim.nonFungibleResource.amount > claim.nonFungibleResource.items.size) {
                        val resourcesInAccount = stateDao.getAccountResourceJoin(
                            resourceAddress = claim.resourceAddress,
                            accountAddress = account.address
                        )

                        if (resourcesInAccount?.vaultAddress != null) {
                            val nftsOnLedger = stateApi.getNextNftItems(
                                accountAddress = account.address,
                                resourceAddress = claim.resourceAddress,
                                vaultAddress = resourcesInAccount.vaultAddress,
                                nextCursor = null,
                                stateVersion = stateVersion
                            ).second

                            val syncedAt = InstantGenerator()
                            val nftEntities = nftsOnLedger.map { it.asEntity(claim.resourceAddress, syncedAt) }

                            stateDao.storeStakeClaims(
                                accountAddress = account.address,
                                stateVersion = stateVersion,
                                claims = nftEntities
                            )

                            nftEntities.map { it.toItem() }
                        } else {
                            claim.nonFungibleResource.items
                        }
                    } else {
                        claim.nonFungibleResource.items
                    }

                    claim.copy(nonFungibleResource = claim.nonFungibleResource.copy(items = claimNFTs))
                }
            }
        }

    override suspend fun getResources(
        addresses: Set<ResourceAddress>,
        underAccountAddress: AccountAddress?,
        withDetails: Boolean
    ): Result<List<Resource>> = withContext(dispatcher) {
        runCatching {
            val addressesWithResources = addresses.associateWith { address ->
                val cachedEntity = stateDao.getResourceDetails(
                    resourceAddress = address,
                    minValidity = resourcesCacheValidity()
                )

                val amount = underAccountAddress?.let { accountAddress ->
                    stateDao.getAccountResourceJoin(resourceAddress = address, accountAddress = accountAddress)?.amount
                }

                cachedEntity?.toResource(amount)
            }.toMutableMap()

            val resourcesToFetch = addressesWithResources.mapNotNull { entry ->
                val cachedResource = entry.value
                if (cachedResource == null || !cachedResource.isDetailsAvailable && withDetails) entry.key else null
            }
            if (resourcesToFetch.isNotEmpty()) {
                stateApi.paginateDetails(
                    addresses = resourcesToFetch.map { it.string }.toSet(),
                    metadataKeys = ExplicitMetadataKey.forAssets,
                    onPage = { page ->
                        page.items.forEach { item ->
                            val amount = underAccountAddress?.let { accountAddress ->
                                stateDao.getAccountResourceJoin(
                                    resourceAddress = ResourceAddress.init(item.address),
                                    accountAddress = accountAddress
                                )?.amount
                            }
                            val updatedEntity = stateDao.updateResourceDetails(item)
                            val resource = updatedEntity.toResource(amount)

                            addressesWithResources[resource.address] = resource
                        }
                    }
                )
            }

            addressesWithResources.values.filterNotNull()
        }
    }

    override suspend fun getPools(poolAddresses: Set<PoolAddress>): Result<List<Pool>> = withContext(dispatcher) {
        runCatching {
            val stateVersion = getLatestCachedStateVersionInNetwork()
            var cachedPools = if (stateVersion != null) {
                stateDao.getCachedPools(
                    poolAddresses = poolAddresses,
                    atStateVersion = stateVersion
                ).values.toList()
            } else {
                emptyList()
            }
            val unknownPools = poolAddresses - cachedPools.map { it.address }.toSet()
            if (unknownPools.isNotEmpty()) {
                val newPools = stateApi.fetchPools(unknownPools.toSet(), stateVersion)
                if (newPools.poolItems.isNotEmpty()) {
                    val fetchedStateVersion = requireNotNull(newPools.stateVersion)
                    val join = newPools.poolItems.asPoolsResourcesJoin(SyncInfo(InstantGenerator(), fetchedStateVersion))
                    stateDao.updatePools(pools = join)
                    cachedPools = stateDao.getCachedPools(
                        poolAddresses = poolAddresses,
                        atStateVersion = fetchedStateVersion
                    ).values.toList()
                }
            }
            cachedPools
        }
    }

    override suspend fun getValidators(validatorAddresses: Set<ValidatorAddress>): Result<List<Validator>> = withContext(dispatcher) {
        runCatching {
            val stateVersion = getLatestCachedStateVersionInNetwork()
            val cachedValidators = if (stateVersion != null) {
                stateDao.getValidators(addresses = validatorAddresses.toSet(), atStateVersion = stateVersion).map {
                    it.asValidatorDetail()
                }
            } else {
                emptyList()
            }

            val unknownAddresses = validatorAddresses - cachedValidators.map { it.address }.toSet()
            if (unknownAddresses.isNotEmpty()) {
                val response = stateApi.fetchValidators(
                    validatorsAddresses = unknownAddresses,
                    stateVersion = stateVersion
                )
                val details = response.validators.asValidators()
                if (details.isNotEmpty()) {
                    val syncInfo = SyncInfo(InstantGenerator(), requireNotNull(response.stateVersion))
                    stateDao.insertValidators(details.map { it.asValidatorEntity(syncInfo) })
                }
                details + cachedValidators
            } else {
                cachedValidators
            }
        }
    }

    override suspend fun getNFTDetails(
        resourceAddress: ResourceAddress,
        localIds: Set<NonFungibleLocalId>
    ): Result<List<Resource.NonFungibleResource.Item>> = withContext(dispatcher) {
        runCatching {
            val cachedItems = stateDao.getNFTDetails(resourceAddress, localIds, resourcesCacheValidity())

            if (cachedItems != null && cachedItems.size == localIds.size) {
                return@withContext Result.success(cachedItems.map { it.toItem() })
            }
            val unknownIds = localIds - cachedItems?.map { it.localId }.orEmpty().toSet()

            val result = mutableListOf<Resource.NonFungibleResource.Item>()
            stateApi.paginateNonFungibles(resourceAddress, nonFungibleIds = unknownIds, onPage = { response ->
                val item = response.nonFungibleIds
                val entities = item.map { it.asEntity(resourceAddress, InstantGenerator()) }
                stateDao.insertNFTs(entities)
                result.addAll(entities.map { it.toItem() })
            })
            result.toList()
        }
    }

    override suspend fun getOwnedXRD(accounts: List<Account>): Result<Map<Account, Decimal192>> =
        accountsStateCache.getOwnedXRD(accounts = accounts)

    override suspend fun getEntityOwnerKeys(entities: List<ProfileEntity>): Result<Map<ProfileEntity, List<PublicKeyHash>>> = runCatching {
        if (entities.isEmpty()) return@runCatching mapOf()

        val entitiesWithOwnerKeys = mutableMapOf<ProfileEntity, List<PublicKeyHash>>()
        stateApi.paginateDetails(
            addresses = entities.map { it.address.string }.toSet(),
            metadataKeys = setOf(ExplicitMetadataKey.OWNER_KEYS),
        ) { page ->
            page.items.forEach { item ->
                val publicKeys = item.explicitMetadata?.toMetadata()?.ownerKeyHashes() ?: return@forEach
                val entity = entities.find { item.address == it.address.string } ?: return@forEach
                entitiesWithOwnerKeys[entity] = publicKeys.keys
            }
        }
        entitiesWithOwnerKeys
    }

    override suspend fun getDAppsDetails(
        definitionAddresses: List<AccountAddress>,
        isRefreshing: Boolean
    ): Result<List<DApp>> = withContext(dispatcher) {
        runCatching {
            if (definitionAddresses.isEmpty()) return@runCatching listOf()

            val cachedDApps = if (isRefreshing) {
                mutableListOf()
            } else {
                stateDao.getDApps(
                    definitionAddresses = definitionAddresses,
                    minValidity = dAppsCacheValidity()
                ).map {
                    it.toDApp()
                }.toMutableList()
            }

            val remainingAddresses = definitionAddresses.toSet() subtract cachedDApps.map { it.dAppAddress }.toSet()
            if (remainingAddresses.isNotEmpty()) {
                stateApi.paginateDetails(
                    addresses = remainingAddresses.map { it.string }.toSet(),
                    metadataKeys = ExplicitMetadataKey.forDApps
                ) { page ->
                    val syncedAt = InstantGenerator()
                    val entities = page.items.map { item -> DAppEntity.from(item, syncedAt) }
                    stateDao.insertDApps(entities)

                    cachedDApps.addAll(entities.map { entity -> entity.toDApp() })
                }
            }

            cachedDApps
        }
    }

    override suspend fun getDAppDefinitions(componentAddresses: List<ComponentAddress>): Result<Map<ComponentAddress, AccountAddress?>> =
        runCatching {
            val result = mutableMapOf<ComponentAddress, AccountAddress?>()
            stateApi.paginateDetails(
                addresses = componentAddresses.map { it.string }.toSet(),
                metadataKeys = ExplicitMetadataKey.forDApps
            ) { page ->
                page.items.map { item ->
                    val componentAddress = ComponentAddress.init(item.address)
                    val dAppDefinitionAddress = item.explicitMetadata?.toMetadata()?.dAppDefinition()?.let { AccountAddress.init(it) }

                    result[componentAddress] = dAppDefinitionAddress
                }
            }
            result
        }

    override suspend fun cacheNewlyCreatedResources(newResources: List<Resource>) = withContext(dispatcher) {
        runCatching {
            val syncedAt = InstantGenerator()
            stateDao.insertOrReplaceResources(newResources.map { it.asEntity(syncedAt) })

            val newNFTs = newResources.filterIsInstance<Resource.NonFungibleResource>().map { it.items }.flatten()
            stateDao.insertNFTs(newNFTs.map { it.asEntity(syncedAt) })
        }
    }

    override suspend fun clearCachedState(): Result<Unit> = accountsStateCache.clear()

    private suspend fun getLatestCachedStateVersionInNetwork(): Long? {
        val currentNetworkId = getProfileUseCase().currentGateway.network.id

        return stateDao.getAccountStateVersions().filter {
            it.address.networkId == currentNetworkId
        }.maxByOrNull { it.stateVersion }?.stateVersion
    }
}
