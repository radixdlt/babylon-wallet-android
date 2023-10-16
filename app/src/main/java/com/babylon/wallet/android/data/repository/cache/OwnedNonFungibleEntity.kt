package com.babylon.wallet.android.data.repository.cache

import androidx.room.ColumnInfo
import androidx.room.Entity
import java.math.BigDecimal
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
    @ColumnInfo("updated_at")
    override val updatedAt: Instant,
    override val epoch: Long
): CachedEntity
