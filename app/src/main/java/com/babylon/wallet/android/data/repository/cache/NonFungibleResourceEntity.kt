package com.babylon.wallet.android.data.repository.cache

import androidx.core.net.toUri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.babylon.wallet.android.data.gateway.extensions.asMetadataItems
import com.babylon.wallet.android.data.gateway.extensions.extractBehaviours
import com.babylon.wallet.android.data.gateway.extensions.totalSupply
import com.babylon.wallet.android.data.gateway.generated.models.NonFungibleResourcesCollectionItem
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItem
import com.babylon.wallet.android.domain.model.metadata.DAppDefinitionsMetadataItem
import com.babylon.wallet.android.domain.model.metadata.DescriptionMetadataItem
import com.babylon.wallet.android.domain.model.metadata.IconUrlMetadataItem
import com.babylon.wallet.android.domain.model.metadata.MetadataItem.Companion.consume
import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem
import com.babylon.wallet.android.domain.model.metadata.TagsMetadataItem
import com.babylon.wallet.android.domain.model.metadata.ValidatorMetadataItem
import com.babylon.wallet.android.domain.model.resources.Resource
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
    override val synced: Instant,
    override val epoch: Long
): CachedEntity {

    fun toResource(withOwnedAmount: Long): Resource.NonFungibleResource {
        return Resource.NonFungibleResource(
            resourceAddress = address,
            amount = withOwnedAmount,
            nameMetadataItem = name?.let { NameMetadataItem(it) },
            descriptionMetadataItem = description?.let { DescriptionMetadataItem(it) },
            iconMetadataItem = iconUrl?.let { IconUrlMetadataItem(it.toUri()) },
            tagsMetadataItem = tags?.let { TagsMetadataItem(tags = it.tags) },
            validatorMetadataItem = validatorAddress?.let { ValidatorMetadataItem(it) },
            dAppDefinitionsMetadataItem = dAppDefinitions?.let { DAppDefinitionsMetadataItem(it.dappDefinitions) },
            behaviours = behaviours?.behaviours?.toSet().orEmpty(),
            items = emptyList(),
            currentSupply = supply,
        )
    }

    fun StateEntityDetailsResponseItem.asNonFungibleResource(): Resource.NonFungibleResource {
        val resourceBehaviours = details?.extractBehaviours().orEmpty()
        val currentSupply = details?.totalSupply()?.toIntOrNull()
        val metaDataItems = explicitMetadata?.asMetadataItems().orEmpty().toMutableList()
        return Resource.NonFungibleResource(
            resourceAddress = address,
            amount = 0,
            nameMetadataItem = metaDataItems.consume(),
            descriptionMetadataItem = metaDataItems.consume(),
            iconMetadataItem = metaDataItems.consume(),
            tagsMetadataItem = metaDataItems.consume(),
            behaviours = resourceBehaviours,
            items = emptyList(),
            currentSupply = currentSupply,
            validatorMetadataItem = metaDataItems.consume(),
            dAppDefinitionsMetadataItem = metaDataItems.consume()
        )
    }

    companion object {
        // Response from an account state request
        fun NonFungibleResourcesCollectionItem.asEntity(
            syncInfo: SyncInfo
        ): NonFungibleResourceEntity {
            val metaDataItems = explicitMetadata?.asMetadataItems().orEmpty().toMutableList()
            return NonFungibleResourceEntity(
                address = resourceAddress,
                name = metaDataItems.consume<NameMetadataItem>()?.name,
                description = metaDataItems.consume<DescriptionMetadataItem>()?.description,
                iconUrl = metaDataItems.consume<IconUrlMetadataItem>()?.url.toString(),
                tags = metaDataItems.consume<TagsMetadataItem>()?.tags?.let { TagsColumn(tags = it) },
                validatorAddress = metaDataItems.consume<ValidatorMetadataItem>()?.validatorAddress,
                dAppDefinitions = metaDataItems.consume<DAppDefinitionsMetadataItem>()?.addresses?.let { DappDefinitionsColumn(it) },
                behaviours = null,
                supply = null,
                synced = syncInfo.synced,
                epoch = syncInfo.epoch
            )
        }

        // Response from details request
        fun StateEntityDetailsResponseItem.asEntity(
            syncInfo: SyncInfo
        ): NonFungibleResourceEntity {
            val metaDataItems = explicitMetadata?.asMetadataItems().orEmpty().toMutableList()
            return NonFungibleResourceEntity(
                address = address,
                name = metaDataItems.consume<NameMetadataItem>()?.name,
                description = metaDataItems.consume<DescriptionMetadataItem>()?.description,
                iconUrl = metaDataItems.consume<IconUrlMetadataItem>()?.url.toString(),
                tags = metaDataItems.consume<TagsMetadataItem>()?.tags?.let { TagsColumn(tags = it) },
                validatorAddress = metaDataItems.consume<ValidatorMetadataItem>()?.validatorAddress,
                dAppDefinitions = metaDataItems.consume<DAppDefinitionsMetadataItem>()?.addresses?.let { DappDefinitionsColumn(it) },
                behaviours = details?.extractBehaviours()?.let { BehavioursColumn(it.toList()) },
                supply = details?.totalSupply()?.toIntOrNull(),
                synced = syncInfo.synced,
                epoch = syncInfo.epoch
            )
        }
    }

}
