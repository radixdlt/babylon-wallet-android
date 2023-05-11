package com.babylon.wallet.android.presentation.model

import com.babylon.wallet.android.domain.model.AccountWithResources

data class NftCollectionUiModel(
    val name: String,
    val iconUrl: String?,
    val nfts: List<NftItemUiModel> = emptyList()
) : AssetUiModel() {

    data class NftItemUiModel(
        val localId: String,
        val resourceAddress: String,
        val nftImage: String? = null,
        val nftsMetadata: List<Pair<String, String>> = emptyList()
    ) {

        val displayAddress: String
            get() = "$resourceAddress:$localId"
    }
}

// TODO when gateway is ready
fun List<AccountWithResources.Resource.NonFungibleResource>.toNftUiModel() = map { nonFungibleResource ->
    NftCollectionUiModel(
        name = nonFungibleResource.name,
        iconUrl = "", // ownedNonFungibleToken.token?.getIconUrl().orEmpty(),
        nfts = emptyList()
    )
}

/*fun List<OwnedNonFungibleToken>.toNftUiModel() = map { ownedNonFungibleToken ->
    NftCollectionUiModel(
        name = ownedNonFungibleToken.token?.getTokenName().orEmpty(),
        iconUrl = ownedNonFungibleToken.token?.getIconUrl().orEmpty(),
        nfts = ownedNonFungibleToken.token?.nfts?.map { nftItemContainer ->
            NftCollectionUiModel.NftItemUiModel(
                localId = nftItemContainer.id,
                resourceAddress = ownedNonFungibleToken.tokenResourceAddress,
                nftImage = nftItemContainer.nftImage,
                nftsMetadata = emptyList()
            )
        }.orEmpty()
    )
}*/
