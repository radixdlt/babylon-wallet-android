package com.babylon.wallet.android.data.gateway.model

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
    ICON_URL("icon_url")
}
