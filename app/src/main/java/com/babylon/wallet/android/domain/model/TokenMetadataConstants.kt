package com.babylon.wallet.android.domain.model

object TokenMetadataConstants {
    const val KEY_SYMBOL = "symbol"
    const val KEY_DESCRIPTION = "description"
    const val KEY_URL = "url"
    const val KEY_NAME = "name"
    const val KEY_NFT_IMAGE = "nft_image"

    const val SYMBOL_XRD = "XRD"
    val SPECIAL_METADATA = listOf(KEY_DESCRIPTION, KEY_NAME, KEY_SYMBOL, KEY_URL)
}
