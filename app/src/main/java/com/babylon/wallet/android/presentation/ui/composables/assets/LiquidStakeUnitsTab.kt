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
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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

fun LazyListScope.liquidStakeUnitsTab(
    assets: Assets,
    collapsibleAssetsState: SnapshotStateMap<String, CollapsibleAssetState>,
    action: AssetsViewAction
) {
    if (assets.validatorsWithStakes.isNotEmpty()) {
        item {
            val viewState = collapsibleAssetsState[CollapsibleAssetState.STAKE_SECTION_ID]
            CollapsibleAssetCard(
                modifier = Modifier
                    .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                    .padding(top = RadixTheme.dimensions.paddingSemiLarge),
                isCollapsed = viewState?.isCollapsed ?: true,
                collapsedItems = assets.validatorsWithStakes.size
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (viewState != null) {
                                collapsibleAssetsState[CollapsibleAssetState.STAKE_SECTION_ID] = viewState.copy(
                                    isCollapsed = !viewState.isCollapsed
                                )
                            }
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

        if (collapsibleAssetsState[CollapsibleAssetState.STAKE_SECTION_ID]?.isCollapsed == false) {
            itemsIndexed(
                items = assets.validatorsWithStakes,
                key = { _, item -> item.validatorDetail.address }
            ) { index, item ->
                AssetCard(
                    modifier = Modifier
                        .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                        .padding(top = 1.dp),
                    itemIndex = index,
                    allItemsSize = assets.validatorsWithStakes.size,
                    roundTopCorners = false
                ) {
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
                                stakeClaimNft = stakeClaimNFT,
                                action = action
                            )
                        }
                    }
                }
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
                        action.onLSUClick(stake.liquidStakeUnit, stake.validatorDetail)
                    }
                    is AssetsViewAction.Selection -> {
                        val isSelected = action.isSelected(stake.liquidStakeUnit.resourceAddress)
                        action.onResourceCheckChanged(stake.liquidStakeUnit.resourceAddress, !isSelected)
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

        val stakeValue = remember(stake) {
            stake.liquidStakeUnit.stakeValueInXRD(stake.validatorDetail.totalXrdStake)
        }
        Text(
            modifier = Modifier
                .sizeIn(minWidth = 96.dp)
                .assetPlaceholder(visible = stakeValue == null),
            text = stakeValue?.displayableQuantity().orEmpty(),
            style = RadixTheme.typography.secondaryHeader,
            color = RadixTheme.colors.gray1,
            maxLines = 1
        )

        if (action is AssetsViewAction.Selection) {
            AssetsViewCheckBox(
                resourceAddress = stake.liquidStakeUnit.resourceAddress,
                action = action
            )
        }
    }
}

@Composable
private fun StakeClaimNftItem(
    stakeClaimNft: Resource.NonFungibleResource.Item?,
    modifier: Modifier = Modifier,
    action: AssetsViewAction
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .assetOutlineBorder()
            .throttleClickable(enabled = action is AssetsViewAction.Selection && stakeClaimNft != null) {
                if (action is AssetsViewAction.Selection && stakeClaimNft != null) {
                    val isSelected = action.isSelected(stakeClaimNft.globalAddress)
                    action.onResourceCheckChanged(stakeClaimNft.globalAddress, !isSelected)
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

        Text(
            modifier = Modifier
                .weight(1f)
                .assetPlaceholder(visible = stakeClaimNft == null),
            text = if (stakeClaimNft != null) {
                stringResource(
                    id = if (stakeClaimNft.readyToClaim) R.string.account_poolUnits_readyToClaim else R.string.account_poolUnits_unstaking
                )
            } else {
                ""
            },
            style = RadixTheme.typography.body2HighImportance,
            color = if (stakeClaimNft != null) {
                if (stakeClaimNft.readyToClaim) RadixTheme.colors.green1 else RadixTheme.colors.gray1
            } else {
                Color.Transparent
            },
            maxLines = 1
        )

        Text(
            text = stakeClaimNft?.claimAmountXrd?.displayableQuantity().orEmpty(),
            style = RadixTheme.typography.secondaryHeader,
            color = RadixTheme.colors.gray1,
            maxLines = 1
        )

        if (action is AssetsViewAction.Selection && stakeClaimNft != null) {
            AssetsViewCheckBox(
                resourceAddress = stakeClaimNft.globalAddress,
                action = action
            )
        }
    }
}
