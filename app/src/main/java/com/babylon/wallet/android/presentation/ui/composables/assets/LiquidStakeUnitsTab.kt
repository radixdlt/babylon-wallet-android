package com.babylon.wallet.android.presentation.ui.composables.assets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.babylon.wallet.android.presentation.ui.modifier.applyIf
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
    if (assets.ownedValidatorsWithStakes.isNotEmpty()) {
        item {
            LSUHeader(
                collapsibleAssetsState = collapsibleAssetsState,
                assets = assets
            )
        }

        if (!collapsibleAssetsState.isStakeSectionCollapsed()) {
            itemsIndexed(
                items = assets.ownedValidatorsWithStakes,
                key = { _, item -> item.validatorDetail.address }
            ) { index, item ->
                LSUItem(
                    index = index,
                    allSize = assets.ownedValidatorsWithStakes.size,
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

        if (item.liquidStakeUnit != null) {
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
        }

        if (item.stakeClaimNft != null && item.stakeClaimNft.nonFungibleResource.amount > 0) {
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
        collapsedItems = assets.ownedValidatorsWithStakes.size
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
                    stringResource(id = R.string.account_poolUnits_numberOfStakes, assets.ownedValidatorsWithStakes.size),
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
                        stake.liquidStakeUnit?.let { action.onLSUClick(it) }
                    }

                    is AssetsViewAction.Selection -> {
                        stake.liquidStakeUnit?.let { liquidStakeUnit ->
                            action.onFungibleCheckChanged(
                                liquidStakeUnit.fungibleResource,
                                !action.isSelected(liquidStakeUnit.resourceAddress)
                            )
                        }
                    }
                }
            }
            .assetOutlineBorder()
            .padding(vertical = RadixTheme.dimensions.paddingDefault)
            .padding(start = RadixTheme.dimensions.paddingDefault)
    ) {
        Icon(
            painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_xrd_token),
            contentDescription = null,
            modifier = Modifier
                .size(44.dp)
                .clip(RadixTheme.shapes.circle),
            tint = Color.Unspecified
        )

        Column(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingMedium),
            verticalArrangement = Arrangement.Center
        ) {
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
                .weight(1f)
                .padding(
                    end = if (action is AssetsViewAction.Click || stakeValue == null) RadixTheme.dimensions.paddingDefault else 0.dp
                )
                .assetPlaceholder(visible = stakeValue == null),
            text = stakeValue?.displayableQuantity().orEmpty(),
            style = RadixTheme.typography.secondaryHeader,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.End,
            maxLines = 2
        )

        if (action is AssetsViewAction.Selection && stakeValue != null) {
            stake.liquidStakeUnit?.let { liquidStakeUnit ->
                val isSelected = remember(liquidStakeUnit.resourceAddress, action) {
                    action.isSelected(liquidStakeUnit.resourceAddress)
                }
                AssetsViewCheckBox(
                    isSelected = isSelected,
                    onCheckChanged = { isChecked ->
                        action.onFungibleCheckChanged(liquidStakeUnit.fungibleResource, isChecked)
                    }
                )
            }
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
    val isReadyToClaim = remember(stakeClaimNft, epoch) {
        if (stakeClaimNft != null && epoch != null) {
            stakeClaimNft.isReadyToClaim(epoch)
        } else {
            null
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
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
            .padding(
                vertical = if (action is AssetsViewAction.Click || isReadyToClaim == null) {
                    RadixTheme.dimensions.paddingDefault
                } else {
                    RadixTheme.dimensions.paddingSmall
                }
            )
            .padding(start = RadixTheme.dimensions.paddingDefault)
    ) {
        Icon(
            painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_xrd_token),
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .clip(RadixTheme.shapes.circle),
            tint = Color.Unspecified
        )

        Text(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = RadixTheme.dimensions.paddingMedium),
            text = if (isReadyToClaim != null) {
                stringResource(id = if (isReadyToClaim) R.string.account_poolUnits_readyToClaim else R.string.account_poolUnits_unstaking)
            } else {
                ""
            },
            style = RadixTheme.typography.body2HighImportance,
            color = if (isReadyToClaim == true) RadixTheme.colors.green1 else RadixTheme.colors.gray1,
            textAlign = TextAlign.Start,
            maxLines = 1
        )

        Text(
            modifier = Modifier
                .weight(1f)
                .padding(
                    end = if (action is AssetsViewAction.Click || isReadyToClaim == null) {
                        RadixTheme.dimensions.paddingDefault
                    } else {
                        0.dp
                    }
                )
                .assetPlaceholder(visible = isReadyToClaim == null),
            text = stakeClaimNft?.claimAmountXrd?.displayableQuantity().orEmpty(),
            style = RadixTheme.typography.secondaryHeader,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.End,
            maxLines = 2
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
