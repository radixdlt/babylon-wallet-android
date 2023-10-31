package com.babylon.wallet.android.data.repository.state

import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItem
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleDetailsResponseItem
import com.babylon.wallet.android.data.repository.cache.database.AccountNFTJoin.Companion.asAccountNFTJoin
import com.babylon.wallet.android.data.repository.cache.database.ResourceEntity
import com.babylon.wallet.android.data.repository.cache.database.ResourceEntity.Companion.asEntity
import com.babylon.wallet.android.data.repository.cache.database.StateDao
import com.babylon.wallet.android.data.repository.cache.database.SyncInfo
import com.babylon.wallet.android.domain.model.resources.AccountDetails
import com.babylon.wallet.android.domain.model.resources.AccountOnLedger
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.metadata.AccountTypeMetadataItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import rdx.works.core.InstantGenerator
import rdx.works.profile.data.model.pernetwork.Network
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class StateCacheDelegate(
    private val stateDao: StateDao
) {

    fun observeCachedAccounts(
        accounts: List<Network.Account>,
        isRefreshing: Boolean
    ): Flow<Map<Network.Account, CachedDetails>> {
        val accountAddresses = accounts.map { it.address }
        return stateDao.observeAccountsPortfolio(accountAddresses, accountCacheValidity(isRefreshing))
            .distinctUntilChanged()
            .map { detailsWithResources ->
                val result = mutableMapOf<Network.Account, CachedDetails>()

                detailsWithResources.forEach { accountDetailsAndResources ->
                    val account = accounts.find { it.address == accountDetailsAndResources.address } ?: return@forEach

                    // Parse details for this account
                    val cachedDetails = CachedDetails(
                        accountDetails = AccountDetails(
                            stateVersion = accountDetailsAndResources.stateVersion,
                            typeMetadataItem = accountDetailsAndResources.accountType?.let { AccountTypeMetadataItem(it) }
                        )
                    )

                    // Compile all resources owned by this account
                    if (accountDetailsAndResources.resource != null && accountDetailsAndResources.amount != null) {
                        when (val resource = accountDetailsAndResources.resource.toResource(accountDetailsAndResources.amount)) {
                            is Resource.FungibleResource -> {
                                result[account] = result.getOrDefault(account, cachedDetails).also {
                                    it.fungibles.add(resource)
                                }
                            }

                            is Resource.NonFungibleResource -> {
                                result[account] = result.getOrDefault(account, cachedDetails).also {
                                    it.nonFungibles.add(resource)
                                }
                            }
                        }
                    } else {
                        result[account] = cachedDetails
                    }
                }

                result
            }.distinctUntilChanged()
    }

    fun storeAccountNFTsPortfolio(
        accountAddress: String,
        resourceAddress: String,
        nextCursor: String?,
        items: List<StateNonFungibleDetailsResponseItem>,
        syncInfo: SyncInfo
    ): List<Resource.NonFungibleResource.Item> {
        val pair = items.map {
            it.asAccountNFTJoin(accountAddress, resourceAddress, syncInfo)
        }

        val nfts = pair.map { it.second }
        stateDao.storeAccountNFTsPortfolio(
            accountAddress = accountAddress,
            resourceAddress = resourceAddress,
            cursor = nextCursor,
            accountNFTsJoin = pair.map { it.first },
            nfts = nfts
        )

        return nfts.map { it.toItem() }
    }

    fun updateResourceDetails(item: StateEntityDetailsResponseItem): ResourceEntity {
        val entity = item.asEntity(synced = InstantGenerator())
        stateDao.updateResourceDetails(entity)
        return entity
    }

    data class CachedDetails(
        val accountDetails: AccountDetails,
        val fungibles: MutableList<Resource.FungibleResource> = mutableListOf(),
        val nonFungibles: MutableList<Resource.NonFungibleResource> = mutableListOf(),
    ) {

        fun toAccountDetails() = AccountOnLedger(
            accountDetails,
            fungibles,
            nonFungibles
        )

    }

    companion object {
        private val accountsCacheDuration = 2.toDuration(DurationUnit.HOURS)
        private val resourcesCacheDuration = 48.toDuration(DurationUnit.HOURS)

        fun accountCacheValidity(isRefreshing: Boolean = false) =
            InstantGenerator().toEpochMilli() - if (isRefreshing) 0 else accountsCacheDuration.inWholeMilliseconds

        fun resourcesCacheValidity(isRefreshing: Boolean = false) =
            InstantGenerator().toEpochMilli() - if (isRefreshing) 0 else resourcesCacheDuration.inWholeMilliseconds
    }
}
