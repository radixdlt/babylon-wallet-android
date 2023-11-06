package com.babylon.wallet.android.data.repository.cache.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollectionItem
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItem
import com.babylon.wallet.android.data.repository.cache.database.PoolResourceJoin.Companion.asPoolResourceJoin
import com.babylon.wallet.android.data.repository.cache.database.ResourceEntity.Companion.asEntity

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = ResourceEntity::class,
            parentColumns = ["address"],
            childColumns = ["resource_address"]
        )
    ]
)
data class PoolEntity(
    @PrimaryKey
    val address: String,
    @ColumnInfo("resource_address")
    val resourceAddress: String
) {

    companion object {
        @Suppress("UnsafeCallOnNullableType")
        fun Map<StateEntityDetailsResponseItem, List<FungibleResourcesCollectionItem>>.asPoolsResourcesJoin(
            syncInfo: SyncInfo
        ): Map<ResourceEntity, List<Pair<PoolResourceJoin, ResourceEntity>>> =
            map { (poolResource, itemsInPool) ->
                val poolResourceEntity = poolResource.asEntity(syncInfo.synced)

                val resourcesInPool = itemsInPool.map { fungibleItem ->
                    fungibleItem.asPoolResourceJoin(poolAddress = poolResourceEntity.poolAddress!!, syncInfo)
                }

                poolResourceEntity to resourcesInPool
            }.toMap()
    }
}
