package com.babylon.wallet.android.data.repository.cache.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.babylon.wallet.android.data.gateway.extensions.amount
import com.babylon.wallet.android.data.gateway.extensions.amountDecimal
import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollectionItem
import com.babylon.wallet.android.data.gateway.generated.models.NonFungibleResourcesCollectionItem
import com.babylon.wallet.android.data.repository.cache.database.ResourceEntity.Companion.asEntity
import java.math.BigDecimal

@Entity(
    primaryKeys = ["account_address", "resource_address"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["address"],
            childColumns = ["account_address"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ResourceEntity::class,
            parentColumns = ["address"],
            childColumns = ["resource_address"]
        )
    ]
)
data class AccountResourceJoin(
    @ColumnInfo("account_address")
    val accountAddress: String,
    @ColumnInfo("resource_address", index = true)
    val resourceAddress: String,
    val amount: BigDecimal
) {

    companion object {
        fun FungibleResourcesCollectionItem.asAccountResourceJoin(
            accountAddress: String,
            syncInfo: SyncInfo
        ): Pair<AccountResourceJoin, ResourceEntity> = AccountResourceJoin(
            accountAddress = accountAddress,
            resourceAddress = resourceAddress,
            amount = amountDecimal
        ) to asEntity(syncInfo)

        fun NonFungibleResourcesCollectionItem.asAccountResourceJoin(
            accountAddress: String,
            syncInfo: SyncInfo
        ): Pair<AccountResourceJoin, ResourceEntity> = AccountResourceJoin(
            accountAddress = accountAddress,
            resourceAddress = resourceAddress,
            amount = amount.toBigDecimal()
        ) to asEntity(syncInfo)
    }
}

@Entity(
    primaryKeys = ["account_address", "resource_address", "local_id"],
    indices = [Index("resource_address", "local_id")],
    foreignKeys = [
        ForeignKey(
            entity = NFTEntity::class,
            parentColumns = ["address", "local_id"],
            childColumns = ["resource_address", "local_id"]
        )
    ]
)
data class AccountNFTJoin(
    @ColumnInfo("account_address")
    val accountAddress: String,
    @ColumnInfo("resource_address")
    val resourceAddress: String,
    @ColumnInfo("local_id")
    val localId: String
)

@Entity(
    primaryKeys = ["pool_address", "resource_address"],
    foreignKeys = [
        ForeignKey(
            entity = PoolEntity::class,
            parentColumns = ["address"],
            childColumns = ["pool_address"]
        ),
        ForeignKey(
            entity = ResourceEntity::class,
            parentColumns = ["address"],
            childColumns = ["resource_address"]
        )
    ]
)
data class PoolResourceJoin(
    @ColumnInfo("pool_address")
    val poolAddress: String,
    @ColumnInfo("resource_address")
    val resourceAddress: String,
    val amount: BigDecimal?
) {

    companion object {
        fun FungibleResourcesCollectionItem.asPoolResourceJoin(
            poolAddress: String,
            syncInfo: SyncInfo
        ): Pair<PoolResourceJoin, ResourceEntity> = PoolResourceJoin(
            poolAddress = poolAddress,
            resourceAddress = resourceAddress,
            amount = amountDecimal
        ) to asEntity(syncInfo)
    }

}
