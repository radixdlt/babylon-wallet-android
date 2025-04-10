package com.babylon.wallet.android.presentation.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R
import com.babylon.wallet.android.presentation.transfer.SpendingAsset
import com.radixdlt.sargon.extensions.formatted
import rdx.works.core.domain.assets.LiquidStakeUnit
import rdx.works.core.domain.assets.NonFungibleCollection
import rdx.works.core.domain.assets.PoolUnit
import rdx.works.core.domain.assets.StakeClaim
import rdx.works.core.domain.resources.Badge
import rdx.works.core.domain.resources.Resource

// When we have the distinction between an NFT and a claim the subtitle for claims will change to the validator's name
@Composable
fun SpendingAsset.NFT.displaySubtitle(): String = item.displaySubtitle()

@Composable
fun Badge.displaySubtitle(): String? {
    val nftResource = resource as? Resource.NonFungibleResource ?: return null
    val item = nftResource.items.firstOrNull() ?: return null

    return item.displaySubtitle()
}

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
fun Resource.NonFungibleResource.displaySubtitle(): String = stringResource(id = R.string.account_nfts_itemsCount, amount)

@Composable
fun Resource.NonFungibleResource.Item.displaySubtitle(): String = remember(this) {
    nameTruncated?.takeIf { it.isNotBlank() } ?: localId.formatted()
}
