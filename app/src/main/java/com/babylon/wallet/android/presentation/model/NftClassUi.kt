package com.babylon.wallet.android.presentation.model

data class NftClassUi(
    val name: String,
    val amount: String,
    val iconUrl: String?,
    val nft: List<NftUi>
) {

    data class NftUi(
        val id: String,
        val name: String
    )
}
