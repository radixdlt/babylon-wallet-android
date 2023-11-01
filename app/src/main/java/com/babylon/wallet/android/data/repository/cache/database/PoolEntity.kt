package com.babylon.wallet.android.data.repository.cache.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.babylon.wallet.android.data.gateway.extensions.amountDecimal
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItem
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItemDetails
import com.babylon.wallet.android.data.repository.cache.database.ResourceEntity.Companion.asEntity
import com.babylon.wallet.android.domain.model.resources.Pool
import com.babylon.wallet.android.domain.model.resources.Resource
import rdx.works.core.InstantGenerator

@Entity
data class PoolEntity(
    @PrimaryKey
    val address: String
) {

    companion object {
        fun Map<StateEntityDetailsResponseItem, Map<String, StateEntityDetailsResponseItemDetails>>.asPools(): List<Pool> = map { entry ->
            val poolDetails = entry.key
            val itemsInPool = entry.value

            val resourcesInPool = poolDetails.fungibleResources?.items.orEmpty().mapNotNull { fungibleItem ->
                val itemDetails = itemsInPool[fungibleItem.resourceAddress] ?: return@mapNotNull null
                fungibleItem
                    .asEntity(InstantGenerator(), itemDetails)
                    .toResource(fungibleItem.amountDecimal) as Resource.FungibleResource
            }

            Pool(
                address = poolDetails.address,
                resources = resourcesInPool
            )
        }

        fun List<Pool>.toPoolsJoin(syncInfo: SyncInfo): Map<PoolEntity, List<Pair<PoolResourceJoin, ResourceEntity>>> {
            val poolsWithResources = mutableMapOf<PoolEntity, List<Pair<PoolResourceJoin, ResourceEntity>>>()
            forEach { entry ->
                val resourcesInPool = entry.resources.map { item ->
                    PoolResourceJoin(
                        poolAddress = entry.address,
                        resourceAddress = item.resourceAddress,
                        amount = item.ownedAmount,
                        stateVersion = syncInfo.accountStateVersion,
                    ) to item.asEntity(syncInfo.synced)
                }
                poolsWithResources[PoolEntity(entry.address)] = resourcesInPool
            }

            return poolsWithResources
        }

    }

}

