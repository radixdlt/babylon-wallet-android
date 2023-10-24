package com.babylon.wallet.android.data.repository.cache.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity
data class ValidatorEntity(
    @PrimaryKey
    val address: String,
    val name: String?,
    val description: String?,
    @ColumnInfo("icon_url")
    val iconUrl: String?,
    @ColumnInfo("stake_unit_resource_address")
    val stakeUnitResourceAddress: String?,
    @ColumnInfo("claim_token_resource_address")
    val claimTokenResourceAddress: String?,
    @ColumnInfo("total_stake")
    val totalStake: BigDecimal?,
    @ColumnInfo("state_version")
    val stateVersion: Long
)
