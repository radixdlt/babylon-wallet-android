package com.babylon.wallet.android.data.repository.cache.database

import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItem
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleDetailsResponseItem
import com.babylon.wallet.android.data.repository.cache.database.AccountNFTJoin.Companion.asAccountNFTJoin
import com.babylon.wallet.android.data.repository.cache.database.ResourceEntity.Companion.asEntity
import com.babylon.wallet.android.data.repository.cache.database.StateDao.Companion.resourcesCacheValidity
import com.radixdlt.sargon.ValidatorAddress
import rdx.works.core.InstantGenerator
import rdx.works.core.domain.resources.Pool
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.Validator

@Suppress("UnsafeCallOnNullableType")
fun StateDao.getCachedPools(poolAddresses: Set<String>, atStateVersion: Long): Map<String, Pool> {
    val pools = mutableMapOf<String, Pool>()
    getPoolDetails(poolAddresses, atStateVersion).forEach { join ->
        // If pool's resource is not up to date or has no details, all pool info is considered stale
        val poolResource = getPoolResource(join.address, resourcesCacheValidity()) ?: return@forEach

        val associatedDApp = getPoolAssociatedDApp(join.address)?.toDApp()

        val resource = if (join.resource != null && join.amount != null) {
            join.resource.toResource(join.amount) as Resource.FungibleResource
        } else {
            return@forEach
        }

        // TODO maybe add check if pool resource is up to date with details
        val pool = pools[poolResource.poolAddress]
        pools[poolResource.poolAddress!!] = pool?.copy(
            resources = pool.resources.toMutableList().apply { add(resource) },
            associatedDApp = associatedDApp
        ) ?: Pool(
            address = join.address,
            resources = listOf(resource),
            metadata = join.poolMetadata?.metadata.orEmpty(),
            associatedDApp = associatedDApp
        )
    }
    return pools.mapValues { it.value.copy(resources = it.value.resources.sorted()) }
}

fun StateDao.getCachedValidators(addresses: Set<ValidatorAddress>, atStateVersion: Long): Map<ValidatorAddress, Validator> {
    val validators = mutableMapOf<ValidatorAddress, Validator>()
    getValidators(addresses, atStateVersion).forEach { entity ->
        validators[entity.address] = entity.asValidatorDetail()
    }
    return validators
}

fun StateDao.storeAccountNFTsPortfolio(
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
    insertAccountNFTsJoin(
        accountAddress = accountAddress,
        resourceAddress = resourceAddress,
        cursor = nextCursor,
        accountNFTsJoin = pair.map { it.first },
        nfts = nfts
    )

    return nfts.map { it.toItem() }
}

fun StateDao.updateResourceDetails(item: StateEntityDetailsResponseItem): ResourceEntity {
    val entity = item.asEntity(synced = InstantGenerator())
    insertOrReplaceResources(listOf(entity))
    return entity
}
