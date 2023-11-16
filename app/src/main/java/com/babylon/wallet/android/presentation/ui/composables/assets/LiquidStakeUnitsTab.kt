package com.babylon.wallet.android.presentation.ui.composables.assets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.assets.Assets
import com.babylon.wallet.android.domain.model.assets.ValidatorDetail
import com.babylon.wallet.android.domain.model.assets.ValidatorWithStakes
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.XrdResource
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import rdx.works.core.displayableQuantity

const val STAKE_COLLECTION_ID = "-1"

fun SnapshotStateMap<String, Boolean>.isStakeSectionCollapsed() = this[STAKE_COLLECTION_ID] == true

fun LazyListScope.liquidStakeUnitsTab(
    assets: Assets,
    epoch: Long?,
    collapsibleAssetsState: SnapshotStateMap<String, Boolean>,
    action: AssetsViewAction
) {
    if (assets.validatorsWithStakes.isNotEmpty()) {
        item {
            LSUHeader(
                collapsibleAssetsState = collapsibleAssetsState,
                assets = assets
            )
        }

        if (!collapsibleAssetsState.isStakeSectionCollapsed()) {
            itemsIndexed(
                items = assets.validatorsWithStakes,
                key = { _, item -> item.validatorDetail.address }
            ) { index, item ->
                LSUItem(
                    index = index,
                    allSize = assets.validatorsWithStakes.size,
                    epoch = epoch,
                    item = item,
                    action = action
                )
            }
        }
    }
}

@Composable
private fun LSUItem(
    index: Int,
    allSize: Int,
    epoch: Long?,
    item: ValidatorWithStakes,
    action: AssetsViewAction,
) {
    AssetCard(
        modifier = Modifier
            .padding(horizontal = RadixTheme.dimensions.paddingDefault)
            .padding(top = 1.dp),
        itemIndex = index,
        allItemsSize = allSize,
        roundTopCorners = false
    ) {
        LaunchedEffect(item) {
            if (!item.isDetailsAvailable) {
                action.onStakesRequest()
            }
        }

        ValidatorDetailsItem(
            modifier = Modifier
                .padding(horizontal = RadixTheme.dimensions.paddingLarge)
                .padding(
                    top = RadixTheme.dimensions.paddingLarge,
                    bottom = RadixTheme.dimensions.paddingDefault
                ),
            validator = item.validatorDetail
        )

        StakeSectionTitle(
            modifier = Modifier
                .padding(horizontal = RadixTheme.dimensions.paddingXLarge)
                .padding(bottom = RadixTheme.dimensions.paddingSmall),
            title = stringResource(id = R.string.account_poolUnits_liquidStakeUnits)
        )

        LiquidStakeUnitItem(
            modifier = Modifier
                .padding(horizontal = RadixTheme.dimensions.paddingLarge)
                .padding(bottom = RadixTheme.dimensions.paddingDefault),
            stake = item,
            action = action
        )

        if (item.stakeClaimNft != null) {
            StakeSectionTitle(
                modifier = Modifier
                    .padding(horizontal = RadixTheme.dimensions.paddingXLarge)
                    .padding(bottom = RadixTheme.dimensions.paddingSmall),
                title = stringResource(id = R.string.account_poolUnits_stakeClaimNFTs)
            )

            repeat(item.stakeClaimNft.nonFungibleResource.amount.toInt()) { index ->
                val stakeClaimNFT = item.stakeClaimNft.nonFungibleResource.items.getOrNull(index)
                val isLast = index == item.stakeClaimNft.nonFungibleResource.amount.toInt() - 1
                StakeClaimNftItem(
                    modifier = Modifier
                        .padding(horizontal = RadixTheme.dimensions.paddingLarge)
                        .padding(
                            top = if (index > 0) RadixTheme.dimensions.paddingSmall else 0.dp,
                            bottom = if (isLast) RadixTheme.dimensions.paddingDefault else 0.dp
                        ),
                    epoch = epoch,
                    collection = item.stakeClaimNft.nonFungibleResource,
                    stakeClaimNft = stakeClaimNFT,
                    action = action
                )
            }
        }
    }
}

@Composable
private fun LSUHeader(
    collapsibleAssetsState: SnapshotStateMap<String, Boolean>,
    assets: Assets
) {
    val isCollapsed = collapsibleAssetsState.isStakeSectionCollapsed()
    CollapsibleAssetCard(
        modifier = Modifier
            .padding(horizontal = RadixTheme.dimensions.paddingDefault)
            .padding(top = RadixTheme.dimensions.paddingSemiLarge),
        isCollapsed = isCollapsed,
        collapsedItems = assets.validatorsWithStakes.size
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    collapsibleAssetsState[STAKE_COLLECTION_ID] = !collapsibleAssetsState.isStakeSectionCollapsed()
                }
                .padding(RadixTheme.dimensions.paddingLarge),
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_splash),
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RadixTheme.shapes.roundedRectSmall),
                tint = Color.Unspecified
            )
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    stringResource(id = R.string.account_poolUnits_lsuResourceHeader),
                    style = RadixTheme.typography.secondaryHeader,
                    color = RadixTheme.colors.gray1,
                    maxLines = 2
                )
                Text(
                    stringResource(id = R.string.account_poolUnits_numberOfStakes, assets.validatorsWithStakes.size),
                    style = RadixTheme.typography.body2HighImportance,
                    color = RadixTheme.colors.gray2,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ValidatorDetailsItem(validator: ValidatorDetail, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        Thumbnail.Validator(
            modifier = Modifier.size(24.dp),
            validator = validator
        )
        Text(
            validator.name,
            style = RadixTheme.typography.body1Header,
            color = RadixTheme.colors.gray1,
            maxLines = 1
        )
    }
}

@Composable
private fun StakeSectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = title,
        style = RadixTheme.typography.body1HighImportance,
        color = RadixTheme.colors.gray2,
        maxLines = 1
    )
}

@Composable
private fun LiquidStakeUnitItem(
    modifier: Modifier = Modifier,
    stake: ValidatorWithStakes,
    action: AssetsViewAction
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .throttleClickable {
                when (action) {
                    is AssetsViewAction.Click -> {
                        action.onLSUClick(stake.liquidStakeUnit)
                    }

                    is AssetsViewAction.Selection -> {
                        action.onFungibleCheckChanged(
                            stake.liquidStakeUnit.fungibleResource,
                            !action.isSelected(stake.liquidStakeUnit.resourceAddress)
                        )
                    }
                }
            }
            .assetOutlineBorder()
            .padding(RadixTheme.dimensions.paddingDefault),
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        Icon(
            painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_xrd_token),
            contentDescription = null,
            modifier = Modifier
                .size(44.dp)
                .clip(RadixTheme.shapes.circle),
            tint = Color.Unspecified
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(
                text = XrdResource.SYMBOL,
                style = RadixTheme.typography.body2HighImportance,
                color = RadixTheme.colors.gray1,
                maxLines = 2
            )
            Text(
                text = stringResource(id = R.string.account_poolUnits_staked),
                style = RadixTheme.typography.body2HighImportance,
                color = RadixTheme.colors.gray2,
                maxLines = 1
            )
        }

        val stakeValue = remember(stake) { stake.stakeValue() }
        Text(
            modifier = Modifier
                .sizeIn(minWidth = 96.dp)
                .assetPlaceholder(visible = stakeValue == null),
            text = stakeValue?.displayableQuantity().orEmpty(),
            style = RadixTheme.typography.secondaryHeader,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.End,
            maxLines = 1
        )

        if (action is AssetsViewAction.Selection) {
            val isSelected = remember(stake.liquidStakeUnit.resourceAddress, action) {
                action.isSelected(stake.liquidStakeUnit.resourceAddress)
            }
            AssetsViewCheckBox(
                isSelected = isSelected,
                onCheckChanged = { isChecked ->
                    action.onFungibleCheckChanged(stake.liquidStakeUnit.fungibleResource, isChecked)
                }
            )
        }
    }
}

@Composable
private fun StakeClaimNftItem(
    epoch: Long?,
    collection: Resource.NonFungibleResource,
    stakeClaimNft: Resource.NonFungibleResource.Item?,
    modifier: Modifier = Modifier,
    action: AssetsViewAction
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .assetOutlineBorder()
            .throttleClickable(enabled = stakeClaimNft != null) {
                when (action) {
                    is AssetsViewAction.Click -> {
                        if (stakeClaimNft == null) return@throttleClickable
                        action.onNonFungibleItemClick(collection, stakeClaimNft)
                    }

                    is AssetsViewAction.Selection -> {
                        if (stakeClaimNft == null) return@throttleClickable
                        action.onNFTCheckChanged(collection, stakeClaimNft, !action.isSelected(stakeClaimNft.globalAddress))
                    }
                }
            }
            .padding(RadixTheme.dimensions.paddingDefault),
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        Icon(
            painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_xrd_token),
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .clip(RadixTheme.shapes.circle),
            tint = Color.Unspecified
        )

        val isReadyToClaim = remember(stakeClaimNft, epoch) {
            epoch?.let { stakeClaimNft?.isReadyToClaim(epoch) == true } ?: false
        }
        Text(
            modifier = Modifier
                .weight(1f)
                .assetPlaceholder(visible = stakeClaimNft == null || epoch == null),
            text = stringResource(
                id = if (isReadyToClaim) R.string.account_poolUnits_readyToClaim else R.string.account_poolUnits_unstaking
            ),
            style = RadixTheme.typography.body2HighImportance,
            color = if (isReadyToClaim) RadixTheme.colors.green1 else RadixTheme.colors.gray1,
            maxLines = 1
        )

        Text(
            text = stakeClaimNft?.claimAmountXrd?.displayableQuantity().orEmpty(),
            style = RadixTheme.typography.secondaryHeader,
            color = RadixTheme.colors.gray1,
            maxLines = 1
        )

        if (action is AssetsViewAction.Selection && stakeClaimNft != null) {
            val isSelected = remember(stakeClaimNft, action) {
                action.isSelected(stakeClaimNft.globalAddress)
            }
            AssetsViewCheckBox(
                isSelected = isSelected,
                onCheckChanged = { isChecked ->
                    action.onNFTCheckChanged(collection, stakeClaimNft, isChecked)
                }
            )
        }
    }
}
