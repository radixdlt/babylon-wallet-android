package com.babylon.wallet.android.data.repository.cache.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.babylon.wallet.android.data.gateway.extensions.PoolsResponse
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItem
import com.babylon.wallet.android.data.repository.cache.database.PoolResourceJoin.Companion.asPoolResourceJoin
import com.babylon.wallet.android.data.repository.cache.database.ResourceEntity.Companion.asEntity
import com.radixdlt.sargon.PoolAddress
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.extensions.init
import rdx.works.core.domain.resources.metadata.poolUnit

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = ResourceEntity::class,
            parentColumns = ["address"],
            childColumns = ["resource_address"]
        )
    ],
    indices = [
        Index("resource_address")
    ]
)
data class PoolEntity(
    @PrimaryKey
    val address: PoolAddress,
    val metadata: MetadataColumn?,
    @ColumnInfo("resource_address")
    val resourceAddress: ResourceAddress
) {

    companion object {
        @Suppress("UnsafeCallOnNullableType")
        fun List<PoolsResponse.PoolItem>.asPoolsResourcesJoin(
            syncInfo: SyncInfo
        ): List<PoolWithResourcesJoinResult> =
            mapNotNull { fetchedPoolDetails ->
                val poolResourceEntity = fetchedPoolDetails.poolUnitDetails.asEntity(syncInfo.synced)
                val resourcesInPool = fetchedPoolDetails.poolResourcesDetails.map { fungibleItem ->
                    fungibleItem.asPoolResourceJoin(poolAddress = poolResourceEntity.poolAddress!!, syncInfo)
                }
                val poolEntity = fetchedPoolDetails.poolDetails.asPoolEntity()
                val associatedDAppEntity = fetchedPoolDetails.associatedDAppDetails?.let { DAppEntity.from(it, syncInfo.synced) }
                poolEntity?.let {
                    PoolWithResourcesJoinResult(
                        pool = poolEntity,
                        poolUnitResource = poolResourceEntity,
                        associatedDApp = associatedDAppEntity,
                        resources = resourcesInPool
                    )
                }
            }
    }
}

fun StateEntityDetailsResponseItem.asPoolEntity(): PoolEntity? {
    val metadataColumn = MetadataColumn.from(
        explicitMetadata = explicitMetadata,
        implicitMetadata = metadata
    )
    val poolUnitResourceAddress = metadataColumn.metadata.poolUnit() ?: return null
    return PoolEntity(
        address = PoolAddress.init(address),
        metadata = metadataColumn,
        resourceAddress = poolUnitResourceAddress
    )
}

data class PoolWithResourcesJoinResult(
    val pool: PoolEntity,
    val poolUnitResource: ResourceEntity,
    val associatedDApp: DAppEntity?,
    val resources: List<Pair<PoolResourceJoin, ResourceEntity>>
)
