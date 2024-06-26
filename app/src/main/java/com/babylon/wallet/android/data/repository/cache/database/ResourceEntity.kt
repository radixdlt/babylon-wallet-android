package com.babylon.wallet.android.data.repository.cache.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.babylon.wallet.android.data.gateway.extensions.divisibility
import com.babylon.wallet.android.data.gateway.extensions.extractBehaviours
import com.babylon.wallet.android.data.gateway.extensions.toMetadata
import com.babylon.wallet.android.data.gateway.extensions.totalSupply
import com.babylon.wallet.android.data.gateway.generated.models.EntityMetadataCollection
import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollectionItem
import com.babylon.wallet.android.data.gateway.generated.models.NonFungibleResourcesCollectionItem
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseFungibleResourceDetails
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItem
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItemDetails
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseNonFungibleResourceDetails
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.PoolAddress
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.ValidatorAddress
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.extensions.toDecimal192
import rdx.works.core.domain.resources.Divisibility
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.metadata.poolAddress
import rdx.works.core.domain.resources.metadata.validatorAddress
import java.time.Instant

enum class ResourceEntityType {
    FUNGIBLE,
    NON_FUNGIBLE
}

@Entity
data class ResourceEntity(
    @PrimaryKey val address: ResourceAddress,
    val type: ResourceEntityType,
    val metadata: MetadataColumn?,
    val divisibility: Divisibility?,
    val behaviours: BehavioursColumn?,
    @ColumnInfo("validator_address")
    val validatorAddress: ValidatorAddress?,
    @ColumnInfo("pool_address")
    val poolAddress: PoolAddress?,
    val supply: Decimal192?,
    val synced: Instant
) {

    val isDetailsAvailable: Boolean
        get() = when (type) {
            ResourceEntityType.FUNGIBLE -> supply != null && divisibility != null && behaviours != null
            ResourceEntityType.NON_FUNGIBLE -> supply != null && behaviours != null
        }

    @Suppress("CyclomaticComplexMethod")
    fun toResource(amount: Decimal192?): Resource {
        return when (type) {
            ResourceEntityType.FUNGIBLE -> {
                Resource.FungibleResource(
                    address = address,
                    ownedAmount = amount,
                    assetBehaviours = behaviours?.behaviours?.toSet(),
                    currentSupply = supply,
                    divisibility = divisibility,
                    metadata = metadata?.metadata.orEmpty()
                )
            }

            ResourceEntityType.NON_FUNGIBLE -> {
                Resource.NonFungibleResource(
                    address = address,
                    amount = amount?.string?.toLongOrNull() ?: 0L,
                    assetBehaviours = behaviours?.behaviours?.toSet(),
                    items = emptyList(),
                    currentSupply = supply?.string?.toIntOrNull(),
                    metadata = metadata?.metadata.orEmpty()
                )
            }
        }
    }

    companion object {
        fun Resource.asEntity(synced: Instant): ResourceEntity = when (this) {
            is Resource.FungibleResource -> ResourceEntity(
                address = address,
                type = ResourceEntityType.FUNGIBLE,
                divisibility = divisibility,
                behaviours = behaviours?.let { BehavioursColumn(it) },
                supply = currentSupply,
                validatorAddress = metadata.validatorAddress(),
                poolAddress = metadata.poolAddress(),
                metadata = metadata
                    .takeIf { it.isNotEmpty() }
                    ?.let { MetadataColumn(it, MetadataColumn.ImplicitMetadataState.Unknown) },
                synced = synced
            )

            is Resource.NonFungibleResource -> ResourceEntity(
                address = address,
                type = ResourceEntityType.NON_FUNGIBLE,
                behaviours = behaviours?.let { BehavioursColumn(it) },
                supply = currentSupply?.toDecimal192(),
                divisibility = null,
                validatorAddress = metadata.validatorAddress(),
                poolAddress = metadata.poolAddress(),
                metadata = metadata
                    .takeIf { it.isNotEmpty() }
                    ?.let { MetadataColumn(it, MetadataColumn.ImplicitMetadataState.Unknown) },
                synced = synced
            )
        }

        // Response from an account state request
        fun FungibleResourcesCollectionItem.asEntity(
            synced: Instant,
            // In case we have fetched details for this item
            details: StateEntityDetailsResponseItemDetails? = null
        ): ResourceEntity = from(
            address = ResourceAddress.init(resourceAddress),
            explicitMetadata = explicitMetadata,
            implicitMetadata = null,
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
            address = ResourceAddress.init(resourceAddress),
            explicitMetadata = explicitMetadata,
            implicitMetadata = null,
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
                address = ResourceAddress.init(address),
                explicitMetadata = explicitMetadata,
                implicitMetadata = metadata,
                details = details,
                type = type,
                synced = synced
            )
        }

        private fun from(
            address: ResourceAddress,
            explicitMetadata: EntityMetadataCollection?,
            implicitMetadata: EntityMetadataCollection?,
            details: StateEntityDetailsResponseItemDetails?,
            type: ResourceEntityType,
            synced: Instant
        ): ResourceEntity {
            val metadataColumn = if (implicitMetadata != null) {
                MetadataColumn.from(
                    explicitMetadata = explicitMetadata,
                    implicitMetadata = implicitMetadata
                )
            } else {
                MetadataColumn(
                    metadata = explicitMetadata?.toMetadata().orEmpty(),
                    implicitState = MetadataColumn.ImplicitMetadataState.Unknown
                )
            }.takeIf { it.metadata.isNotEmpty() }

            return ResourceEntity(
                address = address,
                type = type,
                divisibility = details?.divisibility(),
                behaviours = details?.let { BehavioursColumn(it.extractBehaviours()) },
                supply = details?.totalSupply(),
                validatorAddress = metadataColumn?.metadata?.validatorAddress(),
                poolAddress = metadataColumn?.metadata?.poolAddress(),
                metadata = metadataColumn,
                synced = synced
            )
        }
    }
}
