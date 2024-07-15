package com.babylon.wallet.android.presentation.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.presentation.transfer.SpendingAsset
import rdx.works.core.domain.assets.Asset
import rdx.works.core.domain.assets.LiquidStakeUnit
import rdx.works.core.domain.assets.NonFungibleCollection
import rdx.works.core.domain.assets.PoolUnit
import rdx.works.core.domain.assets.StakeClaim
import rdx.works.core.domain.assets.Token
import rdx.works.core.domain.resources.Badge
import rdx.works.core.domain.resources.Resource

// //// ASSET LEVEL

@Composable
fun Asset.displayTitle(
    fallback: @Composable () -> String = { stringResource(id = R.string.dash) }
): String = when (this) {
    is Token -> resource.displayTitleAsToken(fallback)
    is LiquidStakeUnit -> resource.displayTitleAsLSU()
    is PoolUnit -> resource.displayTitleAsPoolUnit(fallback)
    is NonFungibleCollection -> resource.displayTitleAsNFTCollection(fallback)
    is StakeClaim -> resource.displayTitleAsStakeClaimNFTCollection()
}

@Composable
fun SpendingAsset.displayTitle(
    fallback: @Composable () -> String = { stringResource(id = R.string.dash) }
): String = when (this) {
    is SpendingAsset.Fungible -> resource.displayTitleAsToken(fallback)
    is SpendingAsset.NFT -> resource.displayTitleAsNFTCollection(fallback)
}

@Composable
fun TransferableAsset.displayTitle(
    fallback: @Composable () -> String = { stringResource(id = R.string.dash) }
): String = when (this) {
    is TransferableAsset.Fungible.Token -> resource.displayTitleAsToken(fallback)
    is TransferableAsset.Fungible.LSUAsset -> resource.displayTitleAsLSU()
    is TransferableAsset.Fungible.PoolUnitAsset -> resource.displayTitleAsPoolUnit()
    is TransferableAsset.NonFungible.NFTAssets -> resource.displayTitleAsNFTCollection(fallback)
    is TransferableAsset.NonFungible.StakeClaimAssets -> resource.displayTitleAsStakeClaimNFTCollection()
}

@Composable
fun Badge.displayTitle(): String = when (resource) {
    is Resource.FungibleResource -> resource.name.takeIf { it.isNotBlank() } ?: stringResource(id = R.string.dash)
    is Resource.NonFungibleResource -> resource.name.takeIf { it.isNotBlank() } ?: stringResource(id = R.string.dash)
}

// //// RESOURCE LEVEL

// / Fungibles
@Composable
fun Resource.FungibleResource.displayTitleAsToken(
    fallback: @Composable () -> String = { stringResource(id = R.string.dash) }
) = if (symbol.isNotBlank()) {
    symbol
} else if (name.isNotBlank()) {
    name
} else {
    fallback()
}

@Composable
fun Resource.FungibleResource.displayTitleAsLSU() = stringResource(id = R.string.account_staking_liquidStakeUnits)

@Composable
fun Resource.FungibleResource.displayTitleAsPoolUnit(
    fallback: @Composable () -> String = { stringResource(id = R.string.dash) }
) = displayTitleAsToken(fallback = fallback)

// / Non Fungibles

@Composable
fun Resource.NonFungibleResource.displayTitleAsNFTCollection(
    fallback: @Composable () -> String = { stringResource(id = R.string.dash) }
) = name.takeIf { it.isNotBlank() } ?: fallback()

@Composable
fun Resource.NonFungibleResource.displayTitleAsStakeClaimNFTCollection() = stringResource(id = R.string.account_staking_stakeClaimNFTs)
