package com.babylon.wallet.android.data.repository.state

import com.babylon.wallet.android.data.gateway.apis.StateApi
import com.babylon.wallet.android.data.gateway.extensions.fetchValidators
import com.babylon.wallet.android.data.gateway.extensions.getNextNftItems
import com.babylon.wallet.android.data.gateway.extensions.paginateDetails
import com.babylon.wallet.android.data.gateway.extensions.toMetadata
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleDataRequest
import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.data.repository.cache.database.DAppEntity
import com.babylon.wallet.android.data.repository.cache.database.MetadataColumn
import com.babylon.wallet.android.data.repository.cache.database.NFTEntity.Companion.asEntity
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

    suspend fun getPool(poolAddress: String): Result<Pool>

    suspend fun getValidator(validatorAddress: String): Result<ValidatorDetail>

    suspend fun getValidators(validatorAddresses: Set<String>): Result<List<ValidatorDetail>>

    suspend fun getNFTDetails(resourceAddress: String, localId: String): Result<Resource.NonFungibleResource.Item>

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
                metadataKeys = setOf(
                    ExplicitMetadataKey.NAME,
                    ExplicitMetadataKey.SYMBOL,
                    ExplicitMetadataKey.DESCRIPTION,
                    ExplicitMetadataKey.RELATED_WEBSITES,
                    ExplicitMetadataKey.ICON_URL,
                    ExplicitMetadataKey.INFO_URL,
                    ExplicitMetadataKey.VALIDATOR,
                    ExplicitMetadataKey.POOL,
                    ExplicitMetadataKey.TAGS,
                    ExplicitMetadataKey.DAPP_DEFINITIONS
                ),
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
                    metadataKeys = setOf(
                        ExplicitMetadataKey.NAME,
                        ExplicitMetadataKey.SYMBOL,
                        ExplicitMetadataKey.DESCRIPTION,
                        ExplicitMetadataKey.RELATED_WEBSITES,
                        ExplicitMetadataKey.ICON_URL,
                        ExplicitMetadataKey.INFO_URL,
                        ExplicitMetadataKey.VALIDATOR,
                        ExplicitMetadataKey.POOL,
                        ExplicitMetadataKey.TAGS,
                        ExplicitMetadataKey.DAPP_DEFINITIONS
                    ),
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

    override suspend fun getPool(poolAddress: String): Result<Pool> = withContext(dispatcher) {
        runCatching {
            val stateVersion = stateDao.getLatestStateVersion() ?: error("No cached state version found")
            stateDao.getCachedPools(
                poolAddresses = setOf(poolAddress),
                atStateVersion = stateVersion
            )[poolAddress] ?: error("Pool $poolAddress does not exist")
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
        localId: String
    ): Result<Resource.NonFungibleResource.Item> = withContext(dispatcher) {
        val cachedItem = stateDao.getNFTDetails(resourceAddress, localId, resourcesCacheValidity())

        if (cachedItem != null) {
            return@withContext Result.success(cachedItem.toItem())
        }

        stateApi.nonFungibleData(
            StateNonFungibleDataRequest(
                resourceAddress = resourceAddress,
                nonFungibleIds = listOf(localId)
            )
        ).toResult().mapCatching { response ->
            val item = response.nonFungibleIds.first()
            val entity = item.asEntity(resourceAddress, InstantGenerator())
            stateDao.insertNFTs(listOf(entity))
            entity.toItem()
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

    override suspend fun getDAppsDetails(definitionAddresses: List<String>, skipCache: Boolean): Result<List<DApp>> = withContext(
        dispatcher
    ) {
        runCatching {
            if (definitionAddresses.isEmpty()) return@runCatching listOf()

            val cachedDApps = if (!skipCache) {
                stateDao.getDApps(
                    definitionAddresses = definitionAddresses,
                    minValidity = dAppsCacheValidity()
                ).map {
                    it.toDApp()
                }.toMutableList()
            } else {
                mutableListOf()
            }

            val remainingAddresses = definitionAddresses.toSet() subtract cachedDApps.map { it.dAppAddress }.toSet()
            if (remainingAddresses.isNotEmpty()) {
                stateApi.paginateDetails(
                    addresses = definitionAddresses.toSet(),
                    metadataKeys = setOf(
                        ExplicitMetadataKey.NAME,
                        ExplicitMetadataKey.DESCRIPTION,
                        ExplicitMetadataKey.ACCOUNT_TYPE,
                        ExplicitMetadataKey.DAPP_DEFINITION,
                        ExplicitMetadataKey.DAPP_DEFINITIONS,
                        ExplicitMetadataKey.CLAIMED_WEBSITES,
                        ExplicitMetadataKey.CLAIMED_ENTITIES,
                        ExplicitMetadataKey.ICON_URL
                    )
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
