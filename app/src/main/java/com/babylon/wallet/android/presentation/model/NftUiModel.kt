package com.babylon.wallet.android.presentation.model

import com.babylon.wallet.android.domain.model.OwnedNonFungibleToken
import okio.ByteString.Companion.decodeHex

data class NftUiModel(
    val name: String,
    val iconUrl: String?,
    val nft: List<NftUi> = emptyList()
) {

    data class NftUi(
        val id: String,
        val nftsMetadata: List<Pair<String, String>> = emptyList()
    )
}

fun OwnedNonFungibleToken.toNftUiModel(): NftUiModel {
    String()
    return NftUiModel(
        name = token?.getTokenName().orEmpty(),
        iconUrl = token?.getImageUrl().orEmpty(),
        nft = token?.nonFungibleIdContainer?.ids?.map {
            NftUiModel.NftUi(id = it.idHex?.decodeHex()?.toString().orEmpty())
        } ?: emptyList()
    )
}
