package com.babylon.wallet.android.data.gateway.model

import android.net.Uri
import com.babylon.wallet.android.domain.model.metadata.AccountTypeMetadataItem
import com.babylon.wallet.android.domain.model.metadata.AccountTypeMetadataItem.AccountType
import com.babylon.wallet.android.domain.model.metadata.DAppDefinitionMetadataItem
import com.babylon.wallet.android.domain.model.metadata.DescriptionMetadataItem
import com.babylon.wallet.android.domain.model.metadata.DomainMetadataItem
import com.babylon.wallet.android.domain.model.metadata.IconUrlMetadataItem
import com.babylon.wallet.android.domain.model.metadata.InfoUrlMetadataItem
import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem
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
    ACCOUNT_TYPE("account_type"),
    TAGS("tags"),
    KEY_IMAGE_URL("key_image_url"),
    ICON_URL("icon_url"),
    INFO_URL("info_url"),
    URL("url"); // TODO it should be info_url ?

    fun toStandardMetadataItem(value: String): StandardMetadataItem = when (this) {
        DESCRIPTION -> DescriptionMetadataItem(description = value)
        SYMBOL -> SymbolMetadataItem(symbol = value)
        NAME -> NameMetadataItem(name = value)
        DOMAIN -> DomainMetadataItem(domain = Uri.parse(value))
        DAPP_DEFINITION -> DAppDefinitionMetadataItem(address = value)
        RELATED_WEBSITES -> RelatedWebsiteMetadataItem(website = value)
        ACCOUNT_TYPE -> AccountTypeMetadataItem(type = AccountType.valueOf(value))
        TAGS -> TagsMetadataItem(tags = listOf(value))
        KEY_IMAGE_URL -> IconUrlMetadataItem(url = Uri.parse(value))
        INFO_URL -> InfoUrlMetadataItem(url = Uri.parse(value))
        ICON_URL -> IconUrlMetadataItem(url = Uri.parse(value))
        URL -> InfoUrlMetadataItem(url = Uri.parse(value))
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
