package com.babylon.wallet.android.data.repository.cache

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.Instant

@Entity
data class FungibleResourceEntity(
    @PrimaryKey val address: String,
    val name: String?,
    val symbol: String?,
    val description: String?,
    @ColumnInfo("icon_url")
    val iconUrl: String?,
    val tags: TagsColumn?,
    @ColumnInfo("validator_address")
    val validatorAddress: String?,
    @ColumnInfo("pool_address")
    val poolAddress: String?,
    @ColumnInfo("dapp_definitions")
    val dAppDefinitions: DappDefinitionsColumn?,
    val divisibility: Int?,
    val behaviours: BehavioursColumn?,
    val supply: BigDecimal?,
    @ColumnInfo("updated_at")
    override val updatedAt: Instant,
    override val epoch: Long
): CachedEntity
