package com.babylon.wallet.android.data.repository.cache.database

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import com.babylon.wallet.android.data.gateway.extensions.amount
import com.babylon.wallet.android.data.gateway.extensions.amountDecimal
import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollectionItem
import com.babylon.wallet.android.data.gateway.generated.models.NonFungibleResourcesCollectionItem
import com.babylon.wallet.android.data.repository.cache.CachedEntity
import com.babylon.wallet.android.data.repository.cache.database.ResourceEntity.Companion.asEntity
import java.math.BigDecimal
import java.time.Instant

@Entity(
    primaryKeys = ["account_address", "resource_address"],
    foreignKeys = [
        ForeignKey(
            entity = AccountDetailsEntity::class,
            parentColumns = ["address"],
            childColumns = ["account_address"]
        ),
        ForeignKey(
            entity = ResourceEntity::class,
            parentColumns = ["address"],
            childColumns = ["resource_address"]
        )
    ]
)
data class AccountResourcesPortfolio(
    @ColumnInfo("account_address")
    val accountAddress: String,
    @ColumnInfo("resource_address")
    val resourceAddress: String,
    val amount: BigDecimal,
    override val synced: Instant,
    override val epoch: Long
) : CachedEntity {

    companion object {
        fun FungibleResourcesCollectionItem.asEntityPair(
            accountAddress: String,
            syncInfo: SyncInfo
        ): Pair<AccountResourcesPortfolio, ResourceEntity> = AccountResourcesPortfolio(
            accountAddress = accountAddress,
            resourceAddress = resourceAddress,
            amount = amountDecimal,
            synced = syncInfo.synced,
            epoch = syncInfo.epoch
        ) to asEntity(syncInfo)

        fun NonFungibleResourcesCollectionItem.asEntityPair(
            accountAddress: String,
            syncInfo: SyncInfo
        ): Pair<AccountResourcesPortfolio, ResourceEntity> = AccountResourcesPortfolio(
            accountAddress = accountAddress,
            resourceAddress = resourceAddress,
            amount = amount.toBigDecimal(),
            synced = syncInfo.synced,
            epoch = syncInfo.epoch
        ) to asEntity(syncInfo)
    }
}

data class AccountResourceWrapper(
    @ColumnInfo("account_address")
    val address: String,
    val amount: BigDecimal,
    @Embedded
    val resource: ResourceEntity,
    @ColumnInfo("amount_synced")
    val amountSynced: Instant,
    @ColumnInfo("amount_epoch")
    val amountEpoch: Long
)

@Entity(
    primaryKeys = ["account_address", "resource_address", "local_id"],
    foreignKeys = [
        ForeignKey(
            entity = NFTEntity::class,
            parentColumns = ["address", "local_id"],
            childColumns = ["resource_address", "local_id"]
        )
    ]
)
data class AccountNFTsPortfolio(
    @ColumnInfo("account_address")
    val accountAddress: String,
    @ColumnInfo("resource_address")
    val resourceAddress: String,
    @ColumnInfo("local_id")
    val localId: String,
    override val synced: Instant,
    override val epoch: Long
) : CachedEntity
