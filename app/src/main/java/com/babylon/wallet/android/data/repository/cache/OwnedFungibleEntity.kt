package com.babylon.wallet.android.data.repository.cache

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.babylon.wallet.android.data.gateway.extensions.amountDecimal
import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollectionItem
import com.babylon.wallet.android.data.repository.cache.FungibleResourceEntity.Companion.asFungibleEntity
import java.math.BigDecimal
import java.time.Instant

@Entity(primaryKeys = ["account_address", "resource_address"])
data class OwnedFungibleEntity(
    @ColumnInfo("account_address")
    val accountAddress: String,
    @ColumnInfo("resource_address")
    val resourceAddress: String,
    val amount: BigDecimal,
    override val synced: Instant,
    override val epoch: Long
): CachedEntity {

    companion object {
        fun FungibleResourcesCollectionItem.asEntityPair(
            accountAddress: String,
            syncInfo: SyncInfo
        ): Pair<OwnedFungibleEntity, FungibleResourceEntity> {
            return OwnedFungibleEntity(
                accountAddress = accountAddress,
                resourceAddress = resourceAddress,
                amount = amountDecimal,
                synced = syncInfo.synced,
                epoch = syncInfo.epoch
            ) to asFungibleEntity(syncInfo)
        }
    }

}
