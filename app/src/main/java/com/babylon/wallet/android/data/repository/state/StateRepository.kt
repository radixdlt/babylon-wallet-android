package com.babylon.wallet.android.data.repository.state

import com.babylon.wallet.android.data.gateway.apis.StateApi
import com.babylon.wallet.android.data.gateway.extensions.asMetadataItems
import com.babylon.wallet.android.data.gateway.extensions.getNextNftItems
import com.babylon.wallet.android.data.gateway.extensions.getSingleEntityDetails
import com.babylon.wallet.android.data.gateway.extensions.paginateDetails
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItem
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleDataRequest
import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.data.repository.cache.database.NFTEntity.Companion.asEntity
import com.babylon.wallet.android.data.repository.cache.database.StateDao
import com.babylon.wallet.android.data.repository.cache.database.StateDao.Companion.resourcesCacheValidity
import com.babylon.wallet.android.data.repository.cache.database.SyncInfo
import com.babylon.wallet.android.data.repository.cache.database.storeAccountNFTsPortfolio
import com.babylon.wallet.android.data.repository.cache.database.updateResourceDetails
import com.babylon.wallet.android.data.repository.toResult
import com.babylon.wallet.android.di.coroutines.ApplicationScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.model.DApp
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.metadata.MetadataItem.Companion.consume
import com.babylon.wallet.android.domain.model.resources.metadata.OwnerKeyHashesMetadataItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import rdx.works.core.InstantGenerator
import rdx.works.profile.data.model.pernetwork.Entity
import rdx.works.profile.data.model.pernetwork.Network
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

interface StateRepository {

    fun observeAccountsOnLedger(accounts: List<Network.Account>, isRefreshing: Boolean): Flow<List<AccountWithAssets>>

    suspend fun getMoreNFTs(account: Network.Account, resource: Resource.NonFungibleResource): Result<Resource.NonFungibleResource>

    fun observeResourceDetails(resourceAddress: String, accountAddress: String?): Flow<Resource>

    suspend fun getNFTDetails(resourceAddress: String, localId: String): Result<Resource.NonFungibleResource.Item>

    suspend fun getOwnedXRD(accounts: List<Network.Account>): Result<Map<Network.Account, BigDecimal>>

    suspend fun getEntityOwnerKeys(entities: List<Entity>): Result<Map<Entity, OwnerKeyHashesMetadataItem>>

    suspend fun getDAppsDetails(definitionAddresses: List<String>): Result<List<DApp>>

    sealed class NFTPageError(cause: Throwable) : Exception(cause) {
        data object NoMorePages : NFTPageError(RuntimeException("No more NFTs for this resource."))

        data object VaultAddressMissing : NFTPageError(RuntimeException("No vault address to fetch NFTs"))

        data object StateVersionMissing : NFTPageError(RuntimeException("State version missing for account."))
    }
}

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
            val allNewItems = (currentItems + newItems).distinctBy { it.localId }

            resource.copy(items = allNewItems)
        }
    }

    override fun observeResourceDetails(resourceAddress: String, accountAddress: String?): Flow<Resource> = flow {
        val cachedEntity = stateDao.getResourceDetails(
            resourceAddress = resourceAddress,
            minValidity = resourcesCacheValidity()
        )
        val amount = accountAddress?.let {
            stateDao.getAccountResourceJoin(resourceAddress = resourceAddress, accountAddress = accountAddress)?.amount
        }

        val cachedResource = cachedEntity?.toResource(amount)
        if (cachedResource != null) {
            emit(cachedResource)
        }

        if (cachedResource?.isDetailsAvailable == false) {
            val item = stateApi.getSingleEntityDetails(
                address = resourceAddress,
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
                )
            )
            val updatedEntity = stateDao.updateResourceDetails(item)

            emit(updatedEntity.toResource(amount))
        }
    }

    override suspend fun getNFTDetails(resourceAddress: String, localId: String): Result<Resource.NonFungibleResource.Item> =
        withContext(dispatcher) {
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

    override suspend fun getEntityOwnerKeys(entities: List<Entity>): Result<Map<Entity, OwnerKeyHashesMetadataItem>> = runCatching {
        if (entities.isEmpty()) return@runCatching mapOf()

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

    override suspend fun getDAppsDetails(definitionAddresses: List<String>): Result<List<DApp>> = runCatching {
        if (definitionAddresses.isEmpty()) return@runCatching listOf()

        val items = mutableListOf<StateEntityDetailsResponseItem>()
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
            items.addAll(page.items)
        }

        items.map { item ->
            DApp.from(address = item.address, metadataItems = item.explicitMetadata?.asMetadataItems().orEmpty())
        }
    }
}
