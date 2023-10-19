package com.babylon.wallet.android.data.repository.cache

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.babylon.wallet.android.data.gateway.extensions.asMetadataItems
import com.babylon.wallet.android.data.gateway.extensions.divisibility
import com.babylon.wallet.android.data.gateway.extensions.extractBehaviours
import com.babylon.wallet.android.data.gateway.extensions.totalSupply
import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollectionItem
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItem
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.metadata.DAppDefinitionsMetadataItem
import com.babylon.wallet.android.domain.model.metadata.DescriptionMetadataItem
import com.babylon.wallet.android.domain.model.metadata.IconUrlMetadataItem
import com.babylon.wallet.android.domain.model.metadata.MetadataItem.Companion.consume
import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem
import com.babylon.wallet.android.domain.model.metadata.PoolMetadataItem
import com.babylon.wallet.android.domain.model.metadata.SymbolMetadataItem
import com.babylon.wallet.android.domain.model.metadata.TagsMetadataItem
import com.babylon.wallet.android.domain.model.metadata.ValidatorMetadataItem
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
    override val synced: Instant,
    override val epoch: Long
): CachedEntity {

    fun toResource(withOwnedAmount: BigDecimal? = null) = Resource.FungibleResource(
        resourceAddress = address,
        ownedAmount = withOwnedAmount,
        nameMetadataItem = name?.let { NameMetadataItem(it) },
        symbolMetadataItem = symbol?.let { SymbolMetadataItem(it) },
        descriptionMetadataItem = description?.let { DescriptionMetadataItem(it) },
        iconUrlMetadataItem = iconUrl?.let { IconUrlMetadataItem(Uri.parse(it)) },
        tagsMetadataItem = tags?.let { TagsMetadataItem(tags = it.tags) },
        behaviours = behaviours?.behaviours?.toSet().orEmpty(),
        currentSupply = supply,
        validatorMetadataItem = validatorAddress?.let { ValidatorMetadataItem(it) },
        poolMetadataItem = poolAddress?.let { PoolMetadataItem(it) },
        dAppDefinitionsMetadataItem = dAppDefinitions?.let { DAppDefinitionsMetadataItem(it.dappDefinitions) },
        divisibility = divisibility
    )

    companion object {
        // Response from an account state request
        fun FungibleResourcesCollectionItem.asFungibleEntity(
            syncInfo: SyncInfo
        ): FungibleResourceEntity {
            val metaDataItems = explicitMetadata?.asMetadataItems().orEmpty().toMutableList()
            return FungibleResourceEntity(
                address = resourceAddress,
                name = metaDataItems.consume<NameMetadataItem>()?.name,
                symbol = metaDataItems.consume<SymbolMetadataItem>()?.symbol,
                description = metaDataItems.consume<DescriptionMetadataItem>()?.description,
                iconUrl = metaDataItems.consume<IconUrlMetadataItem>()?.url.toString(),
                tags = metaDataItems.consume<TagsMetadataItem>()?.tags?.let { TagsColumn(tags = it) },
                validatorAddress = metaDataItems.consume<ValidatorMetadataItem>()?.validatorAddress,
                poolAddress = metaDataItems.consume<PoolMetadataItem>()?.poolAddress,
                dAppDefinitions = metaDataItems.consume<DAppDefinitionsMetadataItem>()?.addresses?.let { DappDefinitionsColumn(it) },
                divisibility = null,
                behaviours = null,
                supply = null,
                synced = syncInfo.synced,
                epoch = syncInfo.epoch
            )
        }

        // Response from details request
        fun StateEntityDetailsResponseItem.asEntity(
            syncInfo: SyncInfo
        ): FungibleResourceEntity {
            val metaDataItems = explicitMetadata?.asMetadataItems().orEmpty().toMutableList()
            return FungibleResourceEntity(
                address = address,
                name = metaDataItems.consume<NameMetadataItem>()?.name,
                symbol = metaDataItems.consume<SymbolMetadataItem>()?.symbol,
                description = metaDataItems.consume<DescriptionMetadataItem>()?.description,
                iconUrl = metaDataItems.consume<IconUrlMetadataItem>()?.url.toString(),
                tags = metaDataItems.consume<TagsMetadataItem>()?.tags?.let { TagsColumn(tags = it) },
                validatorAddress = metaDataItems.consume<ValidatorMetadataItem>()?.validatorAddress,
                poolAddress = metaDataItems.consume<PoolMetadataItem>()?.poolAddress,
                dAppDefinitions = metaDataItems.consume<DAppDefinitionsMetadataItem>()?.addresses?.let { DappDefinitionsColumn(it) },
                divisibility = details?.divisibility(),
                behaviours = details?.extractBehaviours()?.let { BehavioursColumn(it.toList()) },
                supply = details?.totalSupply()?.toBigDecimal(),
                synced = syncInfo.synced,
                epoch = syncInfo.epoch
            )
        }
    }

}
