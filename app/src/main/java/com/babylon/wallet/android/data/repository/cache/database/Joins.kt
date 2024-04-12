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
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleDetailsResponseItem
import com.babylon.wallet.android.data.repository.cache.database.NFTEntity.Companion.asEntity
import com.babylon.wallet.android.data.repository.cache.database.ResourceEntity.Companion.asEntity
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.NonFungibleLocalId
import com.radixdlt.sargon.PoolAddress
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.VaultAddress
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.toDecimal192

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
    val accountAddress: AccountAddress,
    @ColumnInfo("resource_address", index = true)
    val resourceAddress: ResourceAddress,
    val amount: Decimal192,
    @ColumnInfo("state_version")
    val stateVersion: Long,
    @ColumnInfo("vault_address")
    val vaultAddress: VaultAddress?,

    // Only needed for non fungible owners were we have fetched data
    // until a specific page but we need to request the next pages
    // in later time
    @ColumnInfo("next_cursor")
    val nextCursor: String?
) {

    companion object {
        fun FungibleResourcesCollectionItem.asAccountResourceJoin(
            accountAddress: AccountAddress,
            syncInfo: SyncInfo
        ): Pair<AccountResourceJoin, ResourceEntity> = AccountResourceJoin(
            accountAddress = accountAddress,
            resourceAddress = ResourceAddress.init(resourceAddress),
            amount = amountDecimal,
            stateVersion = syncInfo.accountStateVersion,
            vaultAddress = vaultAddress?.let { VaultAddress.init(it) },
            nextCursor = null
        ) to asEntity(syncInfo.synced)

        fun NonFungibleResourcesCollectionItem.asAccountResourceJoin(
            accountAddress: AccountAddress,
            syncInfo: SyncInfo
        ): Pair<AccountResourceJoin, ResourceEntity> = AccountResourceJoin(
            accountAddress = accountAddress,
            resourceAddress = ResourceAddress.init(resourceAddress),
            amount = amount.toDecimal192(),
            stateVersion = syncInfo.accountStateVersion,
            vaultAddress = vaultAddress?.let { VaultAddress.init(it) },
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
    val accountAddress: AccountAddress,
    @ColumnInfo("resource_address")
    val resourceAddress: ResourceAddress,
    @ColumnInfo("local_id")
    val localId: NonFungibleLocalId,
    @ColumnInfo("state_version")
    val stateVersion: Long
) {

    companion object {
        fun StateNonFungibleDetailsResponseItem.asAccountNFTJoin(
            accountAddress: AccountAddress,
            resourceAddress: ResourceAddress,
            syncInfo: SyncInfo
        ): Pair<AccountNFTJoin, NFTEntity> = AccountNFTJoin(
            accountAddress = accountAddress,
            resourceAddress = resourceAddress,
            localId = NonFungibleLocalId.init(nonFungibleId),
            stateVersion = syncInfo.accountStateVersion
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
    ],
    indices = [
        Index("resource_address")
    ]
)
data class PoolResourceJoin(
    @ColumnInfo("pool_address")
    val poolAddress: PoolAddress,
    @ColumnInfo("resource_address")
    val resourceAddress: ResourceAddress,
    val amount: Decimal192?,
    @ColumnInfo("state_version")
    val stateVersion: Long
) {

    companion object {
        fun FungibleResourcesCollectionItem.asPoolResourceJoin(
            poolAddress: PoolAddress,
            syncInfo: SyncInfo
        ): Pair<PoolResourceJoin, ResourceEntity> = PoolResourceJoin(
            poolAddress = poolAddress,
            resourceAddress = ResourceAddress.init(resourceAddress),
            amount = amountDecimal,
            stateVersion = syncInfo.accountStateVersion
        ) to asEntity(syncInfo.synced)
    }
}

@Entity(
    primaryKeys = ["pool_address", "dApp_definition_address"],
    foreignKeys = [
        ForeignKey(
            entity = PoolEntity::class,
            parentColumns = ["address"],
            childColumns = ["pool_address"]
        ),
        ForeignKey(
            entity = DAppEntity::class,
            parentColumns = ["definition_address"],
            childColumns = ["dApp_definition_address"]
        )
    ],
    indices = [
        Index("dApp_definition_address")
    ]
)
data class PoolDAppJoin(
    @ColumnInfo("pool_address")
    val poolAddress: PoolAddress,
    @ColumnInfo("dApp_definition_address")
    val dAppDefinitionAddress: AccountAddress,
)
