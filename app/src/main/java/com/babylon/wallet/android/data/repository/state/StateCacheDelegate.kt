package com.babylon.wallet.android.data.repository.state

import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItem
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleDetailsResponseItem
import com.babylon.wallet.android.data.repository.cache.database.AccountNFTJoin.Companion.asAccountNFTJoin
import com.babylon.wallet.android.data.repository.cache.database.ResourceEntity
import com.babylon.wallet.android.data.repository.cache.database.ResourceEntity.Companion.asEntity
import com.babylon.wallet.android.data.repository.cache.database.StateDao
import com.babylon.wallet.android.data.repository.cache.database.SyncInfo
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
import com.babylon.wallet.android.utils.truncatedHash
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import rdx.works.core.InstantGenerator
import timber.log.Timber
import java.time.Instant
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class StateCacheDelegate(
    private val stateDao: StateDao
) {

    // TODO check which are stale
    fun observeCachedAccounts(): Flow<Map<String, CachedDetails>> = stateDao.observeAccounts().map { detailsWithResources ->
        val result = mutableMapOf<String, CachedDetails>()
        val cacheMinBoundary = Instant.ofEpochMilli(accountCacheValidity())
        detailsWithResources.forEach { cache ->
            if (cache.accountSynced != null && cache.accountSynced < cacheMinBoundary) {
                Timber.tag("Bakos").d("\uD83D\uDDD1️ Stale ${cache.address.truncatedHash()}")
                return@forEach
            }

            // Parse details for this account
            val cachedDetails = CachedDetails(
                stateVersion = cache.stateVersion,
                typeMetadataItem = cache.accountType?.let { AccountTypeMetadataItem(it) },
            )

            // Compile all resources owned by this account
            if (cache.stateVersion != null && cache.resource != null && cache.amount != null) {
                when (val resource = cache.resource.toResource(cache.amount)) {
                    is Resource.FungibleResource -> {
                        result[cache.address] = result.getOrDefault(cache.address, cachedDetails).also {
                            it.fungibles.add(resource)
                        }
                    }

                    is Resource.NonFungibleResource -> {
                        result[cache.address] = result.getOrDefault(cache.address, cachedDetails).also {
                            it.nonFungibles.add(resource)
                        }
                    }
                }
            } else {
                result[cache.address] = cachedDetails
            }
        }
        result
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
            accountAddress: String,
            pools: Map<String, Pool>,
            validators: Map<String, ValidatorDetail>
        ): StateRepositoryImpl.AccountsWithAssetsFromCache? {
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


            return StateRepositoryImpl.AccountsWithAssetsFromCache(
                address = accountAddress,
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

        fun accountCacheValidity() = InstantGenerator().toEpochMilli() - accountsCacheDuration.inWholeMilliseconds

        fun resourcesCacheValidity(isRefreshing: Boolean = false) =
            InstantGenerator().toEpochMilli() - if (isRefreshing) 0 else resourcesCacheDuration.inWholeMilliseconds
    }
}
