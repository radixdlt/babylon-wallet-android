package com.babylon.wallet.android.data.repository.cache.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItem
import com.babylon.wallet.android.data.repository.cache.database.PoolResourceJoin.Companion.asPoolResourceJoin

@Entity
data class PoolEntity(
    @PrimaryKey
    val address: String,
    @ColumnInfo("state_version")
    val stateVersion: Long
) {

    companion object {

        fun List<StateEntityDetailsResponseItem>.asPoolsWithResources(
            syncInfo: SyncInfo
        ): Map<PoolEntity, List<Pair<PoolResourceJoin, ResourceEntity>>> {
            val poolsWithResources = associate { pool ->
                val address = pool.address
                val resourcesInPool = pool.fungibleResources?.items.orEmpty().map {
                    it.asPoolResourceJoin(pool.address, syncInfo)
                }

                address to resourcesInPool
            }.mapKeys {
                PoolEntity(it.key, syncInfo.stateVersion)
            }

            return poolsWithResources
        }

    }

}
