package com.babylon.wallet.android.data.gateway.model

import android.net.Uri
import com.babylon.wallet.android.data.gateway.generated.models.EntityMetadataItemValue
import com.babylon.wallet.android.domain.model.metadata.AccountTypeMetadataItem
import com.babylon.wallet.android.domain.model.metadata.AccountTypeMetadataItem.AccountType
import com.babylon.wallet.android.domain.model.metadata.ClaimedEntitiesMetadataItem
import com.babylon.wallet.android.domain.model.metadata.ClaimedWebsiteMetadataItem
import com.babylon.wallet.android.domain.model.metadata.DAppDefinitionMetadataItem
import com.babylon.wallet.android.domain.model.metadata.DescriptionMetadataItem
import com.babylon.wallet.android.domain.model.metadata.DomainMetadataItem
import com.babylon.wallet.android.domain.model.metadata.IconUrlMetadataItem
import com.babylon.wallet.android.domain.model.metadata.InfoUrlMetadataItem
import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem
import com.babylon.wallet.android.domain.model.metadata.OwnerKeyHashesMetadataItem
import com.babylon.wallet.android.domain.model.metadata.RelatedWebsiteMetadataItem
import com.babylon.wallet.android.domain.model.metadata.StandardMetadataItem
import com.babylon.wallet.android.domain.model.metadata.SymbolMetadataItem
import com.babylon.wallet.android.domain.model.metadata.TagsMetadataItem

/**
 * Common metadata keys used in the wallet app and defined
 * by the most dApps and resources
 */
enum class ExplicitMetadataKey(val key: String) {
    DESCRIPTION("description"),
    SYMBOL("symbol"),
    NAME("name"),
    DOMAIN("domain"),
    DAPP_DEFINITION("dapp_definition"),
    RELATED_WEBSITES("related_websites"),
    CLAIMED_WEBSITES("claimed_websites"),
    CLAIMED_ENTITIES("claimed_entities"),
    ACCOUNT_TYPE("account_type"),
    TAGS("tags"),
    KEY_IMAGE_URL("key_image_url"),
    ICON_URL("icon_url"),
    INFO_URL("info_url"),
    URL("url"), // TODO it should be info_url ?
    OWNER_KEYS("owner_keys");

    @Suppress("CyclomaticComplexMethod")
    fun toStandardMetadataItem(value: EntityMetadataItemValue): StandardMetadataItem = when (this) {
        DESCRIPTION -> DescriptionMetadataItem(description = value.asString.orEmpty())
        SYMBOL -> SymbolMetadataItem(symbol = value.asString.orEmpty())
        NAME -> NameMetadataItem(name = value.asString.orEmpty())
        DOMAIN -> DomainMetadataItem(domain = Uri.parse(value.asString.orEmpty()))
        DAPP_DEFINITION -> DAppDefinitionMetadataItem(address = value.asString.orEmpty())
        RELATED_WEBSITES -> RelatedWebsiteMetadataItem(website = value.asString.orEmpty())
        ACCOUNT_TYPE -> AccountTypeMetadataItem(
            type = (AccountType.values().find { it.asString == value.asString } ?: DAPP_DEFINITION) as AccountType
        )
        CLAIMED_WEBSITES -> ClaimedWebsiteMetadataItem(website = value.asString.orEmpty())
        CLAIMED_ENTITIES -> ClaimedEntitiesMetadataItem(entity = value.asString.orEmpty())
        TAGS -> TagsMetadataItem(tags = value.asStringCollection.orEmpty())
        KEY_IMAGE_URL -> IconUrlMetadataItem(url = Uri.parse(value.asString.orEmpty()))
        INFO_URL -> InfoUrlMetadataItem(url = Uri.parse(value.asString.orEmpty()))
        ICON_URL -> IconUrlMetadataItem(url = Uri.parse(value.asString.orEmpty()))
        URL -> InfoUrlMetadataItem(url = Uri.parse(value.asString.orEmpty()))
        OWNER_KEYS -> OwnerKeyHashesMetadataItem(ownerKeys = value.asStringCollection.orEmpty())
    }

    companion object {

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
                URL,
                TAGS
            )

        val forDapp: Set<ExplicitMetadataKey>
            get() = setOf(
                NAME,
                DESCRIPTION,
                ACCOUNT_TYPE,
                RELATED_WEBSITES,
                ICON_URL
            )

        fun from(key: String) = ExplicitMetadataKey.values().find { it.key == key }
    }
}
