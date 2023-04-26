package com.babylon.wallet.android.domain.model

data class FungibleToken(
    val address: String,
    val metadata: Map<String, String> = emptyMap()
) {
    fun getIconUrl(): String? {
        return metadata[MetadataConstants.KEY_ICON]
    }

    fun getTokenName(): String? {
        return metadata[MetadataConstants.KEY_NAME]
    }

    fun getTokenSymbol(): String? {
        return metadata[MetadataConstants.KEY_SYMBOL]
    }

    fun getTokenDescription(): String? {
        return metadata[MetadataConstants.KEY_DESCRIPTION]
    }

    fun getDisplayableMetadata(): Map<String, String> {
        return metadata.filterKeys {
            !MetadataConstants.SPECIAL_METADATA.contains(it)
        }
    }
}
