package com.babylon.wallet.android.presentation.ui.composables.assets

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.assets.AssetPrice
import com.babylon.wallet.android.domain.model.assets.FiatPrice
import com.babylon.wallet.android.domain.model.assets.ValidatorWithStakes
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.XrdResource
import com.babylon.wallet.android.presentation.account.composable.EmptyResourcesContent
import com.babylon.wallet.android.presentation.transfer.assets.AssetsTab
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.ShimmeringView
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import rdx.works.core.displayableQuantity
import java.math.BigDecimal

fun LazyListScope.stakingTab(
    assetsViewData: AssetsViewData,
    isLoadingBalance: Boolean,
    state: AssetsViewState,
    action: AssetsViewAction
) {
    if (assetsViewData.isValidatorWithStakesEmpty) {
        item {
            EmptyResourcesContent(
                modifier = Modifier.fillMaxWidth(),
                tab = AssetsTab.Staking
            )
        }
    } else {
        item {
            StakingSummary(
                assetsViewData = assetsViewData,
                isLoadingBalance = isLoadingBalance,
                action = action
            )
        }

        item {
            ValidatorsSize(assetsViewData = assetsViewData)
        }

        assetsViewData.validatorsWithStakes.forEachIndexed { index, validatorWithStakes ->
            item(
                key = validatorWithStakes.validatorDetail.address,
                contentType = { "validator-header" }
            ) {
                ValidatorDetails(
                    modifier = Modifier.padding(top = if (index != 0) RadixTheme.dimensions.paddingDefault else 0.dp),
                    assetsViewData = assetsViewData,
                    validatorWithStakes = validatorWithStakes,
                    state = state,
                    epoch = assetsViewData.epoch,
                    isLoadingBalance = isLoadingBalance,
                    action = action
                )
            }
        }
    }
}

@Suppress("CyclomaticComplexMethod")
@Composable
private fun StakingSummary(
    modifier: Modifier = Modifier,
    assetsViewData: AssetsViewData,
    isLoadingBalance: Boolean,
    action: AssetsViewAction
) {
    val stakeSummary = assetsViewData.stakeSummary
    AssetCard(
        modifier = modifier
            .padding(horizontal = RadixTheme.dimensions.paddingDefault)
            .padding(top = RadixTheme.dimensions.paddingSemiLarge),
        itemIndex = 0,
        allItemsSize = 1
    ) {
        Row(
            modifier = Modifier
                .padding(
                    horizontal = RadixTheme.dimensions.paddingDefault,
                    vertical = RadixTheme.dimensions.paddingSemiLarge
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
        ) {
            Image(
                modifier = Modifier.size(56.dp),
                painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_lsu),
                contentDescription = null
            )

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.account_staking_lsuResourceHeader),
                style = RadixTheme.typography.secondaryHeader,
                color = RadixTheme.colors.gray1,
                maxLines = 2
            )
        }

        LaunchedEffect(stakeSummary) {
            if (stakeSummary == null) {
                action.onStakesRequest()
            }
        }

        val oneXrdPrice = remember(assetsViewData) {
            val oneXrdPrice = (
                assetsViewData.prices?.values?.find {
                    (it as? AssetPrice.LSUPrice)?.oneXrdPrice != null
                } as? AssetPrice.LSUPrice
                )?.oneXrdPrice
            oneXrdPrice
                ?: (
                    assetsViewData.prices?.values?.find {
                        (it as? AssetPrice.StakeClaimPrice)?.oneXrdPrice != null
                    } as? AssetPrice.StakeClaimPrice
                    )?.oneXrdPrice
        }

        val stakedFiatPrice = remember(stakeSummary, oneXrdPrice) {
            if (stakeSummary != null && oneXrdPrice != null && stakeSummary.hasStakedValue) {
                FiatPrice(
                    price = stakeSummary.staked.multiply(oneXrdPrice.price.toBigDecimal()).toDouble(),
                    currency = oneXrdPrice.currency
                )
            } else {
                null
            }
        }

        val unstakingFiatPrice = remember(stakeSummary, oneXrdPrice) {
            if (stakeSummary != null && oneXrdPrice != null && stakeSummary.unstaking > BigDecimal.ZERO) {
                FiatPrice(
                    price = stakeSummary.unstaking.multiply(oneXrdPrice.price.toBigDecimal()).toDouble(),
                    currency = oneXrdPrice.currency
                )
            } else {
                null
            }
        }

        val readyToClaimFiatPrice = remember(stakeSummary, oneXrdPrice) {
            if (stakeSummary != null && oneXrdPrice != null && stakeSummary.hasReadyToClaimValue) {
                FiatPrice(
                    price = stakeSummary.readyToClaim.multiply(oneXrdPrice.price.toBigDecimal()).toDouble(),
                    currency = oneXrdPrice.currency
                )
            } else {
                null
            }
        }

        StakeAmount(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingLarge),
            label = stringResource(id = R.string.account_staking_staked),
            amount = stakeSummary?.staked,
            fiatPrice = stakedFiatPrice,
            isLoadingBalance = isLoadingBalance,
            amountStyle = RadixTheme.typography.body2HighImportance.copy(
                color = if (assetsViewData.stakeSummary?.hasStakedValue == true) RadixTheme.colors.gray1 else RadixTheme.colors.gray2
            )
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
        StakeAmount(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingLarge),
            label = stringResource(id = R.string.account_staking_unstaking),
            amount = stakeSummary?.unstaking,
            fiatPrice = unstakingFiatPrice,
            isLoadingBalance = isLoadingBalance
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
        StakeAmount(
            modifier = Modifier
                .padding(horizontal = RadixTheme.dimensions.paddingLarge)
                .clickable(enabled = stakeSummary?.hasReadyToClaimValue == true && action is AssetsViewAction.Click) {
                    if (action is AssetsViewAction.Click) {
                        val claims = assetsViewData.validatorsWithStakes
                            .filter { it.hasClaims }
                            .mapNotNull { it.stakeClaimNft }

                        action.onClaimClick(claims)
                    }
                },
            label = stringResource(id = R.string.account_staking_readyToClaim),
            amount = stakeSummary?.readyToClaim,
            fiatPrice = readyToClaimFiatPrice,
            isLoadingBalance = isLoadingBalance,
            labelStyle = if (stakeSummary?.hasReadyToClaimValue == true) {
                RadixTheme.typography.body2Link.copy(color = RadixTheme.colors.blue2)
            } else {
                RadixTheme.typography.body2HighImportance.copy(color = RadixTheme.colors.gray2)
            }
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
    }
}

@Composable
private fun StakeAmount(
    modifier: Modifier = Modifier,
    label: String,
    isLoadingBalance: Boolean,
    amount: BigDecimal?,
    fiatPrice: FiatPrice?,
    labelStyle: TextStyle = RadixTheme.typography.body2HighImportance.copy(
        color = RadixTheme.colors.gray2
    ),
    amountStyle: TextStyle = RadixTheme.typography.body2HighImportance.copy(
        color = RadixTheme.colors.gray2
    )
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = label,
            style = labelStyle,
            maxLines = 1
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .radixPlaceholder(visible = amount == null),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = amount?.let { "${it.displayableQuantity()} ${XrdResource.SYMBOL}" }.orEmpty(),
                style = amountStyle,
                textAlign = TextAlign.End
            )

            if (isLoadingBalance) {
                ShimmeringView(
                    modifier = Modifier
                        .padding(top = RadixTheme.dimensions.paddingXXSmall)
                        .height(10.dp)
                        .fillMaxWidth(0.3f),
                    isVisible = true
                )
            } else if (fiatPrice != null) {
                FiatBalanceView(
                    fiatPrice = fiatPrice,
                    textStyle = amountStyle
                )
            }
        }
    }
}

@Composable
fun ValidatorsSize(
    modifier: Modifier = Modifier,
    assetsViewData: AssetsViewData
) {
    Row(
        modifier = modifier
            .padding(horizontal = RadixTheme.dimensions.paddingDefault)
            .padding(
                top = RadixTheme.dimensions.paddingLarge,
                bottom = RadixTheme.dimensions.paddingSmall
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
    ) {
        Icon(
            modifier = Modifier
                .size(24.dp)
                .dashedCircleBorder(RadixTheme.colors.gray3)
                .padding(RadixTheme.dimensions.paddingXSmall),
            painter = painterResource(id = DSR.ic_validator),
            tint = RadixTheme.colors.gray2,
            contentDescription = null
        )

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.account_staking_stakedValidators, assetsViewData.validatorsWithStakes.size),
            style = RadixTheme.typography.body1HighImportance,
            color = RadixTheme.colors.gray2
        )
    }
}

@Composable
fun ValidatorDetails(
    modifier: Modifier = Modifier,
    assetsViewData: AssetsViewData,
    validatorWithStakes: ValidatorWithStakes,
    isLoadingBalance: Boolean,
    epoch: Long?,
    state: AssetsViewState,
    action: AssetsViewAction
) {
    Column {
        val collapsedCardsCount = remember(validatorWithStakes) {
            var cards = 0
            val stake = validatorWithStakes.liquidStakeUnit?.fungibleResource?.ownedAmount ?: BigDecimal.ZERO
            val claims = validatorWithStakes.stakeClaimNft?.nonFungibleResource?.amount ?: 0L
            if (stake > BigDecimal.ZERO) {
                cards += 1
            }
            if (claims > 0) {
                cards += 1
            }
            cards
        }
        val isCollapsed = state.isCollapsed(validatorWithStakes.validatorDetail.address)
        CollapsibleAssetCard(
            modifier = modifier
                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
            isCollapsed = isCollapsed,
            collapsedItems = collapsedCardsCount
        ) {
            ValidatorHeader(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        action.onCollectionClick(validatorWithStakes.validatorDetail.address)
                    }
                    .padding(RadixTheme.dimensions.paddingLarge),
                validatorWithStakes = validatorWithStakes
            )
        }

        if (!isCollapsed) {
            if (validatorWithStakes.hasLSU) {
                AssetCard(
                    modifier = Modifier
                        .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                        .padding(top = RadixTheme.dimensions.paddingXXSmall),
                    roundTopCorners = false,
                    roundBottomCorners = !validatorWithStakes.hasClaims
                ) {
                    LiquidStakeUnit(
                        validatorWithStakes = validatorWithStakes,
                        assetsViewData = assetsViewData,
                        isLoadingBalance = isLoadingBalance,
                        action = action
                    )
                }
            }

            if (validatorWithStakes.hasClaims) {
                AssetCard(
                    modifier = Modifier
                        .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                        .padding(top = RadixTheme.dimensions.paddingXXSmall),
                    roundTopCorners = false,
                    roundBottomCorners = true
                ) {
                    StakeClaims(
                        assetsViewData = assetsViewData,
                        validatorWithStakes = validatorWithStakes,
                        epoch = epoch,
                        isLoadingBalance = isLoadingBalance,
                        action = action
                    )
                }
            }
        }
    }
}

@Composable
private fun LiquidStakeUnit(
    modifier: Modifier = Modifier,
    assetsViewData: AssetsViewData,
    validatorWithStakes: ValidatorWithStakes,
    isLoadingBalance: Boolean,
    action: AssetsViewAction,
) {
    Column(
        modifier = modifier
            .throttleClickable {
                when (action) {
                    is AssetsViewAction.Click -> {
                        validatorWithStakes.liquidStakeUnit?.let { action.onLSUClick(it) }
                    }

                    is AssetsViewAction.Selection -> {
                        validatorWithStakes.liquidStakeUnit?.let { liquidStakeUnit ->
                            action.onFungibleCheckChanged(
                                liquidStakeUnit.fungibleResource,
                                !action.isSelected(liquidStakeUnit.resourceAddress)
                            )
                        }
                    }
                }
            }
            .padding(RadixTheme.dimensions.paddingDefault)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
        ) {
            Thumbnail.LSU(
                modifier = Modifier.size(36.dp),
                liquidStakeUnit = validatorWithStakes.liquidStakeUnit
            )

            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(id = R.string.account_staking_liquidStakeUnits),
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.gray1
            )

            if (action is AssetsViewAction.Selection && validatorWithStakes.liquidStakeUnit != null) {
                val isSelected = remember(validatorWithStakes.liquidStakeUnit.resourceAddress, action) {
                    action.isSelected(validatorWithStakes.liquidStakeUnit.resourceAddress)
                }
                AssetsViewCheckBox(
                    isSelected = isSelected,
                    onCheckChanged = { isChecked ->
                        action.onFungibleCheckChanged(validatorWithStakes.liquidStakeUnit.fungibleResource, isChecked)
                    }
                )
            }
        }

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = RadixTheme.dimensions.paddingDefault,
                    bottom = RadixTheme.dimensions.paddingSmall
                ),
            text = stringResource(id = R.string.account_staking_worth),
            style = RadixTheme.typography.body2HighImportance,
            color = RadixTheme.colors.gray2
        )

        val fiatPrice = remember(validatorWithStakes) {
            val lsuPrice = validatorWithStakes.liquidStakeUnit?.let {
                assetsViewData.prices?.get(it)
            } as? AssetPrice.LSUPrice

            validatorWithStakes.stakeValue()?.let { lsuPrice?.xrdPrice(it) }
        }

        WorthXRD(
            amount = remember(validatorWithStakes) { validatorWithStakes.stakeValue() },
            fiatPrice = fiatPrice,
            isLoadingBalance = isLoadingBalance
        )
    }
}

@Composable
private fun StakeClaims(
    modifier: Modifier = Modifier,
    assetsViewData: AssetsViewData,
    validatorWithStakes: ValidatorWithStakes,
    isLoadingBalance: Boolean,
    epoch: Long?,
    action: AssetsViewAction
) {
    Column(
        modifier = modifier.padding(RadixTheme.dimensions.paddingDefault)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
        ) {
            Thumbnail.NonFungible(
                modifier = Modifier.size(36.dp),
                collection = validatorWithStakes.stakeClaimNft?.nonFungibleResource
            )

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.account_staking_stakeClaimNFTs),
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.gray1
            )
        }

        if (!validatorWithStakes.isDetailsAvailable) {
            Box(
                modifier = Modifier
                    .padding(top = RadixTheme.dimensions.paddingDefault)
                    .fillMaxWidth()
                    .height(24.dp)
                    .radixPlaceholder(visible = true)
            )
        }

        val (unstakingItems, claimItems) = remember(validatorWithStakes, epoch) {
            if (epoch == null) return@remember emptyList<Resource.NonFungibleResource.Item>() to emptyList()
            validatorWithStakes.stakeClaimNft
                ?.unstakingNFTs(epoch).orEmpty() to validatorWithStakes.stakeClaimNft?.readyToClaimNFTs(epoch).orEmpty()
        }

        if (unstakingItems.isNotEmpty() && validatorWithStakes.stakeClaimNft != null) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = RadixTheme.dimensions.paddingDefault,
                        bottom = RadixTheme.dimensions.paddingMedium
                    ),
                text = stringResource(id = R.string.account_staking_unstaking).uppercase(),
                style = RadixTheme.typography.body2HighImportance,
                color = RadixTheme.colors.gray2
            )

            val stakeClaimPrice = assetsViewData.prices?.get(validatorWithStakes.stakeClaimNft) as? AssetPrice.StakeClaimPrice
            unstakingItems.forEachIndexed { index, item ->
                ClaimWorth(
                    modifier = Modifier.padding(top = if (index != 0) RadixTheme.dimensions.paddingSmall else 0.dp),
                    claimCollection = validatorWithStakes.stakeClaimNft.nonFungibleResource,
                    claimNft = item,
                    stakeClaimPrice = stakeClaimPrice,
                    isLoadingBalance = isLoadingBalance,
                    action = action
                )
            }
        }

        if (claimItems.isNotEmpty() && validatorWithStakes.stakeClaimNft != null) {
            Row(
                modifier = Modifier.padding(
                    top = if (action is AssetsViewAction.Selection) {
                        RadixTheme.dimensions.paddingDefault
                    } else {
                        RadixTheme.dimensions.paddingSmall
                    },
                    bottom = if (action is AssetsViewAction.Selection) RadixTheme.dimensions.paddingSmall else 0.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(id = R.string.account_staking_readyToBeClaimed).uppercase(),
                    style = RadixTheme.typography.body2HighImportance,
                    color = RadixTheme.colors.gray2
                )

                if (action is AssetsViewAction.Click) {
                    RadixTextButton(
                        text = stringResource(id = R.string.account_staking_claim),
                        onClick = {
                            action.onClaimClick(listOf(validatorWithStakes.stakeClaimNft))
                        },
                        textStyle = RadixTheme.typography.body2Link
                    )
                }
            }

            val stakeClaimPrice = assetsViewData.prices?.get(validatorWithStakes.stakeClaimNft) as? AssetPrice.StakeClaimPrice
            claimItems.forEachIndexed { index, item ->
                ClaimWorth(
                    modifier = Modifier.padding(top = if (index != 0) RadixTheme.dimensions.paddingSmall else 0.dp),
                    claimCollection = validatorWithStakes.stakeClaimNft.nonFungibleResource,
                    claimNft = item,
                    stakeClaimPrice = stakeClaimPrice,
                    isLoadingBalance = isLoadingBalance,
                    action = action
                )
            }
        }
    }
}

@Composable
private fun ValidatorHeader(
    modifier: Modifier = Modifier,
    validatorWithStakes: ValidatorWithStakes
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
    ) {
        Thumbnail.Validator(
            modifier = Modifier.size(44.dp),
            validator = validatorWithStakes.validatorDetail
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
        ) {
            Text(
                text = validatorWithStakes.validatorDetail.name,
                style = RadixTheme.typography.secondaryHeader,
                color = RadixTheme.colors.gray1,
                maxLines = 1
            )

            val stakedAmount = remember(validatorWithStakes) {
                if (validatorWithStakes.liquidStakeUnit != null) {
                    validatorWithStakes.stakeValue()
                } else {
                    BigDecimal.ZERO
                }
            }

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .radixPlaceholder(visible = stakedAmount == null),
                text = stakedAmount?.let {
                    "${stringResource(id = R.string.account_staking_staked)} ${it.displayableQuantity()} ${XrdResource.SYMBOL}"
                }.orEmpty(),
                style = RadixTheme.typography.body2HighImportance,
                color = RadixTheme.colors.gray2,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ClaimWorth(
    modifier: Modifier = Modifier,
    claimCollection: Resource.NonFungibleResource,
    claimNft: Resource.NonFungibleResource.Item,
    stakeClaimPrice: AssetPrice.StakeClaimPrice?,
    isLoadingBalance: Boolean,
    action: AssetsViewAction
) {
    val fiatPrice = remember(stakeClaimPrice, claimNft) {
        stakeClaimPrice?.xrdPrice(claimNft)
    }
    WorthXRD(
        modifier = modifier.throttleClickable {
            when (action) {
                is AssetsViewAction.Click -> {
                    action.onNonFungibleItemClick(claimCollection, claimNft)
                }

                is AssetsViewAction.Selection -> {
                    action.onNFTCheckChanged(
                        claimCollection,
                        claimNft,
                        !action.isSelected(claimNft.globalAddress)
                    )
                }
            }
        },
        amount = remember(claimNft) { claimNft.claimAmountXrd },
        fiatPrice = fiatPrice,
        trailingContent = if (action is AssetsViewAction.Selection) {
            {
                val isSelected = remember(claimNft, action) {
                    action.isSelected(claimNft.globalAddress)
                }
                AssetsViewCheckBox(
                    isSelected = isSelected,
                    onCheckChanged = { isChecked ->
                        action.onNFTCheckChanged(
                            claimCollection,
                            claimNft,
                            isChecked
                        )
                    }
                )
            }
        } else {
            null
        },
        isLoadingBalance = isLoadingBalance
    )
}

@Composable
fun WorthXRD(
    modifier: Modifier = Modifier,
    amount: BigDecimal?,
    fiatPrice: FiatPrice?,
    isLoadingBalance: Boolean,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .assetOutlineBorder()
            .padding(
                vertical = if (trailingContent == null || amount == null) {
                    RadixTheme.dimensions.paddingDefault
                } else {
                    RadixTheme.dimensions.paddingSmall
                }
            )
            .padding(start = RadixTheme.dimensions.paddingDefault),
        verticalAlignment = Alignment.CenterVertically,
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
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingSmall),
            text = XrdResource.SYMBOL,
            style = RadixTheme.typography.body2HighImportance,
            color = RadixTheme.colors.gray1,
            maxLines = 1
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(
                    end = if (trailingContent == null || amount == null) RadixTheme.dimensions.paddingDefault else 0.dp
                )
                .assetPlaceholder(visible = amount == null),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = amount?.displayableQuantity().orEmpty(),
                style = RadixTheme.typography.secondaryHeader,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.End,
                maxLines = 2
            )

            if (isLoadingBalance) {
                ShimmeringView(
                    modifier = Modifier
                        .padding(top = RadixTheme.dimensions.paddingXXSmall)
                        .height(12.dp)
                        .fillMaxWidth(0.3f),
                    isVisible = true
                )
            } else if (fiatPrice != null) {
                FiatBalanceView(
                    fiatPrice = fiatPrice,
                    textStyle = RadixTheme.typography.body2HighImportance
                )
            }
        }

        trailingContent?.invoke()
    }
}

@Preview(showBackground = true)
@Composable
fun WorthXRDPreview() {
    RadixWalletPreviewTheme {
        WorthXRD(
            amount = BigDecimal.valueOf(4362.67),
            fiatPrice = null,
            isLoadingBalance = true,
        )
    }
}
