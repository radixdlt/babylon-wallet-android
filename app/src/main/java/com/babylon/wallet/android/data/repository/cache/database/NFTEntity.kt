package com.babylon.wallet.android.data.repository.cache.database

import androidx.room.ColumnInfo
import androidx.room.Entity
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
    override val synced: Instant
) : CachedEntity
