package com.babylon.wallet.android.data.repository.cache.database

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.babylon.wallet.android.domain.model.assets.ValidatorDetail
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
) {

    fun asValidatorDetail() = ValidatorDetail(
        address = address,
        name = name.orEmpty(),
        url = iconUrl?.let { Uri.parse(it) },
        description = description,
        totalXrdStake = totalStake,
        stakeUnitResourceAddress = stakeUnitResourceAddress,
        claimTokenResourceAddress = claimTokenResourceAddress
    )

}
