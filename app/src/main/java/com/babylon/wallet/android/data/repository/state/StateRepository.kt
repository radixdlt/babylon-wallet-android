package com.babylon.wallet.android.data.repository.state

import com.babylon.wallet.android.data.gateway.apis.StateApi
import com.babylon.wallet.android.data.gateway.extensions.asMetadataItems
import com.babylon.wallet.android.data.repository.cache.database.AccountResourceJoin.Companion.asAccountResourceJoin
import com.babylon.wallet.android.data.repository.cache.database.PoolEntity.Companion.asPoolsWithResources
import com.babylon.wallet.android.data.repository.cache.database.StateDao
import com.babylon.wallet.android.data.repository.cache.database.SyncInfo
import com.babylon.wallet.android.data.repository.cache.database.ValidatorEntity.Companion.asValidatorEntities
import com.babylon.wallet.android.domain.model.resources.metadata.MetadataItem.Companion.consume
import com.babylon.wallet.android.domain.model.resources.AccountOnLedger
import com.babylon.wallet.android.domain.model.resources.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.transform
import rdx.works.core.InstantGenerator
import rdx.works.profile.data.model.pernetwork.Network
import timber.log.Timber
import java.lang.RuntimeException
import javax.inject.Inject

interface StateRepository {

    fun observeAccountsOnLedger(accounts: List<Network.Account>, isRefreshing: Boolean): Flow<Map<Network.Account, AccountOnLedger>>

    suspend fun getMoreNFTs(account: Network.Account, resource: Resource.NonFungibleResource): Result<Resource.NonFungibleResource>

    sealed class NFTPageError(cause: Throwable): Exception(cause) {
        data object NoMorePages: NFTPageError(RuntimeException("No more NFTs for this resource."))

        data object VaultAddressMissing: NFTPageError(RuntimeException("No vault address to fetch NFTs"))

        data object StateVersionMissing : NFTPageError(RuntimeException("State version missing for account."))
    }
}

class StateRepositoryImpl @Inject constructor(
    private val stateApi: StateApi,
    private val stateDao: StateDao
) : StateRepository {

    private val cacheDelegate = StateCacheDelegate(stateDao = stateDao)
    private val stateApiDelegate = StateApiDelegate(stateApi = stateApi)

    override fun observeAccountsOnLedger(
        accounts: List<Network.Account>,
        isRefreshing: Boolean
    ): Flow<Map<Network.Account, AccountOnLedger>> = cacheDelegate
        .observeCachedAccounts(accounts, isRefreshing)
        .transform { cachedAccounts ->
            emit(cachedAccounts.mapValues { it.value.toAccountDetails() })

            val remainingAccounts = accounts.toSet() - cachedAccounts.keys
            if (remainingAccounts.isNotEmpty()) {
                Timber.tag("Bakos").d("=> ${remainingAccounts.first().displayName}")
                stateApiDelegate.fetchAllResources(
                    accounts = setOf(remainingAccounts.first()),
                    onAccount = { account, gatewayDetails ->
                        val accountMetadataItems = gatewayDetails.accountMetadata?.asMetadataItems()?.toMutableList()
                        val syncInfo = SyncInfo(synced = InstantGenerator(), stateVersion = gatewayDetails.ledgerState.stateVersion)
                        val fungibleEntityPairs = gatewayDetails.fungibles.map { item ->
                            item.asAccountResourceJoin(account.address, syncInfo)
                        }
                        val nonFungibleEntityPairs = gatewayDetails.nonFungibles.map { item ->
                            item.asAccountResourceJoin(account.address, syncInfo)
                        }

                        // Gather and store pool details
                        val poolAddresses = fungibleEntityPairs.mapNotNull { it.second.poolAddress }.toSet()
                        val pools = stateApiDelegate.getPoolDetails(poolAddresses = poolAddresses, stateVersion = syncInfo.stateVersion)

                        // Gather and store validator details
                        val validatorAddresses = (fungibleEntityPairs.mapNotNull {
                            it.second.validatorAddress
                        } + nonFungibleEntityPairs.mapNotNull {
                            it.second.validatorAddress
                        }).toSet()
                        val validators = stateApiDelegate.getValidatorsDetails(
                            validatorsAddresses = validatorAddresses,
                            stateVersion = syncInfo.stateVersion
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
        }.distinctUntilChanged()

    override suspend fun getMoreNFTs(
        account: Network.Account,
        resource: Resource.NonFungibleResource
    ): Result<Resource.NonFungibleResource> = runCatching {
        // No more pages to return
        if (resource.amount.toInt() == resource.items.size) throw StateRepository.NFTPageError.NoMorePages

        val accountNftPortfolio = stateDao.getAccountNFTPortfolio(
            accountAddress = account.address,
            resourceAddress = resource.resourceAddress
        ).firstOrNull()

        val stateVersion: Long = accountNftPortfolio?.accountStateVersion ?: throw StateRepository.NFTPageError.StateVersionMissing

        val cachedNFTItems = stateDao.getOwnedNfts(
            accountAddress = account.address,
            resourceAddress = resource.resourceAddress,
            stateVersion = stateVersion
        )

        // All items cached, return the result
        if (cachedNFTItems.size == resource.amount.toInt()) {
            return@runCatching resource.copy(items = cachedNFTItems.map { it.toItem() })
        }

        val vaultAddress = accountNftPortfolio.accountNonFungibleResourceJoin.vaultAddress ?: throw StateRepository.NFTPageError.VaultAddressMissing
        val nextCursor = accountNftPortfolio.accountNonFungibleResourceJoin.nextCursor

        Timber.tag("Bakos").d("Fetching NFT items ($nextCursor)")
        val page = stateApiDelegate.getNextNftItems(
            accountAddress = account.address,
            resourceAddress = resource.resourceAddress,
            vaultAddress = vaultAddress,
            nextCursor = nextCursor,
            stateVersion = stateVersion
        )
        val syncInfo = SyncInfo(synced = InstantGenerator(), stateVersion = stateVersion)

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
