package com.babylon.wallet.android.presentation.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R
import com.babylon.wallet.android.presentation.transaction.model.Transferable
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
fun Transferable.displayTitle(
    fallback: @Composable () -> String = { stringResource(id = R.string.dash) }
): String = when (this) {
    is Transferable.FungibleType.LSU -> asset.resource.displayTitleAsLSU()
    is Transferable.FungibleType.PoolUnit -> asset.resource.displayTitleAsPoolUnit()
    is Transferable.FungibleType.Token -> asset.resource.displayTitleAsToken(fallback)
    is Transferable.NonFungibleType.NFTCollection -> asset.resource.displayTitleAsNFTCollection(fallback)
    is Transferable.NonFungibleType.StakeClaim -> asset.resource.displayTitleAsStakeClaimNFTCollection()
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
