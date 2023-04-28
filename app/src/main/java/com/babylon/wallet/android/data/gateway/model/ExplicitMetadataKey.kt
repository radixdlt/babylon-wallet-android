package com.babylon.wallet.android.data.gateway.model

import android.net.Uri
import com.babylon.wallet.android.domain.model.metadata.AccountTypeMetadataItem
import com.babylon.wallet.android.domain.model.metadata.AccountTypeMetadataItem.AccountType
import com.babylon.wallet.android.domain.model.metadata.DAppDefinitionMetadataItem
import com.babylon.wallet.android.domain.model.metadata.DescriptionMetadataItem
import com.babylon.wallet.android.domain.model.metadata.DomainMetadataItem
import com.babylon.wallet.android.domain.model.metadata.IconUrlMetadataItem
import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem
import com.babylon.wallet.android.domain.model.metadata.RelatedWebsiteMetadataItem
import com.babylon.wallet.android.domain.model.metadata.StandardMetadataItem
import com.babylon.wallet.android.domain.model.metadata.SymbolMetadataItem

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
    ICON_URL("icon_url");

    fun toStandardMetadataItem(value: String): StandardMetadataItem = when (this) {
        DESCRIPTION -> DescriptionMetadataItem(description = value)
        SYMBOL -> SymbolMetadataItem(symbol = value)
        NAME -> NameMetadataItem(name = value)
        DOMAIN -> DomainMetadataItem(domain = Uri.parse(value))
        DAPP_DEFINITION -> DAppDefinitionMetadataItem(address = value)
        RELATED_WEBSITES -> RelatedWebsiteMetadataItem(website = value)
        ACCOUNT_TYPE -> AccountTypeMetadataItem(type = AccountType.valueOf(value))
        ICON_URL -> IconUrlMetadataItem(url = Uri.parse(value))
    }

    companion object {
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
