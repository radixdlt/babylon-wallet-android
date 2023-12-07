package com.babylon.wallet.android.data.repository.cache.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.babylon.wallet.android.data.gateway.extensions.claimTokenResourceAddress
import com.babylon.wallet.android.data.gateway.extensions.stakeUnitResourceAddress
import com.babylon.wallet.android.data.gateway.extensions.toMetadata
import com.babylon.wallet.android.data.gateway.extensions.totalXRDStake
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItem
import com.babylon.wallet.android.domain.model.assets.ValidatorDetail
import java.math.BigDecimal

@Entity
data class ValidatorEntity(
    @PrimaryKey
    val address: String,
    @ColumnInfo("stake_unit_resource_address")
    val stakeUnitResourceAddress: String?,
    @ColumnInfo("claim_token_resource_address")
    val claimTokenResourceAddress: String?,
    @ColumnInfo("total_stake")
    val totalStake: BigDecimal?,
    val metadata: MetadataColumn?,
    @ColumnInfo("state_version")
    val stateVersion: Long
) {

    fun asValidatorDetail() = ValidatorDetail(
        address = address,
        totalXrdStake = totalStake,
        stakeUnitResourceAddress = stakeUnitResourceAddress,
        claimTokenResourceAddress = claimTokenResourceAddress,
        metadata = metadata?.metadata.orEmpty()
    )

    companion object {
        fun ValidatorDetail.asValidatorEntity(syncInfo: SyncInfo) = ValidatorEntity(
            address = address,
            stakeUnitResourceAddress = stakeUnitResourceAddress,
            claimTokenResourceAddress = claimTokenResourceAddress,
            totalStake = totalXrdStake,
            metadata = metadata.takeIf { it.isNotEmpty() }?.let { MetadataColumn(it) },
            stateVersion = syncInfo.accountStateVersion
        )

        fun List<StateEntityDetailsResponseItem>.asValidators() = map { item ->
            val metadata = item.explicitMetadata?.toMetadata().orEmpty()
            ValidatorDetail(
                address = item.address,
                totalXrdStake = item.totalXRDStake,
                stakeUnitResourceAddress = item.details?.stakeUnitResourceAddress.orEmpty(),
                claimTokenResourceAddress = item.details?.claimTokenResourceAddress.orEmpty(),
                metadata = metadata
            )
        }

        fun List<ValidatorDetail>.asValidatorEntities(syncInfo: SyncInfo) = map { item ->
            item.asValidatorEntity(syncInfo)
        }
    }
}
