package com.babylon.wallet.android.presentation.model

data class NftClassUi(
    val name: String,
    val nftsInCirculation: String?,
    val nftsInPossession: String?,
    val iconUrl: String?,
    val nft: List<NftUi>
) {

    data class NftUi(
        val id: String,
        val imageUrl: String?,
        val nftsMetadata: List<Pair<String, String>> = emptyList()
    )
}
