package com.babylon.wallet.android.presentation.model

import com.babylon.wallet.android.domain.model.OwnedNonFungibleToken

data class NftUiModel(
    val name: String,
    val iconUrl: String?,
    val nft: List<NftUi> = emptyList()
) {

    data class NftUi(
        val id: String,
        val imageUrl: String?,
        val nftsMetadata: List<Pair<String, String>> = emptyList()
    )
}

fun OwnedNonFungibleToken.toNftUiModel(): NftUiModel {
    return NftUiModel(
        name = token?.getTokenName().orEmpty(),
        iconUrl = token?.getImageUrl().orEmpty(),
//        Test data to test NFT List
//        nft = listOf(
//            NftUiModel.NftUi(
//                Random.nextInt(100000, 100000000).toString(),
//                null,
//                listOf(Random.nextInt(100000).toString() to Random.nextInt(100000).toString())
//            ),
//            NftUiModel.NftUi(
//                Random.nextInt(100000, 100000000).toString(),
//                null,
//                listOf(Random.nextInt(100000).toString() to Random.nextInt(100000).toString())
//            ),
//            NftUiModel.NftUi(
//                Random.nextInt(100000, 100000000).toString(),
//                null,
//                listOf(Random.nextInt(100000).toString() to Random.nextInt(100000).toString())
//            )
//        )
    )
}
