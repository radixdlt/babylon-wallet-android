package com.babylon.wallet.android.data.repository.state

import com.babylon.wallet.android.data.gateway.apis.StateApi
import com.babylon.wallet.android.data.gateway.extensions.asMetadataItems
import com.babylon.wallet.android.data.gateway.extensions.paginateDetails
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleDataRequest
import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.data.repository.cache.database.AccountResourceJoin.Companion.asAccountResourceJoin
import com.babylon.wallet.android.data.repository.cache.database.NFTEntity.Companion.asEntity
import com.babylon.wallet.android.data.repository.cache.database.PoolEntity.Companion.asPoolsWithResources
import com.babylon.wallet.android.data.repository.cache.database.StateDao
import com.babylon.wallet.android.data.repository.cache.database.SyncInfo
import com.babylon.wallet.android.data.repository.cache.database.ValidatorEntity.Companion.asValidatorEntities
import com.babylon.wallet.android.data.repository.toResult
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.model.resources.AccountOnLedger
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.XrdResource
import com.babylon.wallet.android.domain.model.resources.metadata.MetadataItem.Companion.consume
import com.babylon.wallet.android.domain.model.resources.metadata.OwnerKeyHashesMetadataItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.withContext
import rdx.works.core.InstantGenerator
import rdx.works.profile.data.model.pernetwork.Entity
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.derivation.model.NetworkId
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject

interface StateRepository {

    fun observeAccountsOnLedger(accounts: List<Network.Account>, isRefreshing: Boolean): Flow<Map<Network.Account, AccountOnLedger>>

    suspend fun getMoreNFTs(account: Network.Account, resource: Resource.NonFungibleResource): Result<Resource.NonFungibleResource>

    fun observeResourceDetails(resourceAddress: String, accountAddress: String?): Flow<Resource>

    suspend fun getNFTDetails(resourceAddress: String, localId: String): Result<Resource.NonFungibleResource.Item>

    suspend fun getOwnedXRD(accounts: List<Network.Account>): Result<Map<Network.Account, BigDecimal>>

    suspend fun getEntityOwnerKeys(entities: List<Entity>): Result<Map<Entity, OwnerKeyHashesMetadataItem>>

    sealed class NFTPageError(cause: Throwable) : Exception(cause) {
        data object NoMorePages : NFTPageError(RuntimeException("No more NFTs for this resource."))

        data object VaultAddressMissing : NFTPageError(RuntimeException("No vault address to fetch NFTs"))

        data object StateVersionMissing : NFTPageError(RuntimeException("State version missing for account."))
    }
}

class StateRepositoryImpl @Inject constructor(
    private val stateApi: StateApi,
    private val stateDao: StateDao,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) : StateRepository {

    private val cacheDelegate = StateCacheDelegate(stateDao = stateDao)
    private val stateApiDelegate = StateApiDelegate(stateApi = stateApi)

    override fun observeAccountsOnLedger(
        accounts: List<Network.Account>,
        isRefreshing: Boolean
    ): Flow<Map<Network.Account, AccountOnLedger>> = cacheDelegate
        .observeCachedAccounts(accounts, isRefreshing)
        .transform { cachedAccounts ->
            val cachedAccountsWithDetails = cachedAccounts.mapValues { it.value.toAccountDetails() }
            emit(cachedAccountsWithDetails)

            val prevAccountsStateVersion = cachedAccountsWithDetails.values.lastOrNull()?.details?.stateVersion

            val remainingAccounts = accounts.toSet() - cachedAccounts.keys
            if (remainingAccounts.isNotEmpty()) {
                Timber.tag("Bakos").d("=> ${remainingAccounts.first().displayName}")
                stateApiDelegate.fetchAllResources(
                    accounts = setOf(remainingAccounts.first()),
                    onStateVersion = prevAccountsStateVersion,
                    onAccount = { account, gatewayDetails ->
                        val accountMetadataItems = gatewayDetails.accountMetadata?.asMetadataItems()?.toMutableList()
                        val syncInfo = SyncInfo(synced = InstantGenerator(), accountStateVersion = gatewayDetails.ledgerState.stateVersion)
                        val fungibleEntityPairs = gatewayDetails.fungibles.map { item ->
                            item.asAccountResourceJoin(account.address, syncInfo)
                        }
                        val nonFungibleEntityPairs = gatewayDetails.nonFungibles.map { item ->
                            item.asAccountResourceJoin(account.address, syncInfo)
                        }

                        // Gather and store pool details
                        val poolAddresses = fungibleEntityPairs.mapNotNull { it.second.poolAddress }.toSet()
                        val pools =
                            stateApiDelegate.getPoolDetails(poolAddresses = poolAddresses, stateVersion = syncInfo.accountStateVersion)

                        // Gather and store validator details
                        val validatorAddresses = (fungibleEntityPairs.mapNotNull {
                            it.second.validatorAddress
                        } + nonFungibleEntityPairs.mapNotNull {
                            it.second.validatorAddress
                        }).toSet()
                        val validators = stateApiDelegate.getValidatorsDetails(
                            validatorsAddresses = validatorAddresses,
                            stateVersion = syncInfo.accountStateVersion
                        )

                        // Store account details
                        stateDao.updateAccountData(
                            accountAddress = account.address,
                            accountTypeMetadataItem = accountMetadataItems?.consume(),
                            syncInfo = syncInfo,
                            accountWithResources = fungibleEntityPairs + nonFungibleEntityPairs,
                            poolsWithResources = pools.asPoolsWithResources(syncInfo),
                            validators = validators.asValidatorEntities(syncInfo)
                        )
                    }
                )
            }
        }
        .flowOn(dispatcher)
        .distinctUntilChanged()

    override suspend fun getMoreNFTs(
        account: Network.Account,
        resource: Resource.NonFungibleResource
    ): Result<Resource.NonFungibleResource> = withContext(dispatcher) {
        runCatching {
            // No more pages to return
            if (resource.amount.toInt() == resource.items.size) throw StateRepository.NFTPageError.NoMorePages

            val accountNftPortfolio = stateDao.getAccountNFTPortfolio(
                accountAddress = account.address,
                resourceAddress = resource.resourceAddress
            ).firstOrNull()

            val accountStateVersion: Long =
                accountNftPortfolio?.accountStateVersion ?: throw StateRepository.NFTPageError.StateVersionMissing

            val cachedNFTItems = stateDao.getOwnedNfts(
                accountAddress = account.address,
                resourceAddress = resource.resourceAddress,
                stateVersion = accountStateVersion
            )

            // All items cached, return the result
            if (cachedNFTItems.size == resource.amount.toInt()) {
                return@runCatching resource.copy(items = cachedNFTItems.map { it.toItem() })
            }

            val vaultAddress = accountNftPortfolio.vaultAddress ?: throw StateRepository.NFTPageError.VaultAddressMissing
            val nextCursor = accountNftPortfolio.nextCursor

            Timber.tag("Bakos").d("Fetching NFT items ($nextCursor)")
            val page = stateApiDelegate.getNextNftItems(
                accountAddress = account.address,
                resourceAddress = resource.resourceAddress,
                vaultAddress = vaultAddress,
                nextCursor = nextCursor,
                stateVersion = accountStateVersion
            )
            val syncInfo = SyncInfo(synced = InstantGenerator(), accountStateVersion = accountStateVersion)

            val newItems = cacheDelegate.storeAccountNFTsPortfolio(
                accountAddress = account.address,
                resourceAddress = resource.resourceAddress,
                nextCursor = page.first,
                items = page.second,
                syncInfo = syncInfo
            )
            val currentItems = resource.items
            val allNewItems = (currentItems + newItems).distinctBy { it.localId }

            resource.copy(items = allNewItems)
        }
    }

    override fun observeResourceDetails(resourceAddress: String, accountAddress: String?): Flow<Resource> = flow {
        val cachedEntity = stateDao.getResourceDetails(
            resourceAddress = resourceAddress,
            minValidity = StateCacheDelegate.resourcesCacheValidity()
        )
        val amount = accountAddress?.let {
            stateDao.getAccountResourceJoin(resourceAddress = resourceAddress, accountAddress = accountAddress)?.amount
        }

        val cachedResource = cachedEntity?.toResource(amount)
        if (cachedResource != null) {
            emit(cachedResource)
        }

        if (cachedResource?.isDetailsAvailable == false) {
            val item = stateApiDelegate.getResourceDetails(resourceAddress = resourceAddress)
            val updatedEntity = cacheDelegate.updateResourceDetails(item)

            emit(updatedEntity.toResource(amount))
        }
    }

    override suspend fun getNFTDetails(resourceAddress: String, localId: String): Result<Resource.NonFungibleResource.Item> =
        withContext(dispatcher) {
            val cachedItem = stateDao.getNFTDetails(resourceAddress, localId, StateCacheDelegate.resourcesCacheValidity())

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

    override suspend fun getOwnedXRD(accounts: List<Network.Account>): Result<Map<Network.Account, BigDecimal>> = withContext(dispatcher) {
        if (accounts.isEmpty()) return@withContext Result.success(emptyMap())
        val networkId = NetworkId.from(accounts.first().networkID)
        val xrdAddress = XrdResource.address(networkId = networkId)

        val accountsWithXRDVaults = accounts.associateWith { account ->
            stateDao.getAccountResourceJoin(accountAddress = account.address, resourceAddress = xrdAddress)?.vaultAddress
        }

        runCatching {
            val vaultsWithAmounts = stateApiDelegate.getVaultsDetails(accountsWithXRDVaults.mapNotNull { it.value }.toSet())

            accountsWithXRDVaults.mapValues { entry ->
                entry.value?.let { vaultsWithAmounts[it] } ?: BigDecimal.ZERO
            }
        }
    }

    override suspend fun getEntityOwnerKeys(entities: List<Entity>): Result<Map<Entity, OwnerKeyHashesMetadataItem>> = runCatching {
        val entitiesWithOwnerKeys = mutableMapOf<Entity, OwnerKeyHashesMetadataItem>()
        stateApi.paginateDetails(
            addresses = entities.map { it.address }.toSet(),
            metadataKeys = setOf(ExplicitMetadataKey.OWNER_KEYS),
        ) { page ->
            page.items.forEach { item ->
                val keyHash = item.explicitMetadata
                    ?.asMetadataItems()
                    ?.toMutableList()
                    ?.consume<OwnerKeyHashesMetadataItem>() ?: return@forEach
                val entity = entities.find { item.address == it.address } ?: return@forEach
                entitiesWithOwnerKeys[entity] = keyHash
            }
        }
        entitiesWithOwnerKeys
    }
}
