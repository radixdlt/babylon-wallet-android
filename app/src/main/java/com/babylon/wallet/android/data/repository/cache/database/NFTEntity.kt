package com.babylon.wallet.android.data.repository.cache.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.babylon.wallet.android.data.gateway.extensions.toMetadata
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleDetailsResponseItem
import com.radixdlt.sargon.NonFungibleLocalId
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.extensions.init
import rdx.works.core.domain.resources.Resource
import java.time.Instant

@Entity(primaryKeys = ["address", "local_id"])
data class NFTEntity(
    val address: ResourceAddress,
    @ColumnInfo("local_id")
    val localId: NonFungibleLocalId,
    @ColumnInfo("metadata")
    val metadata: MetadataColumn?,
    val synced: Instant
) {

    fun toItem() = Resource.NonFungibleResource.Item(
        collectionAddress = address,
        localId = localId,
        metadata = metadata?.metadata.orEmpty()
    )

    companion object {
        fun StateNonFungibleDetailsResponseItem.asEntity(
            resourceAddress: ResourceAddress,
            synced: Instant
        ): NFTEntity {
            return NFTEntity(
                address = resourceAddress,
                localId = NonFungibleLocalId.init(nonFungibleId),
                metadata = toMetadata().takeIf {
                    it.isNotEmpty()
                }?.let {
                    MetadataColumn(it)
                },
                synced = synced
            )
        }

        fun Resource.NonFungibleResource.Item.asEntity(synced: Instant) = NFTEntity(
            address = collectionAddress,
            localId = localId,
            metadata = metadata.takeIf {
                it.isNotEmpty()
            }?.let {
                MetadataColumn(it)
            },
            synced = synced
        )
    }
}
