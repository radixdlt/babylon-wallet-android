package com.babylon.wallet.android.data.repository.cache.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.babylon.wallet.android.data.gateway.extensions.toMetadata
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleDetailsResponseItem
import com.babylon.wallet.android.domain.model.resources.Resource
import java.time.Instant

@Entity(primaryKeys = ["address", "local_id"])
data class NFTEntity(
    val address: String,
    @ColumnInfo("local_id")
    val localId: String,
    @ColumnInfo("metadata")
    val metadata: MetadataColumn?,
    val synced: Instant
) {

    fun toItem() = Resource.NonFungibleResource.Item(
        collectionAddress = address,
        localId = Resource.NonFungibleResource.Item.ID.from(localId),
        metadata = metadata?.metadata.orEmpty()
    )

    companion object {
        fun StateNonFungibleDetailsResponseItem.asEntity(
            resourceAddress: String,
            synced: Instant
        ): NFTEntity {
            return NFTEntity(
                address = resourceAddress,
                localId = nonFungibleId,
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
            localId = localId.code,
            metadata = metadata.takeIf {
                it.isNotEmpty()
            }?.let {
                MetadataColumn(it)
            },
            synced = synced
        )
    }
}
