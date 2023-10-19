package com.babylon.wallet.android.data.repository.cache

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.babylon.wallet.android.data.gateway.extensions.amount
import com.babylon.wallet.android.data.gateway.generated.models.NonFungibleResourcesCollectionItem
import com.babylon.wallet.android.data.repository.cache.NonFungibleResourceEntity.Companion.asEntity
import java.time.Instant

@Entity(primaryKeys = ["account_address", "resource_address"])
data class OwnedNonFungibleEntity(
    @ColumnInfo("account_address")
    val accountAddress: String,
    @ColumnInfo("resource_address")
    val resourceAddress: String,
    val amount: Long,
    @ColumnInfo("nft_ids")
    val nftsIds: NFTIdsColumn?,
    override val synced: Instant,
    override val epoch: Long
): CachedEntity {

    companion object {
        fun NonFungibleResourcesCollectionItem.asEntityPair(
            accountAddress: String,
            syncInfo: SyncInfo
        ): Pair<OwnedNonFungibleEntity, NonFungibleResourceEntity> {
            return OwnedNonFungibleEntity(
                accountAddress = accountAddress,
                resourceAddress = resourceAddress,
                amount = amount,
                nftsIds = null,
                synced = syncInfo.synced,
                epoch = syncInfo.epoch
            ) to asEntity(syncInfo)
        }
    }

}
