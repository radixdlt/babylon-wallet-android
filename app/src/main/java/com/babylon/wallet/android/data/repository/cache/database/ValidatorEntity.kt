package com.babylon.wallet.android.data.repository.cache.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.babylon.wallet.android.data.gateway.extensions.claimTokenResourceAddress
import com.babylon.wallet.android.data.gateway.extensions.stakeUnitResourceAddress
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
        fun StateEntityDetailsResponseItem.asValidatorEntity(syncInfo: SyncInfo) = ValidatorEntity(
            address = ValidatorAddress.init(address),
            stakeUnitResourceAddress = details?.stakeUnitResourceAddress?.let { ResourceAddress.init(it) },
            claimTokenResourceAddress = details?.claimTokenResourceAddress?.let { ResourceAddress.init(it) },
            totalStake = totalXRDStake,
            metadata = MetadataColumn.from(
                explicitMetadata = explicitMetadata,
                implicitMetadata = metadata
            ),
            stateVersion = syncInfo.accountStateVersion
        )
    }
}
