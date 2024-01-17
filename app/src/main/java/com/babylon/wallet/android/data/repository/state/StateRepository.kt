package com.babylon.wallet.android.data.repository.state

import com.babylon.wallet.android.data.gateway.apis.StateApi
import com.babylon.wallet.android.data.gateway.extensions.fetchPools
import com.babylon.wallet.android.data.gateway.extensions.fetchValidators
import com.babylon.wallet.android.data.gateway.extensions.getNextNftItems
import com.babylon.wallet.android.data.gateway.extensions.paginateDetails
import com.babylon.wallet.android.data.gateway.extensions.toMetadata
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleDataRequest
import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.data.repository.cache.database.DAppEntity
import com.babylon.wallet.android.data.repository.cache.database.MetadataColumn
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
import com.babylon.wallet.android.data.repository.toResult
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.model.DApp
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.domain.model.assets.LiquidStakeUnit
import com.babylon.wallet.android.domain.model.assets.ValidatorDetail
import com.babylon.wallet.android.domain.model.assets.ValidatorWithStakes
import com.babylon.wallet.android.domain.model.resources.Pool
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.metadata.PublicKeyHash
import com.babylon.wallet.android.domain.model.resources.metadata.ownerKeyHashes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import rdx.works.core.InstantGenerator
import rdx.works.profile.data.model.pernetwork.Entity
import rdx.works.profile.data.model.pernetwork.Network
import java.math.BigDecimal
import javax.inject.Inject

@Suppress("TooManyFunctions")
interface StateRepository {

    fun observeAccountsOnLedger(accounts: List<Network.Account>, isRefreshing: Boolean): Flow<List<AccountWithAssets>>

    suspend fun getNextNFTsPage(account: Network.Account, resource: Resource.NonFungibleResource): Result<Resource.NonFungibleResource>

    suspend fun updateLSUsInfo(account: Network.Account, validatorsWithStakes: List<ValidatorWithStakes>): Result<List<ValidatorWithStakes>>

    suspend fun getResources(addresses: Set<String>, underAccountAddress: String?, withDetails: Boolean): Result<List<Resource>>

    suspend fun getPools(poolAddresses: Set<String>): Result<List<Pool>>

    suspend fun getValidator(validatorAddress: String): Result<ValidatorDetail>

    suspend fun getValidators(validatorAddresses: Set<String>): Result<List<ValidatorDetail>>

    suspend fun getNFTDetails(resourceAddress: String, localIds: Set<String>): Result<List<Resource.NonFungibleResource.Item>>

    suspend fun getOwnedXRD(accounts: List<Network.Account>): Result<Map<Network.Account, BigDecimal>>

    suspend fun getEntityOwnerKeys(entities: List<Entity>): Result<Map<Entity, List<PublicKeyHash>>>

    suspend fun getDAppsDetails(definitionAddresses: List<String>, skipCache: Boolean): Result<List<DApp>>

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
    private val accountsStateCache: AccountsStateCache
) : StateRepository {

    override fun observeAccountsOnLedger(
        accounts: List<Network.Account>,
        isRefreshing: Boolean
    ): Flow<List<AccountWithAssets>> = accountsStateCache.observeAccountsOnLedger(
        accounts = accounts,
        isRefreshing = isRefreshing
    )

    override suspend fun getNextNFTsPage(
        account: Network.Account,
        resource: Resource.NonFungibleResource
    ): Result<Resource.NonFungibleResource> = withContext(dispatcher) {
        runCatching {
            // No more pages to return
            if (resource.amount.toInt() == resource.items.size) throw StateRepository.Error.NoMorePages

            val accountStateVersion = stateDao.getAccountStateVersion(accountAddress = account.address)
                ?: throw StateRepository.Error.StateVersionMissing

            val accountResourceJoin = stateDao.getAccountResourceJoin(
                accountAddress = account.address,
                resourceAddress = resource.resourceAddress
            )

            val cachedNFTItems = stateDao.getOwnedNfts(
                accountAddress = account.address,
                resourceAddress = resource.resourceAddress,
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
                resourceAddress = resource.resourceAddress,
                vaultAddress = vaultAddress,
                nextCursor = nextCursor,
                stateVersion = accountStateVersion
            )
            val syncInfo = SyncInfo(synced = InstantGenerator(), accountStateVersion = accountStateVersion)

            val newItems = stateDao.storeAccountNFTsPortfolio(
                accountAddress = account.address,
                resourceAddress = resource.resourceAddress,
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
        account: Network.Account,
        validatorsWithStakes: List<ValidatorWithStakes>
    ) = withContext(dispatcher) {
        runCatching {
            val stateVersion = stateDao.getAccountStateVersion(account.address) ?: throw StateRepository.Error.StateVersionMissing

            var result = validatorsWithStakes

            val lsuEntities = mutableMapOf<String, ResourceEntity>()
            stateApi.paginateDetails(
                addresses = result
                    .filter { it.liquidStakeUnit != null && it.liquidStakeUnit.fungibleResource.isDetailsAvailable.not() }
                    .map { it.liquidStakeUnit!!.resourceAddress }
                    .toSet(),
                metadataKeys = ExplicitMetadataKey.forAssets,
                onPage = { response ->
                    val synced = InstantGenerator()
                    val newLSUs = response.items.map { it.asEntity(synced) }.associateBy { it.address }
                    lsuEntities.putAll(newLSUs)
                }
            )

            result = result.map { item ->
                item.copy(
                    liquidStakeUnit = if (item.liquidStakeUnit != null && item.liquidStakeUnit.fungibleResource.isDetailsAvailable.not()) {
                        val newLsu = lsuEntities[item.liquidStakeUnit.resourceAddress]?.toResource(
                            item.liquidStakeUnit.fungibleResource.ownedAmount
                        ) as? Resource.FungibleResource

                        if (newLsu != null) {
                            LiquidStakeUnit(newLsu)
                        } else {
                            item.liquidStakeUnit
                        }
                    } else {
                        item.liquidStakeUnit
                    }
                )
            }

            val claims = result.map { validatorWithStakes ->
                val stakeClaimCollection = validatorWithStakes.stakeClaimNft?.nonFungibleResource
                if (stakeClaimCollection != null && stakeClaimCollection.amount.toInt() != stakeClaimCollection.items.size) {
                    val resourcesInAccount = stateDao.getAccountResourceJoin(
                        resourceAddress = stakeClaimCollection.resourceAddress,
                        accountAddress = account.address
                    )
                    if (resourcesInAccount?.vaultAddress != null) {
                        val nfts = stateApi.getNextNftItems(
                            accountAddress = account.address,
                            resourceAddress = stakeClaimCollection.resourceAddress,
                            vaultAddress = resourcesInAccount.vaultAddress,
                            nextCursor = null,
                            stateVersion = stateVersion
                        ).second

                        val syncedAt = InstantGenerator()
                        nfts.map { it.asEntity(stakeClaimCollection.resourceAddress, syncedAt) }
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

    override suspend fun getResources(
        addresses: Set<String>,
        underAccountAddress: String?,
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
                    addresses = resourcesToFetch.toSet(),
                    metadataKeys = ExplicitMetadataKey.forAssets,
                    onPage = { page ->
                        page.items.forEach { item ->
                            val amount = underAccountAddress?.let { accountAddress ->
                                stateDao.getAccountResourceJoin(resourceAddress = item.address, accountAddress = accountAddress)?.amount
                            }
                            val updatedEntity = stateDao.updateResourceDetails(item)
                            val resource = updatedEntity.toResource(amount)

                            addressesWithResources[resource.resourceAddress] = resource
                        }
                    }
                )
            }

            addressesWithResources.values.filterNotNull()
        }
    }

    override suspend fun getPools(poolAddresses: Set<String>): Result<List<Pool>> = withContext(dispatcher) {
        runCatching {
            val stateVersion = stateDao.getLatestStateVersion() ?: error("No cached state version found")
            var cachedPools = stateDao.getCachedPools(
                poolAddresses = poolAddresses,
                atStateVersion = stateVersion
            ).values.toList()
            val unknownPools = poolAddresses - cachedPools.map { it.address }.toSet()
            if (unknownPools.isNotEmpty()) {
                val newPools = stateApi.fetchPools(unknownPools, stateVersion)
                if (newPools.isNotEmpty()) {
                    val join = newPools.asPoolsResourcesJoin(SyncInfo(InstantGenerator(), stateVersion))
                    stateDao.updatePools(pools = join)
                    cachedPools = stateDao.getCachedPools(
                        poolAddresses = poolAddresses,
                        atStateVersion = stateVersion
                    ).values.toList()
                }
            }
            cachedPools
        }
    }

    override suspend fun getValidator(validatorAddress: String): Result<ValidatorDetail> = getValidators(setOf(validatorAddress)).map {
        it.first()
    }

    override suspend fun getValidators(validatorAddresses: Set<String>): Result<List<ValidatorDetail>> = withContext(dispatcher) {
        runCatching {
            val stateVersion = stateDao.getLatestStateVersion() ?: error("No cached state version found")
            val validators = stateDao.getValidators(addresses = validatorAddresses.toSet(), atStateVersion = stateVersion)
            val unknownAddresses = validatorAddresses - validators.map { it.address }.toSet()
            if (unknownAddresses.isNotEmpty()) {
                val details = stateApi.fetchValidators(
                    validatorsAddresses = unknownAddresses.toSet(),
                    stateVersion = stateVersion
                ).asValidators()

                stateDao.insertValidators(details.map { it.asValidatorEntity(SyncInfo(InstantGenerator(), stateVersion)) })
                details + validators.map { it.asValidatorDetail() }
            } else {
                validators.map { it.asValidatorDetail() }
            }
        }
    }

    override suspend fun getNFTDetails(
        resourceAddress: String,
        localIds: Set<String>
    ): Result<List<Resource.NonFungibleResource.Item>> = withContext(dispatcher) {
        val cachedItems = stateDao.getNFTDetails(resourceAddress, localIds, resourcesCacheValidity())

        if (cachedItems != null && cachedItems.size == localIds.size) {
            return@withContext Result.success(cachedItems.map { it.toItem() })
        }
        val unknownIds = localIds - cachedItems?.map { it.localId }.orEmpty().toSet()

        stateApi.nonFungibleData(
            StateNonFungibleDataRequest(
                resourceAddress = resourceAddress,
                nonFungibleIds = unknownIds.toList()
            )
        ).toResult().mapCatching { response ->
            val item = response.nonFungibleIds
            val entities = item.map { it.asEntity(resourceAddress, InstantGenerator()) }
            stateDao.insertNFTs(entities)
            cachedItems.orEmpty().map { it.toItem() } + entities.map { it.toItem() }
        }
    }

    override suspend fun getOwnedXRD(accounts: List<Network.Account>): Result<Map<Network.Account, BigDecimal>> =
        accountsStateCache.getOwnedXRD(accounts = accounts)

    override suspend fun getEntityOwnerKeys(entities: List<Entity>): Result<Map<Entity, List<PublicKeyHash>>> = runCatching {
        if (entities.isEmpty()) return@runCatching mapOf()

        val entitiesWithOwnerKeys = mutableMapOf<Entity, List<PublicKeyHash>>()
        stateApi.paginateDetails(
            addresses = entities.map { it.address }.toSet(),
            metadataKeys = setOf(ExplicitMetadataKey.OWNER_KEYS),
        ) { page ->
            page.items.forEach { item ->
                val publicKeys = item.explicitMetadata?.toMetadata()?.ownerKeyHashes() ?: return@forEach
                val entity = entities.find { item.address == it.address } ?: return@forEach
                entitiesWithOwnerKeys[entity] = publicKeys.keys
            }
        }
        entitiesWithOwnerKeys
    }

    override suspend fun getDAppsDetails(
        definitionAddresses: List<String>,
        skipCache: Boolean
    ): Result<List<DApp>> = withContext(dispatcher) {
        runCatching {
            if (definitionAddresses.isEmpty()) return@runCatching listOf()

            val cachedDApps = if (skipCache) {
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
                    addresses = definitionAddresses.toSet(),
                    metadataKeys = ExplicitMetadataKey.forDApps
                ) { page ->
                    val syncedAt = InstantGenerator()
                    val entities = page.items.map { item ->
                        DAppEntity(
                            definitionAddress = item.address,
                            metadata = item.explicitMetadata?.toMetadata()?.let { MetadataColumn(it) },
                            synced = syncedAt
                        )
                    }
                    stateDao.insertDApps(entities)

                    cachedDApps.addAll(entities.map { entity -> entity.toDApp() })
                }
            }

            cachedDApps
        }
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
}
