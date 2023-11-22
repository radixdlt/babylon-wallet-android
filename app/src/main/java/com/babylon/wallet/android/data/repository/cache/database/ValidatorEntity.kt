package com.babylon.wallet.android.data.repository.cache.database

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.babylon.wallet.android.data.gateway.extensions.asMetadataItems
import com.babylon.wallet.android.data.gateway.extensions.claimTokenResourceAddress
import com.babylon.wallet.android.data.gateway.extensions.stakeUnitResourceAddress
import com.babylon.wallet.android.data.gateway.extensions.totalXRDStake
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItem
import com.babylon.wallet.android.domain.model.assets.ValidatorDetail
import com.babylon.wallet.android.domain.model.resources.metadata.DescriptionMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.IconUrlMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.MetadataItem.Companion.consume
import com.babylon.wallet.android.domain.model.resources.metadata.NameMetadataItem
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

    companion object {
        fun ValidatorDetail.asValidatorEntity(syncInfo: SyncInfo) = ValidatorEntity(
            address = address,
            name = name.takeIf { it.isNotBlank() },
            description = description,
            iconUrl = url?.toString(),
            stakeUnitResourceAddress = stakeUnitResourceAddress,
            claimTokenResourceAddress = claimTokenResourceAddress,
            totalStake = totalXrdStake,
            stateVersion = syncInfo.accountStateVersion
        )

        fun List<StateEntityDetailsResponseItem>.asValidators() = map { item ->
            val metadataItems = item.explicitMetadata?.asMetadataItems().orEmpty().toMutableList()
            ValidatorDetail(
                address = item.address,
                name = metadataItems.consume<NameMetadataItem>()?.name.orEmpty(),
                url = metadataItems.consume<IconUrlMetadataItem>()?.url,
                description = metadataItems.consume<DescriptionMetadataItem>()?.description,
                totalXrdStake = item.totalXRDStake,
                stakeUnitResourceAddress = item.details?.stakeUnitResourceAddress.orEmpty(),
                claimTokenResourceAddress = item.details?.claimTokenResourceAddress.orEmpty()
            )
        }

        fun List<ValidatorDetail>.asValidatorEntities(syncInfo: SyncInfo) = map { item ->
            item.asValidatorEntity(syncInfo)
        }
    }
}
