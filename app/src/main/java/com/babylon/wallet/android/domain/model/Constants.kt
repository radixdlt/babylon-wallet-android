package com.babylon.wallet.android.domain.model

object AppConstants {
    const val VM_STOP_TIMEOUT_MS = 5000L
}

object MetadataConstants {
    const val KEY_SYMBOL = "symbol"
    const val KEY_DESCRIPTION = "description"
    const val KEY_IMAGE_URL = "key_image_url"
    const val KEY_ICON = "icon_url"
    const val KEY_NAME = "name"
    const val KEY_ACCOUNT_TYPE = "account_type"
    const val KEY_RELATED_DOMAIN_NAME = "related_websites"
    const val KEY_NFT_IMAGE = "nft_image"
    const val KEY_D_APP_DEFINITION = "dapp_definition"

    const val SYMBOL_XRD = "XRD"
    val SPECIAL_METADATA = listOf(
        KEY_DESCRIPTION,
        KEY_NAME,
        KEY_SYMBOL,
        KEY_IMAGE_URL,
        KEY_ACCOUNT_TYPE,
        KEY_D_APP_DEFINITION
    )
}
