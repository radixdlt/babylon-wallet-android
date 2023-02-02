package com.babylon.wallet.android.domain.model

data class DappMetadata(
    val dAppDefinitionAddress: String,
    val metadata: Map<String, String> = emptyMap()
) {
    fun getName(): String? {
        return metadata[MetadataConstants.KEY_NAME]
    }

    fun getIcon(): String? {
        return metadata[MetadataConstants.KEY_IMAGE_URL]
    }

    fun getRelatedDomainName(): String? {
        return metadata[MetadataConstants.KEY_RELATED_DOMAIN_NAME]
    }
}
