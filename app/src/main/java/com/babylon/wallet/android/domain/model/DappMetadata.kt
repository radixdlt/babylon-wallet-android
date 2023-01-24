package com.babylon.wallet.android.domain.model

data class DappMetadata(
    val id: String,
    val dAppDefinitionAddress: String,
    val metadata: Map<String, String> = emptyMap()
) {
    fun getName(): String? {
        return metadata["name"]
    }
}
