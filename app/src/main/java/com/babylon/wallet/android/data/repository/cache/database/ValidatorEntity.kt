package com.babylon.wallet.android.data.repository.cache.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.babylon.wallet.android.data.gateway.extensions.claimTokenResourceAddress
import com.babylon.wallet.android.data.gateway.extensions.stakeUnitResourceAddress
import com.babylon.wallet.android.data.gateway.extensions.toMetadata
import com.babylon.wallet.android.data.gateway.extensions.totalXRDStake
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItem
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.ValidatorAddress
import com.radixdlt.sargon.extensions.init
import rdx.works.core.domain.resources.Validator

@Entity
data class ValidatorEntity(
    @PrimaryKey
    val address: ValidatorAddress,
    @ColumnInfo("stake_unit_resource_address")
    val stakeUnitResourceAddress: ResourceAddress?,
    @ColumnInfo("claim_token_resource_address")
    val claimTokenResourceAddress: ResourceAddress?,
    @ColumnInfo("total_stake")
    val totalStake: Decimal192?,
    val metadata: MetadataColumn?,
    @ColumnInfo("state_version")
    val stateVersion: Long
) {

    fun asValidatorDetail() = Validator(
        address = address,
        totalXrdStake = totalStake,
        stakeUnitResourceAddress = stakeUnitResourceAddress,
        claimTokenResourceAddress = claimTokenResourceAddress,
        metadata = metadata?.metadata.orEmpty()
    )

    companion object {
        fun Validator.asValidatorEntity(syncInfo: SyncInfo) = ValidatorEntity(
            address = address,
            stakeUnitResourceAddress = stakeUnitResourceAddress,
            claimTokenResourceAddress = claimTokenResourceAddress,
            totalStake = totalXrdStake,
            metadata = metadata.takeIf { it.isNotEmpty() }?.let { MetadataColumn(it) },
            stateVersion = syncInfo.accountStateVersion
        )

        fun List<StateEntityDetailsResponseItem>.asValidators() = map { item ->
            val metadata = item.explicitMetadata?.toMetadata().orEmpty()
            Validator(
                address = ValidatorAddress.init(item.address),
                totalXrdStake = item.totalXRDStake,
                stakeUnitResourceAddress = item.details?.stakeUnitResourceAddress?.let { ResourceAddress.init(it) },
                claimTokenResourceAddress = item.details?.claimTokenResourceAddress?.let { ResourceAddress.init(it) },
                metadata = metadata
            )
        }

        fun List<Validator>.asValidatorEntities(syncInfo: SyncInfo) = map { item ->
            item.asValidatorEntity(syncInfo)
        }
    }
}
