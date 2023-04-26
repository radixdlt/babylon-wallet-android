package com.babylon.wallet.android.presentation.model

import com.babylon.wallet.android.domain.model.OwnedNonFungibleToken

data class NftCollectionUiModel(
    val name: String,
    val iconUrl: String?,
    val nfts: List<NftItemUiModel> = emptyList()
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
        iconUrl = ownedNonFungibleToken.token?.getIconUrl().orEmpty(),
        nfts = ownedNonFungibleToken.token?.nfts?.map { nftItemContainer ->
            NftCollectionUiModel.NftItemUiModel(
                id = nftItemContainer.id,
                displayId = (ownedNonFungibleToken.tokenResourceAddress + nftItemContainer.id).hashCode(),
                nftImage = nftItemContainer.nftImage,
                nftsMetadata = emptyList()
            )
        }.orEmpty()
    )
}
