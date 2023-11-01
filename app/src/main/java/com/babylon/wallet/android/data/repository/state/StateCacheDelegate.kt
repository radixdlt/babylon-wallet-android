package com.babylon.wallet.android.data.repository.state

import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItem
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleDetailsResponseItem
import com.babylon.wallet.android.data.repository.cache.database.AccountNFTJoin.Companion.asAccountNFTJoin
import com.babylon.wallet.android.data.repository.cache.database.PoolEntity.Companion.toPoolsJoin
import com.babylon.wallet.android.data.repository.cache.database.ResourceEntity
import com.babylon.wallet.android.data.repository.cache.database.ResourceEntity.Companion.asEntity
import com.babylon.wallet.android.data.repository.cache.database.StateDao
import com.babylon.wallet.android.data.repository.cache.database.SyncInfo
import com.babylon.wallet.android.data.repository.cache.database.ValidatorEntity.Companion.asValidatorEntities
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.domain.model.assets.Assets
import com.babylon.wallet.android.domain.model.assets.LiquidStakeUnit
import com.babylon.wallet.android.domain.model.assets.PoolUnit
import com.babylon.wallet.android.domain.model.assets.StakeClaim
import com.babylon.wallet.android.domain.model.assets.ValidatorDetail
import com.babylon.wallet.android.domain.model.assets.ValidatorWithStakes
import com.babylon.wallet.android.domain.model.resources.AccountDetails
import com.babylon.wallet.android.domain.model.resources.Pool
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

                detailsWithResources.forEach { cache ->
                    val account = accounts.find { it.address == cache.address } ?: return@forEach

                    // Parse details for this account
                    val cachedDetails = CachedDetails(
                        stateVersion = cache.stateVersion,
                        typeMetadataItem = cache.accountType?.let { AccountTypeMetadataItem(it) },
                    )

                    // Compile all resources owned by this account
                    if (cache.stateVersion != null && cache.resource != null && cache.amount != null) {
                        when (val resource = cache.resource.toResource(cache.amount)) {
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
            }
            .distinctUntilChanged()
    }

    fun markPendingRemainingAccounts(
        allAccounts: List<Network.Account>,
        cached: StateRepositoryImpl.AccountsWithAssetsFromCache
    ): Set<Network.Account> {
        val inProgressAccounts = allAccounts.filter { it.address in cached.inProgress }.toSet()
        val remainingAccounts = (allAccounts.toSet() - cached.completed.keys - inProgressAccounts).take(1).toSet()
        stateDao.updatePendingData(
            pendingAccountAddresses = remainingAccounts.map { it.address },
            newPools = cached.stateVersion?.let {
                cached.newPools.toPoolsJoin(SyncInfo(synced = InstantGenerator(), accountStateVersion = it))
            },
            newValidators = cached.stateVersion?.let {
                cached.newValidators.asValidatorEntities(SyncInfo(synced = InstantGenerator(), accountStateVersion = it))
            }
        )
        return remainingAccounts
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
        stateDao.insertOrReplaceResources(listOf(entity))
        return entity
    }

    fun getPoolDetails(addresses: Set<String>, atStateVersion: Long): Map<String, Pool> {
        val pools = mutableMapOf<String, Pool>()
        stateDao.getPoolDetails(addresses, atStateVersion).forEach { join ->
            val resource = if (join.resource != null && join.amount != null) {
                join.resource.toResource(join.amount) as Resource.FungibleResource
            } else {
                return@forEach
            }

            val pool = pools[join.address]
            pools[join.address] = pool?.copy(
                resources = pool.resources.toMutableList().apply { add(resource) }
            ) ?: Pool(address = join.address, resources = listOf(resource))
        }
        return pools
    }

    fun getValidatorDetails(addresses: Set<String>, atStateVersion: Long): Map<String, ValidatorDetail> {
        val validators = mutableMapOf<String, ValidatorDetail>()
        stateDao.getValidators(addresses, atStateVersion).forEach { entity ->
            validators[entity.address] = entity.asValidatorDetail()
        }
        return validators
    }

    data class CachedDetails(
        val stateVersion: Long?,
        val typeMetadataItem: AccountTypeMetadataItem?,
        val fungibles: MutableList<Resource.FungibleResource> = mutableListOf(),
        val nonFungibles: MutableList<Resource.NonFungibleResource> = mutableListOf(),
    ) {

        fun poolAddresses() = fungibles.mapNotNull { it.poolAddress }.toSet()

        fun validatorAddresses() = (fungibles.mapNotNull { it.validatorAddress } + nonFungibles.mapNotNull { it.validatorAddress }).toSet()

        fun toAccountWithAssets(
            account: Network.Account,
            pools: Map<String, Pool>,
            validators: Map<String, ValidatorDetail>
        ): AccountWithAssets? {
            if (stateVersion == null) return null

            val stakeUnitAddressToValidator = validators.mapKeys { it.value.stakeUnitResourceAddress }
            val claimTokenAddressToValidator = validators.mapKeys { it.value.claimTokenResourceAddress }

            val resultingPoolUnits = mutableListOf<PoolUnit>()
            val resultingStakeUnits = mutableMapOf<ValidatorDetail, ValidatorWithStakes>()

            val resultingFungibles = fungibles.toMutableList()
            val resultingNonFungibles = nonFungibles.toMutableList()

            val fungiblesIterator = resultingFungibles.iterator()
            while (fungiblesIterator.hasNext()) {
                val fungible = fungiblesIterator.next()

                val pool = pools[fungible.poolAddress]
                if (pool != null) {
                    resultingPoolUnits.add(
                        PoolUnit(
                            stake = fungible,
                            pool = pool,
                        )
                    )

                    fungiblesIterator.remove()
                }

                val validatorDetails = stakeUnitAddressToValidator[fungible.resourceAddress]
                if (validatorDetails != null) {
                    val lsu = LiquidStakeUnit(fungible)

                    resultingStakeUnits[validatorDetails] = ValidatorWithStakes(
                        validatorDetail = validatorDetails,
                        liquidStakeUnit = lsu
                    )

                    // Remove this fungible from the list as it will be included as an lsu
                    fungiblesIterator.remove()
                }
            }

            val nonFungiblesIterator = resultingNonFungibles.iterator()
            while (nonFungiblesIterator.hasNext()) {
                val nonFungible = nonFungiblesIterator.next()

                val validatorDetails = claimTokenAddressToValidator[nonFungible.resourceAddress]
                if (validatorDetails != null) {
                    resultingStakeUnits[validatorDetails]?.copy(stakeClaimNft = StakeClaim(nonFungible))?.let {
                        resultingStakeUnits[validatorDetails] = it
                    }

                    // Remove this non-fungible from the list as it will be included as a stake claim
                    nonFungiblesIterator.remove()
                }
            }

            val resultingValidatorsWithStakeResources = resultingStakeUnits.map {
                ValidatorWithStakes(
                    validatorDetail = it.key,
                    liquidStakeUnit = it.value.liquidStakeUnit,
                    stakeClaimNft = it.value.stakeClaimNft
                )
            }


            return AccountWithAssets(
                account = account,
                details = AccountDetails(
                    stateVersion = stateVersion,
                    typeMetadataItem = typeMetadataItem
                ),
                assets = Assets(
                    fungibles = resultingFungibles,
                    nonFungibles = resultingNonFungibles,
                    poolUnits = resultingPoolUnits,
                    validatorsWithStakes = resultingValidatorsWithStakeResources
                )
            )
        }

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
