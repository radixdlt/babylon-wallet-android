package com.babylon.wallet.android.data.mockdata

import com.babylon.wallet.android.data.AssetDto
import com.babylon.wallet.android.data.AssetDto.Companion.toTokenUiModel
import com.babylon.wallet.android.data.AssetDto.NftClassDto.Companion.toNftClassUiModel
import com.babylon.wallet.android.domain.AssetType

// list of NFT assets
private val mockNftDtoList = listOf(
    AssetDto.NftClassDto(
        classId = "puppy",
        name = "Puppy first",
        iconUrl = "https://cdn-icons-png.flaticon.com/512/824/824719.png",
        nftsInCirculation = "20000",
        nftsInPossession = "8",
        nfts = listOf(
            AssetDto.NftClassDto.NftDto(
                id = "puppy_nft_id",
                name = "ghosty",
                iconUrl = null,
                nftsMetadata = listOf(
                    Pair("Section", "E"),
                    Pair("Seat", "15D")
                )
            ),
            AssetDto.NftClassDto.NftDto(
                id = "puppy_small_nft_id",
                name = "Paw",
                iconUrl = "https://cdn-icons-png.flaticon.com/512/1273/1273729.png",
                nftsMetadata = listOf(
                    Pair("Section", "E"),
                    Pair("Seat", "15D")
                )
            )
        )
    ),
    AssetDto.NftClassDto(
        classId = "arsenal_nft_id",
        name = "Arsenal",
        iconUrl = "https://cdn-icons-png.flaticon.com/512/824/824719.png",
        nftsInCirculation = "1000",
        nftsInPossession = "2",
        nfts = listOf(
            AssetDto.NftClassDto.NftDto(
                id = "arsenal_henry_nft_id",
                name = "Henry",
                iconUrl = "https://cdn-icons-png.flaticon.com/512/738/738680.png",
                nftsMetadata = listOf(
                    Pair("Type", "Kevin Durant - Dunk")
                )
            ),
            AssetDto.NftClassDto.NftDto(
                id = "arsenal_henry_nft_id",
                name = "Henry",
                iconUrl = "https://cdn-icons-png.flaticon.com/512/738/738680.png",
                nftsMetadata = listOf(
                    Pair("Type", "Kevin Durant - Dunk")
                )
            ),
            AssetDto.NftClassDto.NftDto(
                id = "arsenal_bergkamp_nft_id",
                name = "Bergkamp",
                iconUrl = "https://cdn-icons-png.flaticon.com/512/1273/1273729.png",
                nftsMetadata = listOf(
                    Pair("Type", "Kevin Durant - Dunk")
                )
            ),
            AssetDto.NftClassDto.NftDto(
                id = "arsenal_vieira_nft_id",
                name = "Vieira",
                iconUrl = null,
                nftsMetadata = listOf(
                    Pair("Type", "Kevin Durant - Dunk"),
                    Pair("Type", "James Bond - Next")
                )
            )
        )
    ),
    AssetDto.NftClassDto(
        classId = "black_hole_nft_id",
        name = "Black Hole",
        iconUrl = "https://cdn-icons-png.flaticon.com/512/824/824719.png",
        nftsInCirculation = "100",
        nftsInPossession = "1",
        nfts = listOf(
            AssetDto.NftClassDto.NftDto(
                id = "stellar_nft_id",
                name = "stellar",
                iconUrl = "https://cdn-icons.flaticon.com/png/512/1636/premium/1636920.png?token=exp=1660065129~hmac=74a06634adeba2711540c67e207296d3",
                nftsMetadata = listOf(
                    Pair("Section", "E"),
                    Pair("Seat", "15D")
                )
            )
        )
    ),
    AssetDto.NftClassDto(
        classId = "dog_nft_id",
        name = "Doggies",
        iconUrl = "https://cdn-icons-png.flaticon.com/512/824/824719.png",
        nftsInCirculation = "20000",
        nftsInPossession = "8",
        nfts = listOf(
            AssetDto.NftClassDto.NftDto(
                id = "ghosty_nft_id",
                name = "ghosty",
                iconUrl = null,
                nftsMetadata = listOf(
                    Pair("Section", "E"),
                    Pair("Seat", "15D")
                )
            ),
            AssetDto.NftClassDto.NftDto(
                id = "paw_nft_id",
                name = "Paw",
                iconUrl = "https://cdn-icons-png.flaticon.com/512/1273/1273729.png",
                nftsMetadata = listOf(
                    Pair("Section", "E"),
                    Pair("Seat", "15D")
                )
            )
        )
    )
)

// asset dto that contains NFT assets
private val mockNtfAssetDto = AssetDto(
    type = "nft",
    id = "nft_id",
    name = null,
    symbol = null,
    resourceAddress = "bc1qxy2kgdysfsafsq2n0yrf2493p83kksgdgx0wlh",
    tokenQuantity = 0F,
    marketPrice = null,
    iconUrl = null,
    nftClasses = mockNftDtoList
)

// asset for bitcoin token
private val mockBitcoinAssetDto = AssetDto(
    type = "token",
    id = "btc_id",
    name = "Bitcoin",
    symbol = "BTC",
    resourceAddress = "bc1qxy2safsafafsq2n0yrf2493p83kksgder0wlh",
    tokenQuantity = 0.38684F,
    marketPrice = 23004.89F,
    iconUrl = "https://avatars.githubusercontent.com/u/528860?s=200&v=4",
    nftClasses = null
)

// asset for radix token
private val mockRadixAssetDto = AssetDto(
    type = "token",
    id = "xrd_id",
    name = "Radix",
    symbol = "XRD",
    resourceAddress = "bc1qxy2fafsaffafsq2n0yrf2354dp83kksgder0wlh",
    tokenQuantity = 3959861.79F,
    marketPrice = 0.00046103F,
    iconUrl = "https://s2.coinmarketcap.com/static/img/coins/64x64/11948.png",
    nftClasses = null
)

// asset for ripple token
private val mockEthereumAssetDto = AssetDto(
    type = "token",
    id = "eth_id",
    name = "Ethereum",
    symbol = "ETH",
    resourceAddress = "bcfsafxy2fafsaffafsq2n0yrf2354dp83kksgder0wlh",
    tokenQuantity = 1F,
    marketPrice = 7853886F,
    iconUrl = null,
    nftClasses = null
)

// asset for android token
private val mockAndroidAssetDto = AssetDto(
    type = "token",
    id = "and_id",
    name = "Android",
    symbol = "AND",
    resourceAddress = "bcfsar34r2fafsaffafsq2n0yrf2354gsd3kksgder0wlh",
    tokenQuantity = 999.9F,
    marketPrice = 10101001101.06F,
    iconUrl = "https://img.icons8.com/cotton/452/android-os.png",
    nftClasses = null
)

val mockAssetDtoList = listOf(
    mockBitcoinAssetDto, // token
    mockRadixAssetDto, // token
    mockEthereumAssetDto, // token
    mockNtfAssetDto, // nft
    mockAndroidAssetDto // token
)

val mockTokenUiList = mockAssetDtoList.filter { assetDto ->
    assetDto.type == AssetType.TOKEN.typeId
}.map { assetDto ->
    assetDto.toTokenUiModel()
}

val mockNftUiList = mockAssetDtoList.filter { assetDto ->
    assetDto.type == AssetType.NTF.typeId
}.flatMap { assetDto ->
    assetDto.nftClasses?.map { nftClassDto ->
        nftClassDto.toNftClassUiModel()
    }.orEmpty()
}
