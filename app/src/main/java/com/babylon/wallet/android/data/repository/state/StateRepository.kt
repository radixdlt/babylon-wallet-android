package com.babylon.wallet.android.data.repository.state

import com.babylon.wallet.android.data.gateway.apis.StateApi
import com.babylon.wallet.android.data.gateway.extensions.asMetadataItems
import com.babylon.wallet.android.data.gateway.extensions.paginateDetails
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleDataRequest
import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.data.repository.cache.database.NFTEntity.Companion.asEntity
import com.babylon.wallet.android.data.repository.cache.database.PoolEntity.Companion.asPools
import com.babylon.wallet.android.data.repository.cache.database.PoolEntity.Companion.toPoolsJoin
import com.babylon.wallet.android.data.repository.cache.database.StateDao
import com.babylon.wallet.android.data.repository.cache.database.SyncInfo
import com.babylon.wallet.android.data.repository.cache.database.ValidatorEntity.Companion.asValidatorEntities
import com.babylon.wallet.android.data.repository.cache.database.ValidatorEntity.Companion.asValidators
import com.babylon.wallet.android.data.repository.toResult
import com.babylon.wallet.android.di.coroutines.ApplicationScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.model.DApp
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.domain.model.assets.Assets
import com.babylon.wallet.android.domain.model.assets.ValidatorDetail
import com.babylon.wallet.android.domain.model.resources.AccountDetails
import com.babylon.wallet.android.domain.model.resources.Pool
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.XrdResource
import com.babylon.wallet.android.domain.model.resources.metadata.MetadataItem.Companion.consume
import com.babylon.wallet.android.domain.model.resources.metadata.OwnerKeyHashesMetadataItem
import com.babylon.wallet.android.utils.truncatedHash
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rdx.works.core.InstantGenerator
import rdx.works.profile.data.model.currentGateway
import rdx.works.profile.data.model.currentNetwork
import rdx.works.profile.data.model.pernetwork.Entity
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.repository.BackupProfileRepository
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.domain.notHiddenAccounts
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

@Singleton
class StateRepositoryImpl @Inject constructor(
    private val stateApi: StateApi,
    private val stateDao: StateDao,
    private val profileRepository: ProfileRepository,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
    @ApplicationScope private val applicationScope: CoroutineScope
) : StateRepository {

    private val accountsState = MutableStateFlow<List<AccountsWithAssetsFromCache>?>(null)

    private val cacheDelegate = StateCacheDelegate(stateDao = stateDao)
    private val stateApiDelegate = StateApiDelegate(stateApi = stateApi)

    init {
        applicationScope.launch {
            cacheDelegate.observeCachedAccounts().onEach { Timber.tag("Bakos").d("Cache invoked") }
            .transform { cached ->
                val stateVersion = cached.values.mapNotNull { it.stateVersion }.maxOrNull() ?: run {
                    emit(emptyList())
                    return@transform
                }

                val allValidatorAddresses = cached.map { it.value.validatorAddresses() }.flatten().toSet()
                val cachedValidators = cacheDelegate.getValidatorDetails(allValidatorAddresses, stateVersion).toMutableMap()
                val newValidators = stateApiDelegate.getValidatorsDetails(
                    allValidatorAddresses - cachedValidators.keys,
                    stateVersion
                ).asValidators().onEach {
                    cachedValidators[it.address] = it
                }
                if (newValidators.isNotEmpty()) {
                    Timber.tag("Bakos").d("\uD83D\uDCBD Inserting validators")
                    stateDao.insertValidators(newValidators.asValidatorEntities(SyncInfo(InstantGenerator(), stateVersion)))
                }

                val allPoolAddresses = cached.map { it.value.poolAddresses() }.flatten().toSet()
                val cachedPools = cacheDelegate.getPoolDetails(allPoolAddresses, stateVersion).toMutableMap()
                val newPools = stateApiDelegate.getPoolDetails(allPoolAddresses - cachedPools.keys, stateVersion).asPools().onEach {
                    cachedPools[it.address] = it
                }

                if (newPools.isNotEmpty()) {
                    Timber.tag("Bakos").d("\uD83D\uDCBD Inserting pools")
                    stateDao.updatePools(newPools.toPoolsJoin(SyncInfo(InstantGenerator(), stateVersion)))
                } else {
                    emit(
                        cached.mapNotNull {
                            it.value.toAccountWithAssets(
                                accountAddress = it.key,
                                pools = cachedPools,
                                validators = cachedValidators
                            )
                        }
                    )
                }
            }
            .distinctUntilChanged()
            .collect { accountsWithAssets ->
                accountsState.value = accountsWithAssets
            }
        }
    }

    override fun observeAccountsOnLedger(
        accounts: List<Network.Account>,
        isRefreshing: Boolean
    ): Flow<List<AccountWithAssets>> = accountsState
        .filterNotNull()
        .onStart {
            if (isRefreshing) {
                Timber.tag("Bakos").d("\uD83D\uDCBD Deleting accounts")
                stateDao.deleteAccounts(accounts.map { it.address }.toSet())
            }
        }
        .transform { cachedAccounts ->
            val accountsToReturn = accounts.map { account ->
                val cachedAccount = cachedAccounts.find { it.address == account.address }
                AccountWithAssets(
                    account = account,
                    details = cachedAccount?.details,
                    assets = cachedAccount?.assets
                )
            }.onEach {
                //Timber.tag("Bakos").d("${it.account.displayName} - ${it.assets?.allSize()}")
            }
            emit(accountsToReturn)

            // Put this on a different class that returns the GW ing
            val accountsOnGateway = stateApiDelegate.fetchAllResources(
                accountAddresses = accountsToReturn.filterNot { it.assets != null }.map { it.account.address }.toSet(),
                onStateVersion = cachedAccounts.maxOfOrNull { it.details?.stateVersion ?: -1L }?.takeIf { it > 0L },
            )
            if (accountsOnGateway.isNotEmpty()) {
                withContext(dispatcher) {
                    Timber.tag("Bakos").d("\uD83D\uDCBD Inserting accounts ${accountsOnGateway.map { it.accountAddress.truncatedHash() }}")
                    stateDao.updateAccountData(accountsOnGateway)
                }
            }
        }
        .flowOn(dispatcher)

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

        stateApiDelegate.getDAppsDetails(definitionAddresses.toSet()).map { item ->
            DApp.from(address = item.address, metadataItems = item.explicitMetadata?.asMetadataItems().orEmpty())
        }
    }

    data class AccountsWithAssetsFromCache(
        val address: String,
        val details: AccountDetails?,
        val assets: Assets?
    )
}
