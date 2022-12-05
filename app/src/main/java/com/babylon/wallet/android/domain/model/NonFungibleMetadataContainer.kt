package com.babylon.wallet.android.domain.model

data class NonFungibleMetadataContainer(
    val metadata: Map<String, String> = emptyMap(),
    val nextCursor: String? = null,
    val previousCursor: String? = null,
)
