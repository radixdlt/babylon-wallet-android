package com.babylon.wallet.android.domain.model

data class DappMetadata(
    val dAppDefinitionAddress: String,
    val metadata: Map<String, String> = emptyMap()
) {

    fun getName(): String? {
        return metadata[MetadataConstants.KEY_NAME]
    }

    fun getDescription(): String? {
        return metadata[MetadataConstants.KEY_DESCRIPTION]
    }

    fun getIcon(): String? {
        return metadata[MetadataConstants.KEY_IMAGE_URL]
    }

    fun getRelatedDomainName(): String? {
        return metadata[MetadataConstants.KEY_RELATED_DOMAIN_NAME]
    }

    private fun getAccountType(): String? {
        return metadata[MetadataConstants.KEY_ACCOUNT_TYPE]
    }

    fun isDappDefinition(): Boolean {
        return getAccountType() == "dapp definition"
    }

    fun getDappDefinition(): String {
        return metadata[MetadataConstants.KEY_D_APP_DEFINITION].orEmpty()
    }

    fun getDisplayableMetadata(): Map<String, String> {
        return metadata.filterKeys {
            !MetadataConstants.SPECIAL_METADATA.contains(it)
        }
    }
}
