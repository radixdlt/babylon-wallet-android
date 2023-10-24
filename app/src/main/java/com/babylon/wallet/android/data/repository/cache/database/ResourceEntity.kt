package com.babylon.wallet.android.data.repository.cache.database

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.babylon.wallet.android.data.gateway.extensions.asMetadataItems
import com.babylon.wallet.android.data.gateway.extensions.divisibility
import com.babylon.wallet.android.data.gateway.extensions.extractBehaviours
import com.babylon.wallet.android.data.gateway.extensions.totalSupply
import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollectionItem
import com.babylon.wallet.android.data.gateway.generated.models.NonFungibleResourcesCollectionItem
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseFungibleResourceDetails
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItem
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseNonFungibleResourceDetails
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.metadata.DAppDefinitionsMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.DescriptionMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.IconUrlMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.MetadataItem.Companion.consume
import com.babylon.wallet.android.domain.model.resources.metadata.NameMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.PoolMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.SymbolMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.TagsMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.ValidatorMetadataItem
import java.math.BigDecimal
import java.time.Instant

enum class ResourceEntityType {
    FUNGIBLE,
    NON_FUNGIBLE
}

@Entity
data class ResourceEntity(
    @PrimaryKey val address: String,
    val type: ResourceEntityType,
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
    @ColumnInfo("state_version")
    override val stateVersion: Long
) : CachedEntity {

    @Suppress("CyclomaticComplexMethod")
    fun toResource(amount: BigDecimal): Resource = when (type) {
        ResourceEntityType.FUNGIBLE -> Resource.FungibleResource(
            resourceAddress = address,
            ownedAmount = amount,
            nameMetadataItem = name?.let { NameMetadataItem(it) },
            symbolMetadataItem = symbol?.let { SymbolMetadataItem(it) },
            descriptionMetadataItem = description?.let { DescriptionMetadataItem(it) },
            iconUrlMetadataItem = iconUrl?.let { IconUrlMetadataItem(Uri.parse(it)) },
            tagsMetadataItem = tags?.let { TagsMetadataItem(it.tags) },
            behaviours = behaviours?.behaviours.orEmpty().toSet(),
            currentSupply = supply,
            validatorMetadataItem = validatorAddress?.let { ValidatorMetadataItem(it) },
            poolMetadataItem = poolAddress?.let { PoolMetadataItem(it) },
            dAppDefinitionsMetadataItem = dAppDefinitions?.let { DAppDefinitionsMetadataItem(it.dappDefinitions) },
            divisibility = divisibility
        )
        ResourceEntityType.NON_FUNGIBLE -> Resource.NonFungibleResource(
            resourceAddress = address,
            amount = amount.toLong(),
            nameMetadataItem = name?.let { NameMetadataItem(it) },
            descriptionMetadataItem = description?.let { DescriptionMetadataItem(it) },
            iconMetadataItem = iconUrl?.let { IconUrlMetadataItem(Uri.parse(it)) },
            tagsMetadataItem = tags?.let { TagsMetadataItem(it.tags) },
            behaviours = behaviours?.behaviours.orEmpty().toSet(),
            items = emptyList(),
            currentSupply = supply?.toInt(),
            validatorMetadataItem = validatorAddress?.let { ValidatorMetadataItem(it) },
            dAppDefinitionsMetadataItem = dAppDefinitions?.let { DAppDefinitionsMetadataItem(it.dappDefinitions) },
        )
    }

    companion object {
        // Response from an account state request
        fun FungibleResourcesCollectionItem.asEntity(
            syncInfo: SyncInfo
        ): ResourceEntity {
            val metaDataItems = explicitMetadata?.asMetadataItems().orEmpty().toMutableList()
            return ResourceEntity(
                address = resourceAddress,
                type = ResourceEntityType.FUNGIBLE,
                name = metaDataItems.consume<NameMetadataItem>()?.name,
                symbol = metaDataItems.consume<SymbolMetadataItem>()?.symbol,
                description = metaDataItems.consume<DescriptionMetadataItem>()?.description,
                iconUrl = metaDataItems.consume<IconUrlMetadataItem>()?.url?.toString(),
                tags = metaDataItems.consume<TagsMetadataItem>()?.tags?.let { TagsColumn(tags = it) },
                validatorAddress = metaDataItems.consume<ValidatorMetadataItem>()?.validatorAddress,
                poolAddress = metaDataItems.consume<PoolMetadataItem>()?.poolAddress,
                dAppDefinitions = metaDataItems.consume<DAppDefinitionsMetadataItem>()?.addresses?.let { DappDefinitionsColumn(it) },
                divisibility = null,
                behaviours = null,
                supply = null,
                synced = syncInfo.synced,
                stateVersion = syncInfo.stateVersion
            )
        }

        // Response from an account state request
        fun NonFungibleResourcesCollectionItem.asEntity(
            syncInfo: SyncInfo
        ): ResourceEntity {
            val metaDataItems = explicitMetadata?.asMetadataItems().orEmpty().toMutableList()
            return ResourceEntity(
                address = resourceAddress,
                type = ResourceEntityType.NON_FUNGIBLE,
                name = metaDataItems.consume<NameMetadataItem>()?.name,
                description = metaDataItems.consume<DescriptionMetadataItem>()?.description,
                iconUrl = metaDataItems.consume<IconUrlMetadataItem>()?.url?.toString(),
                tags = metaDataItems.consume<TagsMetadataItem>()?.tags?.let { TagsColumn(tags = it) },
                validatorAddress = metaDataItems.consume<ValidatorMetadataItem>()?.validatorAddress,
                dAppDefinitions = metaDataItems.consume<DAppDefinitionsMetadataItem>()?.addresses?.let { DappDefinitionsColumn(it) },
                behaviours = null,
                supply = null,
                synced = syncInfo.synced,
                divisibility = null,
                poolAddress = null,
                symbol = null,
                stateVersion = syncInfo.stateVersion
            )
        }

        // Response from details request
        fun StateEntityDetailsResponseItem.asResourceEntity(
            syncInfo: SyncInfo
        ): ResourceEntity {
            val metaDataItems = explicitMetadata?.asMetadataItems().orEmpty().toMutableList()
            val type = when (details) {
                is StateEntityDetailsResponseFungibleResourceDetails -> ResourceEntityType.FUNGIBLE
                is StateEntityDetailsResponseNonFungibleResourceDetails -> ResourceEntityType.NON_FUNGIBLE
                else -> ResourceEntityType.FUNGIBLE // TODO CHECK THAT
            }
            return ResourceEntity(
                address = address,
                type = type,
                name = metaDataItems.consume<NameMetadataItem>()?.name,
                symbol = metaDataItems.consume<SymbolMetadataItem>()?.symbol,
                description = metaDataItems.consume<DescriptionMetadataItem>()?.description,
                iconUrl = metaDataItems.consume<IconUrlMetadataItem>()?.url?.toString(),
                tags = metaDataItems.consume<TagsMetadataItem>()?.tags?.let { TagsColumn(tags = it) },
                validatorAddress = metaDataItems.consume<ValidatorMetadataItem>()?.validatorAddress,
                poolAddress = metaDataItems.consume<PoolMetadataItem>()?.poolAddress,
                dAppDefinitions = metaDataItems.consume<DAppDefinitionsMetadataItem>()?.addresses?.let { DappDefinitionsColumn(it) },
                divisibility = details?.divisibility(),
                behaviours = details?.extractBehaviours()?.let { BehavioursColumn(it.toList()) },
                supply = details?.totalSupply()?.toBigDecimal(),
                synced = syncInfo.synced,
                stateVersion = syncInfo.stateVersion
            )
        }
    }
}
