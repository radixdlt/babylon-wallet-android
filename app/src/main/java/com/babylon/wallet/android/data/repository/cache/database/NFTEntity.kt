package com.babylon.wallet.android.data.repository.cache.database

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import com.babylon.wallet.android.data.gateway.extensions.claimAmount
import com.babylon.wallet.android.data.gateway.extensions.claimEpoch
import com.babylon.wallet.android.data.gateway.extensions.image
import com.babylon.wallet.android.data.gateway.extensions.name
import com.babylon.wallet.android.data.gateway.extensions.stringMetadata
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleDetailsResponseItem
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.metadata.ClaimAmountMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.ClaimEpochMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.IconUrlMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.NameMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.StringMetadataItem
import java.math.BigDecimal
import java.time.Instant

@Entity(primaryKeys = ["address", "local_id"])
data class NFTEntity(
    val address: String,
    @ColumnInfo("local_id")
    val localId: String,
    val name: String?,
    @ColumnInfo("image_url")
    val imageUrl: String?,
    @ColumnInfo("claim_amount")
    val claimAmount: BigDecimal?,
    @ColumnInfo("claim_epoch")
    val claimEpoch: Long?,
    @ColumnInfo("metadata")
    val metadata: StringMetadataColumn?,
    val synced: Instant
) {

    fun toItem() = Resource.NonFungibleResource.Item(
        collectionAddress = address,
        localId = Resource.NonFungibleResource.Item.ID.from(localId),
        nameMetadataItem = name?.let { NameMetadataItem(it) },
        iconMetadataItem = imageUrl?.let { IconUrlMetadataItem(Uri.parse(it)) },
        claimEpochMetadataItem = claimEpoch?.let { ClaimEpochMetadataItem(it) },
        claimAmountMetadataItem = claimAmount?.let { ClaimAmountMetadataItem(it) },
        remainingMetadata = metadata?.metadata.orEmpty().map { StringMetadataItem(it.first, it.second) }
    )

    companion object {
        fun StateNonFungibleDetailsResponseItem.asEntity(
            resourceAddress: String,
            syncInfo: SyncInfo
        ): NFTEntity = NFTEntity(
            address = resourceAddress,
            localId = nonFungibleId,
            name = name()?.name,
            imageUrl = image()?.url?.toString(),
            claimAmount = claimAmount()?.amount,
            claimEpoch = claimEpoch()?.claimEpoch,
            metadata = stringMetadata()?.let { metadata ->
                StringMetadataColumn(metadata.map { it.key to it.value })
            },
            synced = syncInfo.synced
        )
    }

}
