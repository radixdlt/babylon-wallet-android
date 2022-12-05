package com.babylon.wallet.android.domain.model

data class NonFungibleTokenIdContainer(
    val ids: List<NonFungibleTokenId> = emptyList(),
    val nextCursor: String? = null,
    val previousCursor: String? = null,
)
