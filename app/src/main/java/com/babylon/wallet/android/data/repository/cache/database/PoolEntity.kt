package com.babylon.wallet.android.data.repository.cache.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItem
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItemDetails
import com.babylon.wallet.android.data.repository.cache.database.PoolResourceJoin.Companion.asPoolResourceJoin

@Entity
data class PoolEntity(
    @PrimaryKey
    val address: String
) {

    companion object {

        fun Map<StateEntityDetailsResponseItem, Map<String, StateEntityDetailsResponseItemDetails>>.asPoolsWithResources(
            syncInfo: SyncInfo
        ): Map<PoolEntity, List<Pair<PoolResourceJoin, ResourceEntity>>> {
            val poolsWithResources = mutableMapOf<PoolEntity, List<Pair<PoolResourceJoin, ResourceEntity>>>()

            forEach { entry ->
                val poolDetails = entry.key
                val itemsInPool = entry.value

                val resourcesInPool = poolDetails.fungibleResources?.items.orEmpty().mapNotNull { fungibleItem ->
                    val itemDetails = itemsInPool[fungibleItem.resourceAddress] ?: return@mapNotNull null
                    fungibleItem.asPoolResourceJoin(poolDetails.address, itemDetails, syncInfo)
                }

                val poolEntity = PoolEntity(poolDetails.address)
                poolsWithResources[poolEntity] = resourcesInPool
            }

            return poolsWithResources
        }

    }

}

