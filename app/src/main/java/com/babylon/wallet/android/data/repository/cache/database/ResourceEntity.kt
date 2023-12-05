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
import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.metadata.Metadata
import com.babylon.wallet.android.domain.model.resources.metadata.MetadataType
import com.babylon.wallet.android.domain.model.resources.metadata.poolAddress
import com.babylon.wallet.android.domain.model.resources.metadata.validatorAddress
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
    val metadata: MetadataColumn?,
    val divisibility: Int?,
    val behaviours: BehavioursColumn?,
    @ColumnInfo("validator_address")
    val validatorAddress: String?,
    @ColumnInfo("pool_address")
    val poolAddress: String?,
    val supply: BigDecimal?,
    val synced: Instant
) {

    @Suppress("CyclomaticComplexMethod")
    fun toResource(amount: BigDecimal?): Resource {
        val validatorAndPoolMetadata = listOf(
            validatorAddress?.let {
                Metadata.Primitive(ExplicitMetadataKey.VALIDATOR.key, it, MetadataType.Address)
            },
            poolAddress?.let {
                Metadata.Primitive(ExplicitMetadataKey.POOL.key, it, MetadataType.Address)
            }
        ).mapNotNull { it }

        return when (type) {
            ResourceEntityType.FUNGIBLE -> {
                Resource.FungibleResource(
                    resourceAddress = address,
                    ownedAmount = amount,
                    assetBehaviours = behaviours?.behaviours?.toSet(),
                    currentSupply = supply,
                    divisibility = divisibility,
                    metadata = metadata?.metadata.orEmpty() + validatorAndPoolMetadata
                )
            }

            ResourceEntityType.NON_FUNGIBLE -> {
                Resource.NonFungibleResource(
                    resourceAddress = address,
                    amount = amount?.toLong() ?: 0L,
                    assetBehaviours = behaviours?.behaviours?.toSet(),
                    items = emptyList(),
                    currentSupply = supply?.toInt(),
                    metadata = metadata?.metadata.orEmpty() + validatorAndPoolMetadata
                )
            }
        }
    }

    companion object {
        fun Resource.asEntity(synced: Instant): ResourceEntity = when (this) {
            is Resource.FungibleResource -> ResourceEntity(
                address = resourceAddress,
                type = ResourceEntityType.FUNGIBLE,
                divisibility = divisibility,
                behaviours = behaviours?.let { BehavioursColumn(it) },
                supply = currentSupply,
                validatorAddress = metadata.validatorAddress(),
                poolAddress = metadata.poolAddress(),
                metadata = metadata
                    .filterNot { it.key in setOf(ExplicitMetadataKey.POOL.key, ExplicitMetadataKey.VALIDATOR.key) }
                    .takeIf { it.isNotEmpty() }
                    ?.let { MetadataColumn(it) },
                synced = synced
            )

            is Resource.NonFungibleResource -> ResourceEntity(
                address = resourceAddress,
                type = ResourceEntityType.NON_FUNGIBLE,
                behaviours = behaviours?.let { BehavioursColumn(it) },
                supply = currentSupply?.toBigDecimal(),
                divisibility = null,
                validatorAddress = metadata.validatorAddress(),
                poolAddress = metadata.poolAddress(),
                metadata = metadata
                    .filterNot { it.key in setOf(ExplicitMetadataKey.POOL.key, ExplicitMetadataKey.VALIDATOR.key) }
                    .takeIf { it.isNotEmpty() }
                    ?.let { MetadataColumn(it) },
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
            metadataCollection = explicitMetadata,
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
            metadataCollection = explicitMetadata,
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
                metadataCollection = metadata,
                details = details,
                type = type,
                synced = synced
            )
        }

        private fun from(
            address: String,
            metadataCollection: EntityMetadataCollection?,
            details: StateEntityDetailsResponseItemDetails?,
            type: ResourceEntityType,
            synced: Instant
        ): ResourceEntity {
            val metadata = metadataCollection?.toMetadata().orEmpty()
            return ResourceEntity(
                address = address,
                type = type,
                divisibility = details?.divisibility(),
                behaviours = details?.let { BehavioursColumn(it.extractBehaviours()) },
                supply = details?.totalSupply()?.toBigDecimalOrNull(),
                validatorAddress = metadata.validatorAddress(),
                poolAddress = metadata.poolAddress(),
                metadata = metadata
                    .filterNot { it.key in setOf(ExplicitMetadataKey.VALIDATOR.key, ExplicitMetadataKey.POOL.key) }
                    .takeIf { it.isNotEmpty() }
                    ?.let { MetadataColumn(it) },
                synced = synced
            )
        }
    }
}
