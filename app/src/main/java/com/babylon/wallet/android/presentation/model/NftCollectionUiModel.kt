package com.babylon.wallet.android.presentation.model

import com.babylon.wallet.android.domain.model.OwnedNonFungibleToken

data class NftCollectionUiModel(
    val name: String,
    val iconUrl: String?,
    val nft: List<NftItemUiModel> = emptyList()
) : AssetUiModel() {

    data class NftItemUiModel(
        val id: String,
        val displayId: Int,
        val nftImage: String? = null,
        val nftsMetadata: List<Pair<String, String>> = emptyList()
    )
}

fun List<OwnedNonFungibleToken>.toNftUiModel() = map { ownedNonFungibleToken ->
    NftCollectionUiModel(
        name = ownedNonFungibleToken.token?.getTokenName().orEmpty(),
        iconUrl = ownedNonFungibleToken.token?.getImageUrl().orEmpty(),
        nft = ownedNonFungibleToken.token?.nonFungibleIdContainer?.ids?.map {
            NftCollectionUiModel.NftItemUiModel(id = it, displayId = (ownedNonFungibleToken.tokenResourceAddress + it).hashCode())
        }.orEmpty()
    )
}
