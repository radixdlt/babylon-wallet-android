package com.babylon.wallet.android.data.repository.cache.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.babylon.wallet.android.data.gateway.extensions.amount
import com.babylon.wallet.android.data.gateway.extensions.amountDecimal
import com.babylon.wallet.android.data.gateway.extensions.vaultAddress
import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollectionItem
import com.babylon.wallet.android.data.gateway.generated.models.NonFungibleResourcesCollectionItem
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItemDetails
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleDetailsResponseItem
import com.babylon.wallet.android.data.repository.cache.database.NFTEntity.Companion.asEntity
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
    val amount: BigDecimal,
    @ColumnInfo("account_state_version")
    val accountStateVersion: Long,

    // Owners of non fungible collections need to store
    // the vault address so we can query their owned ids
    @ColumnInfo("vault_address")
    val vaultAddress: String?,

    // Only needed for non fungible owners were we have fetched data
    // until a specific page but we need to request the next pages
    // in later time
    @ColumnInfo("next_cursor")
    val nextCursor: String?
) {

    companion object {
        fun FungibleResourcesCollectionItem.asAccountResourceJoin(
            accountAddress: String,
            syncInfo: SyncInfo
        ): Pair<AccountResourceJoin, ResourceEntity> = AccountResourceJoin(
            accountAddress = accountAddress,
            resourceAddress = resourceAddress,
            amount = amountDecimal,
            accountStateVersion = syncInfo.accountStateVersion,
            vaultAddress = null,
            nextCursor = null
        ) to asEntity(syncInfo.synced)

        fun NonFungibleResourcesCollectionItem.asAccountResourceJoin(
            accountAddress: String,
            syncInfo: SyncInfo
        ): Pair<AccountResourceJoin, ResourceEntity> = AccountResourceJoin(
            accountAddress = accountAddress,
            resourceAddress = resourceAddress,
            amount = amount.toBigDecimal(),
            accountStateVersion = syncInfo.accountStateVersion,
            vaultAddress = vaultAddress,
            nextCursor = null
        ) to asEntity(syncInfo.synced)
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
    val localId: String,
    @ColumnInfo("account_state_version")
    val accountStateVersion: Long
) {

    companion object {
        fun StateNonFungibleDetailsResponseItem.asAccountNFTJoin(
            accountAddress: String,
            resourceAddress: String,
            syncInfo: SyncInfo
        ): Pair<AccountNFTJoin, NFTEntity> = AccountNFTJoin(
            accountAddress = accountAddress,
            resourceAddress = resourceAddress,
            localId = nonFungibleId,
            accountStateVersion = syncInfo.accountStateVersion
        ) to asEntity(resourceAddress, syncInfo.synced)
    }

}

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
    val amount: BigDecimal?,
    @ColumnInfo("account_state_version")
    val accountStateVersion: Long
) {

    companion object {
        fun FungibleResourcesCollectionItem.asPoolResourceJoin(
            poolAddress: String,
            details: StateEntityDetailsResponseItemDetails,
            syncInfo: SyncInfo
        ): Pair<PoolResourceJoin, ResourceEntity> = PoolResourceJoin(
            poolAddress = poolAddress,
            resourceAddress = resourceAddress,
            amount = amountDecimal,
            accountStateVersion = syncInfo.accountStateVersion
        ) to asEntity(syncInfo.synced, details)
    }

}
