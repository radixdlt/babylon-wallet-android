package com.babylon.wallet.android.data.repository.state

import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleDetailsResponseItem
import com.babylon.wallet.android.data.repository.cache.database.AccountNFTJoin.Companion.asAccountNFTJoin
import com.babylon.wallet.android.data.repository.cache.database.StateDao
import com.babylon.wallet.android.data.repository.cache.database.SyncInfo
import com.babylon.wallet.android.domain.model.assets.ValidatorDetail
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

                    // If for some reason the account's state version has changed but its joins have an older one
                    // that means that the amount information is stale so we need to treat this account as if it does
                    // not exist in the cache. So we avoid adding it into the result.
                    if (accountDetailsAndResources.stateVersionsMismatch) return@forEach

                    // Parse details for this account
                    val cachedDetails = CachedDetails(
                        accountStateVersion = accountDetailsAndResources.accountStateVersion,
                        accountDetails = AccountDetails(
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

                val iterator = result.entries.iterator()
                while (iterator.hasNext()) {
                    val cachedData = iterator.next().value

                    // Compile all pools
                    val poolAddresses = cachedData.poolAddresses()
                    val cachedPoolsWithTokens = stateDao.getPoolDetails(poolAddresses, cachedData.accountStateVersion)
                    cachedPoolsWithTokens.forEach { tokenInPool ->
                        if (tokenInPool.resource != null && tokenInPool.amount != null) {
                            cachedData.pools[tokenInPool.address] = cachedData.pools.getOrDefault(
                                tokenInPool.address,
                                mutableListOf()
                            ).also {
                                it.add(tokenInPool.resource.toResource(tokenInPool.amount) as Resource.FungibleResource)
                            }
                        }
                    }
                    val remainingPools = poolAddresses - cachedData.pools.keys
                    if (remainingPools.isNotEmpty()) {
                        iterator.remove()
                    }

                    // Compile all validators
                    val validatorsAddresses = cachedData.validatorAddresses()
                    val cachedValidators = stateDao.getValidators(validatorsAddresses, cachedData.accountStateVersion)
                    if (cachedValidators.size != validatorsAddresses.size) {
                        iterator.remove()
                    }
                    cachedData.validators.addAll(cachedValidators.map { it.asValidatorDetail() })
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

    data class CachedDetails(
        val accountStateVersion: Long,
        val accountDetails: AccountDetails,
        val fungibles: MutableList<Resource.FungibleResource> = mutableListOf(),
        val nonFungibles: MutableList<Resource.NonFungibleResource> = mutableListOf(),
        val pools: MutableMap<String, MutableList<Resource.FungibleResource>> = mutableMapOf(),
        val validators: MutableSet<ValidatorDetail> = mutableSetOf()
    ) {

        fun poolAddresses() = fungibles.mapNotNull { it.poolAddress }.toSet()

        fun validatorAddresses() = (fungibles.mapNotNull { it.validatorAddress } + nonFungibles.mapNotNull { it.validatorAddress }).toSet()

        fun toAccountDetails() = AccountOnLedger(
            accountDetails,
            fungibles,
            nonFungibles,
            validators.toList(),
            pools
        )

    }

    companion object {
        private val accountsCacheDuration = 2.toDuration(DurationUnit.HOURS)
        private val resourcesCacheDuration = 24.toDuration(DurationUnit.HOURS)

        fun accountCacheValidity(isRefreshing: Boolean) =
            InstantGenerator().toEpochMilli() - if (isRefreshing) 0 else accountsCacheDuration.inWholeMilliseconds

        fun resourcesCacheValidity(isRefreshing: Boolean) =
            InstantGenerator().toEpochMilli() - if (isRefreshing) 0 else resourcesCacheDuration.inWholeMilliseconds
    }
}
