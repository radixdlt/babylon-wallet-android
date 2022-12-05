package com.babylon.wallet.android.presentation.model

import com.babylon.wallet.android.domain.model.OwnedNonFungibleToken
import okio.ByteString.Companion.decodeHex

data class NftCollectionUiModel(
    val name: String,
    val iconUrl: String?,
    val nft: List<NftItemUiModel> = emptyList()
) : AssetUiModel() {

    data class NftItemUiModel(
        val id: String,
        val nftImage: String? = null,
        val nftsMetadata: List<Pair<String, String>> = emptyList()
    )
}

fun OwnedNonFungibleToken.toNftUiModel(): NftCollectionUiModel {
    return NftCollectionUiModel(
        name = token?.getTokenName().orEmpty(),
        iconUrl = token?.getImageUrl().orEmpty(),
        nft = token?.nonFungibleIdContainer?.ids?.map {
            NftCollectionUiModel.NftItemUiModel(id = it.idHex?.decodeHex()?.toString().orEmpty())
        }.orEmpty()
    )
}
