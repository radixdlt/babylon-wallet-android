package com.babylon.wallet.android.data

import com.babylon.wallet.android.data.AssetDto.NftClassDto.NftDto.Companion.toUiModel
import com.babylon.wallet.android.presentation.model.NftClassUi
import com.babylon.wallet.android.presentation.model.TokenUi
import java.math.BigDecimal

// WARNING: this is not the actual data structure of the data transfer object and
// might change completely in the future, since we don't have any knowledge of the backend.
//
// At this stage of development we can only assume that the server will provide
// the following data structure for all assets and every type of asset is defined
// by the parameter "type".
//
data class AssetDto(
    val type: String, // "nft", "token", "pool share", "badge"
    val id: String,
    val name: String?,
    val symbol: String?, // short name
    val resourceAddress: String,
    val tokenQuantity: Double, // if the type is "token"
    val marketPrice: Float?, // this is treated as "token"/USD trading pair at the moment
    val iconUrl: String?, // icon for the token
    val nftClasses: List<NftClassDto>? // NFT types, and each NFT type has a list of actual NFTs
) {

    data class NftClassDto(
        val classId: String, // the NFT class id
        val name: String,
        val iconUrl: String?, // the icon of the NFT class
        val nftsInCirculation: String?,
        val nftsInPossession: String?,
        val nfts: List<NftDto> // the list of actual NFTs
    ) {

        data class NftDto(
            val id: String,
            val name: String,
            val iconUrl: String?,
            val nftsMetadata: List<Pair<String, String>>
        ) {

            companion object {
                fun NftDto.toUiModel() = NftClassUi.NftUi(
                    id = id,
                    imageUrl = iconUrl,
                    nftsMetadata = nftsMetadata
                )
            }
        }

        companion object {
            fun NftClassDto.toNftClassUiModel() = NftClassUi(
                name = name,
                nftsInCirculation = nftsInCirculation,
                nftsInPossession = nftsInPossession,
                iconUrl = iconUrl,
                nft = nfts.map { nftDto ->
                    nftDto.toUiModel()
                }
            )
        }
    }

    companion object {
        fun AssetDto.toTokenUiModel() = TokenUi(
            id = id,
            name = name,
            symbol = symbol,
            tokenQuantity = BigDecimal.valueOf(tokenQuantity),
            tokenValue = if (marketPrice == null) null else (marketPrice.times(tokenQuantity)).toString(),
            iconUrl = iconUrl
        )
    }
}
