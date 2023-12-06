package com.babylon.wallet.android.data.repository.cache.database

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.babylon.wallet.android.data.gateway.extensions.asMetadataItems
import com.babylon.wallet.android.data.gateway.extensions.divisibility
import com.babylon.wallet.android.data.gateway.extensions.extractBehaviours
import com.babylon.wallet.android.data.gateway.extensions.totalSupply
import com.babylon.wallet.android.data.gateway.generated.models.EntityMetadataCollection
import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollectionItem
import com.babylon.wallet.android.data.gateway.generated.models.NonFungibleResourcesCollectionItem
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseFungibleResourceDetails
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItem
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItemDetails
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
    val synced: Instant
) {

    @Suppress("CyclomaticComplexMethod")
    fun toResource(amount: BigDecimal?): Resource = when (type) {
        ResourceEntityType.FUNGIBLE -> Resource.FungibleResource(
            resourceAddress = address,
            ownedAmount = amount,
            nameMetadataItem = name?.let { NameMetadataItem(it) },
            symbolMetadataItem = symbol?.let { SymbolMetadataItem(it) },
            descriptionMetadataItem = description?.let { DescriptionMetadataItem(it) },
            iconUrlMetadataItem = iconUrl?.let { IconUrlMetadataItem(Uri.parse(it)) },
            tagsMetadataItem = tags?.let { TagsMetadataItem(it.tags) },
            assetBehaviours = behaviours?.behaviours?.toSet(),
            currentSupply = supply,
            validatorMetadataItem = validatorAddress?.let { ValidatorMetadataItem(it) },
            poolMetadataItem = poolAddress?.let { PoolMetadataItem(it) },
            dAppDefinitionsMetadataItem = dAppDefinitions?.let { DAppDefinitionsMetadataItem(it.dappDefinitions) },
            divisibility = divisibility
        )

        ResourceEntityType.NON_FUNGIBLE -> Resource.NonFungibleResource(
            resourceAddress = address,
            amount = amount?.toLong() ?: 0L,
            nameMetadataItem = name?.let { NameMetadataItem(it) },
            descriptionMetadataItem = description?.let { DescriptionMetadataItem(it) },
            iconMetadataItem = iconUrl?.let { IconUrlMetadataItem(Uri.parse(it)) },
            tagsMetadataItem = tags?.let { TagsMetadataItem(it.tags) },
            assetBehaviours = behaviours?.behaviours?.toSet(),
            items = emptyList(),
            currentSupply = supply?.toInt(),
            validatorMetadataItem = validatorAddress?.let { ValidatorMetadataItem(it) },
            dAppDefinitionsMetadataItem = dAppDefinitions?.let { DAppDefinitionsMetadataItem(it.dappDefinitions) },
        )
    }

    companion object {
        fun Resource.asEntity(synced: Instant): ResourceEntity = when (this) {
            is Resource.FungibleResource -> ResourceEntity(
                address = resourceAddress,
                type = ResourceEntityType.FUNGIBLE,
                name = nameMetadataItem?.name,
                symbol = symbolMetadataItem?.symbol,
                description = descriptionMetadataItem?.description,
                iconUrl = iconUrlMetadataItem?.url?.toString(),
                tags = tagsMetadataItem?.tags?.let { TagsColumn(it) },
                validatorAddress = validatorMetadataItem?.address,
                poolAddress = poolMetadataItem?.address,
                dAppDefinitions = dAppDefinitionsMetadataItem?.addresses?.let { DappDefinitionsColumn(it) },
                divisibility = divisibility,
                behaviours = behaviours?.let { BehavioursColumn(it) },
                supply = currentSupply,
                synced = synced
            )

            is Resource.NonFungibleResource -> ResourceEntity(
                address = resourceAddress,
                type = ResourceEntityType.NON_FUNGIBLE,
                name = nameMetadataItem?.name,
                description = descriptionMetadataItem?.description,
                iconUrl = iconMetadataItem?.url?.toString(),
                tags = tagsMetadataItem?.tags?.let { TagsColumn(it) },
                behaviours = behaviours?.let { BehavioursColumn(it) },
                validatorAddress = validatorMetadataItem?.address,
                supply = currentSupply?.toBigDecimal(),
                dAppDefinitions = dAppDefinitionsMetadataItem?.addresses?.let { DappDefinitionsColumn(it) },
                divisibility = null,
                poolAddress = null,
                symbol = null,
                synced = synced
            )
        }

        // Response from an account state request
        fun FungibleResourcesCollectionItem.asEntity(
            synced: Instant,
            // In case we have fetched details for this item
            details: StateEntityDetailsResponseItemDetails? = null
        ): ResourceEntity = from(
            address = resourceAddress,
            metadata = explicitMetadata,
            details = details,
            type = ResourceEntityType.FUNGIBLE,
            synced = synced
        )

        // Response from an account state request
        fun NonFungibleResourcesCollectionItem.asEntity(
            synced: Instant,
            // In case we have fetched details for this item
            details: StateEntityDetailsResponseItemDetails? = null
        ): ResourceEntity = from(
            address = resourceAddress,
            metadata = explicitMetadata,
            details = details,
            type = ResourceEntityType.NON_FUNGIBLE,
            synced = synced
        )

        // When fetching details for specific resource
        fun StateEntityDetailsResponseItem.asEntity(
            synced: Instant
        ): ResourceEntity {
            val type = when (details) {
                is StateEntityDetailsResponseFungibleResourceDetails -> ResourceEntityType.FUNGIBLE
                is StateEntityDetailsResponseNonFungibleResourceDetails -> ResourceEntityType.NON_FUNGIBLE
                else -> error("Item is neither fungible nor non-fungible")
            }
            return from(
                address = address,
                metadata = metadata,
                details = details,
                type = type,
                synced = synced
            )
        }

        private fun from(
            address: String,
            metadata: EntityMetadataCollection?,
            details: StateEntityDetailsResponseItemDetails?,
            type: ResourceEntityType,
            synced: Instant
        ): ResourceEntity {
            val metaDataItems = metadata?.asMetadataItems().orEmpty().toMutableList()
            return ResourceEntity(
                address = address,
                type = type,
                name = metaDataItems.consume<NameMetadataItem>()?.name,
                symbol = metaDataItems.consume<SymbolMetadataItem>()?.symbol,
                description = metaDataItems.consume<DescriptionMetadataItem>()?.description,
                iconUrl = metaDataItems.consume<IconUrlMetadataItem>()?.url?.toString(),
                tags = metaDataItems.consume<TagsMetadataItem>()?.tags?.let { TagsColumn(tags = it) },
                validatorAddress = metaDataItems.consume<ValidatorMetadataItem>()?.address,
                poolAddress = metaDataItems.consume<PoolMetadataItem>()?.address,
                dAppDefinitions = metaDataItems.consume<DAppDefinitionsMetadataItem>()?.addresses?.let { DappDefinitionsColumn(it) },
                divisibility = details?.divisibility(),
                behaviours = details?.let { BehavioursColumn(it.extractBehaviours()) },
                supply = details?.totalSupply()?.toBigDecimalOrNull(),
                synced = synced
            )
        }
    }
}
