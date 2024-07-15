package com.babylon.wallet.android.presentation.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.presentation.transfer.SpendingAsset
import com.radixdlt.sargon.extensions.formatted
import rdx.works.core.domain.assets.LiquidStakeUnit
import rdx.works.core.domain.assets.NonFungibleCollection
import rdx.works.core.domain.assets.PoolUnit
import rdx.works.core.domain.assets.StakeClaim
import rdx.works.core.domain.resources.Resource

@Composable
fun TransferableAsset.Fungible.LSUAsset.displaySubtitle(): String = lsu.displaySubtitle()
@Composable
fun TransferableAsset.Fungible.PoolUnitAsset.displaySubtitle(): String = unit.displaySubtitle()
@Composable
fun TransferableAsset.NonFungible.StakeClaimAssets.displaySubtitle(): String = claim.displaySubtitle()

// When we have the distinction between an NFT and a claim the subtitle for claims will change to the validator's name
@Composable
fun SpendingAsset.NFT.displaySubtitle(): String = item.displaySubtitle()

@Composable
fun NonFungibleCollection.displaySubtitle(): String = resource.displaySubtitle()

@Composable
fun LiquidStakeUnit.displaySubtitle(): String = validator.name.takeIf {
    it.isNotBlank()
} ?: stringResource(id = R.string.dash)

@Composable
fun PoolUnit.displaySubtitle(): String = this.pool?.associatedDApp?.name?.takeIf {
    it.isNotBlank()
} ?: stringResource(id = R.string.dash)

@Composable
fun StakeClaim.displaySubtitle(): String = validator.name.takeIf {
    it.isNotBlank()
} ?: stringResource(id = R.string.dash)

@Composable
fun Resource.NonFungibleResource.displaySubtitle(): String = amount.toString()

@Composable
fun Resource.NonFungibleResource.Item.displaySubtitle(): String = remember(this) {
    name?.takeIf { it.isNotBlank() } ?: localId.formatted()
}