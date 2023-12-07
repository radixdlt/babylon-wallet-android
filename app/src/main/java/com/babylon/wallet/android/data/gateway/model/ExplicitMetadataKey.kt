@file:Suppress("LongMethod, CyclomaticComplexity")

package com.babylon.wallet.android.data.gateway.model

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
    OWNER_KEYS("owner_keys"),
    OWNER_BADGE("owner_badge");

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
