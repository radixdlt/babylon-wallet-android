package com.babylon.wallet.android.data.gateway.model

import android.net.Uri
import com.babylon.wallet.android.data.gateway.generated.models.EntityMetadataItemValue
import com.babylon.wallet.android.data.gateway.generated.models.MetadataDecimalValue
import com.babylon.wallet.android.data.gateway.generated.models.MetadataGlobalAddressArrayValue
import com.babylon.wallet.android.data.gateway.generated.models.MetadataGlobalAddressValue
import com.babylon.wallet.android.data.gateway.generated.models.MetadataI64Value
import com.babylon.wallet.android.data.gateway.generated.models.MetadataOriginArrayValue
import com.babylon.wallet.android.data.gateway.generated.models.MetadataPublicKeyHashArrayValue
import com.babylon.wallet.android.data.gateway.generated.models.MetadataStringArrayValue
import com.babylon.wallet.android.data.gateway.generated.models.MetadataStringValue
import com.babylon.wallet.android.data.gateway.generated.models.MetadataUrlArrayValue
import com.babylon.wallet.android.data.gateway.generated.models.MetadataUrlValue
import com.babylon.wallet.android.data.gateway.generated.models.PublicKeyHashEcdsaSecp256k1
import com.babylon.wallet.android.data.gateway.generated.models.PublicKeyHashEddsaEd25519
import com.babylon.wallet.android.domain.model.resources.metadata.AccountTypeMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.ClaimAmountMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.ClaimEpochMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.ClaimNftMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.ClaimedEntitiesMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.ClaimedWebsitesMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.DAppDefinitionsMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.DescriptionMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.IconUrlMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.InfoUrlMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.NameMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.OwnerKeyHashesMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.PoolMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.PoolUnitMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.RelatedWebsitesMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.StandardMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.SymbolMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.TagsMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.ValidatorMetadataItem

/**
 * Common metadata keys used in the wallet app and defined
 * by the most dApps and resources
 */
enum class ExplicitMetadataKey(val key: String) {
    DESCRIPTION("description"),
    SYMBOL("symbol"),
    NAME("name"),
    DAPP_DEFINITION("dapp_definition"),
    DAPP_DEFINITIONS("dapp_definitions"),
    RELATED_WEBSITES("related_websites"),
    CLAIMED_WEBSITES("claimed_websites"),
    CLAIMED_ENTITIES("claimed_entities"),
    ACCOUNT_TYPE("account_type"),
    TAGS("tags"),
    KEY_IMAGE_URL("key_image_url"),
    ICON_URL("icon_url"),
    INFO_URL("info_url"),
    VALIDATOR("validator"),
    CLAIM_AMOUNT("claim_amount"),
    CLAIM_EPOCH("claim_epoch"),
    POOL("pool"),
    POOL_UNIT("pool_unit"),
    CLAIM_NFT("claim_nft"),
    OWNER_KEYS("owner_keys");

    @Suppress("CyclomaticComplexMethod")
    fun toStandardMetadataItem(value: EntityMetadataItemValue): StandardMetadataItem? = when (this) {
        DESCRIPTION -> DescriptionMetadataItem(
            description = value.typed<MetadataStringValue>()?.value.orEmpty()
        )
        SYMBOL -> SymbolMetadataItem(
            symbol = value.typed<MetadataStringValue>()?.value.orEmpty()
        )
        NAME -> NameMetadataItem(
            name = value.typed<MetadataStringValue>()?.value.orEmpty()
        )
        DAPP_DEFINITION -> DAppDefinitionsMetadataItem(
            addresses = value.typed<MetadataGlobalAddressValue>()?.value?.let { listOf(it) }.orEmpty()
        )
        DAPP_DEFINITIONS -> DAppDefinitionsMetadataItem(
            addresses = value.typed<MetadataGlobalAddressArrayValue>()?.propertyValues.orEmpty()
        )
        RELATED_WEBSITES -> RelatedWebsitesMetadataItem(
            websites = value.typed<MetadataUrlArrayValue>()?.propertyValues.orEmpty()
        )
        ACCOUNT_TYPE -> AccountTypeMetadataItem.from(
            value = value.typed<MetadataStringValue>()?.value.orEmpty()
        )
        CLAIMED_WEBSITES -> ClaimedWebsitesMetadataItem(
            websites = value.typed<MetadataOriginArrayValue>()?.propertyValues.orEmpty()
        )
        CLAIMED_ENTITIES -> ClaimedEntitiesMetadataItem(
            entities = value.typed<MetadataGlobalAddressArrayValue>()?.propertyValues.orEmpty()
        )
        TAGS -> TagsMetadataItem(
            tags = value.typed<MetadataStringArrayValue>()?.propertyValues.orEmpty()
        )
        KEY_IMAGE_URL -> IconUrlMetadataItem(
            url = Uri.parse(value.typed<MetadataUrlValue>()?.value.orEmpty())
        )
        INFO_URL -> InfoUrlMetadataItem(
            url = Uri.parse(value.typed<MetadataUrlValue>()?.value.orEmpty())
        )
        ICON_URL -> IconUrlMetadataItem(
            url = Uri.parse(value.typed<MetadataUrlValue>()?.value.orEmpty())
        )
        OWNER_KEYS -> OwnerKeyHashesMetadataItem(
            keyHashes = value.typed<MetadataPublicKeyHashArrayValue>()?.propertyValues?.map { hash ->
                when (hash) {
                    is PublicKeyHashEcdsaSecp256k1 -> OwnerKeyHashesMetadataItem.KeyHash.EcdsaSecp256k1(hash.hashHex)
                    is PublicKeyHashEddsaEd25519 -> OwnerKeyHashesMetadataItem.KeyHash.EddsaEd25519(hash.hashHex)
                }
            }.orEmpty()
        )
        VALIDATOR -> ValidatorMetadataItem(value.typed<MetadataGlobalAddressValue>()?.value.orEmpty())
        CLAIM_AMOUNT -> ClaimAmountMetadataItem(value.typed<MetadataDecimalValue>()?.value.orEmpty())
        CLAIM_EPOCH -> ClaimEpochMetadataItem(value.typed<MetadataI64Value>()?.value?.toLong() ?: 0)
        POOL -> PoolMetadataItem(value.typed<MetadataGlobalAddressValue>()?.value.orEmpty())
        POOL_UNIT -> PoolUnitMetadataItem(value.typed<MetadataGlobalAddressValue>()?.value.orEmpty())
        CLAIM_NFT -> ClaimNftMetadataItem(value.typed<MetadataGlobalAddressValue>()?.value.orEmpty())
    }

    companion object {

        val forEntities: Set<ExplicitMetadataKey>
            get() = setOf(
                OWNER_KEYS
            )

        val forAssets: Set<ExplicitMetadataKey>
            get() = setOf(
                NAME,
                SYMBOL,
                DESCRIPTION,
                KEY_IMAGE_URL,
                ACCOUNT_TYPE,
                RELATED_WEBSITES,
                ICON_URL,
                INFO_URL,
                TAGS,
                DAPP_DEFINITIONS
            )

        val forResources: Set<ExplicitMetadataKey>
            get() = setOf(
                NAME,
                SYMBOL,
                DESCRIPTION,
                KEY_IMAGE_URL,
                ICON_URL,
                INFO_URL,
                TAGS,
                VALIDATOR,
                POOL
            )

        val forDapp: Set<ExplicitMetadataKey>
            get() = setOf(
                NAME,
                DESCRIPTION,
                ACCOUNT_TYPE,
                DAPP_DEFINITION,
                DAPP_DEFINITIONS,
                CLAIMED_WEBSITES,
                CLAIMED_ENTITIES,
                ICON_URL
            )

        val forValidatorsAndPools: Set<ExplicitMetadataKey>
            get() = setOf(
                NAME,
                ICON_URL,
                POOL_UNIT,
                CLAIM_NFT
            )

        fun from(key: String) = ExplicitMetadataKey.values().find { it.key == key }
    }
}
