package com.babylon.wallet.android.data.repository.cache

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.material.search.SearchView.Behavior
import java.math.BigDecimal
import java.time.Instant

@Entity
data class NonFungibleResourceEntity(
    @PrimaryKey val address: String,
    val name: String?,
    val description: String?,
    @ColumnInfo("icon_url")
    val iconUrl: String?,
    val tags: TagsColumn?,
    @ColumnInfo("validator_address")
    val validatorAddress: String?,
    @ColumnInfo("dapp_definitions")
    val dAppDefinitions: DappDefinitionsColumn?,
    val behaviours: BehavioursColumn?,
    val supply: Int?,
    @ColumnInfo("updated_at")
    override val updatedAt: Instant,
    override val epoch: Long
): CachedEntity
