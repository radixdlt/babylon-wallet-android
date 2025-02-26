@file:Suppress("TooManyFunctions")

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
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.account.composable.EmptyResourcesContent
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.model.displayTitle
import com.babylon.wallet.android.presentation.transfer.assets.AssetsTab
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.ShimmeringView
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.card.CollapsibleCommonCard
import com.babylon.wallet.android.presentation.ui.composables.card.CommonCard
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.compareTo
import com.radixdlt.sargon.extensions.formatted
import com.radixdlt.sargon.extensions.orZero
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.extensions.times
import com.radixdlt.sargon.extensions.toDecimal192
import rdx.works.core.domain.assets.AssetPrice
import rdx.works.core.domain.assets.FiatPrice
import rdx.works.core.domain.assets.ValidatorWithStakes
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.XrdResource

fun LazyListScope.stakingTab(
    assetsViewData: AssetsViewData,
    isLoadingBalance: Boolean,
    state: AssetsViewState,
    action: AssetsViewAction,
    onInfoClick: (GlossaryItem) -> Unit
) {
    if (assetsViewData.isValidatorWithStakesEmpty) {
        item {
            EmptyResourcesContent(
                modifier = Modifier.fillMaxWidth(),
                tab = AssetsTab.Staking,
                onInfoClick = onInfoClick
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
                key = validatorWithStakes.validator.address.string,
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
    action: AssetsViewAction,
) {
    val stakeSummary = assetsViewData.stakeSummary
    CommonCard(
        modifier = modifier
            .padding(horizontal = RadixTheme.dimensions.paddingDefault)
            .padding(top = RadixTheme.dimensions.paddingSemiLarge),
        itemIndex = 0,
        allItemsSize = 1
    ) {
        Row(
            modifier = Modifier
                .padding(
                    horizontal = RadixTheme.dimensions.paddingLarge,
                    vertical = RadixTheme.dimensions.paddingSemiLarge
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
        ) {
            Image(
                modifier = Modifier.size(56.dp),
                painter = painterResource(id = DSR.ic_lsu),
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
            assetsViewData.oneXrdPrice
        }

        val stakedFiatPrice = remember(stakeSummary, oneXrdPrice) {
            if (stakeSummary != null && oneXrdPrice != null && stakeSummary.hasStakedValue) {
                FiatPrice(
                    price = stakeSummary.staked.times(oneXrdPrice.price),
                    currency = oneXrdPrice.currency
                )
            } else {
                null
            }
        }

        val unstakingFiatPrice = remember(stakeSummary, oneXrdPrice) {
            if (stakeSummary != null && oneXrdPrice != null && stakeSummary.unstaking > 0.toDecimal192()) {
                FiatPrice(
                    price = stakeSummary.unstaking.times(oneXrdPrice.price),
                    currency = oneXrdPrice.currency
                )
            } else {
                null
            }
        }

        val readyToClaimFiatPrice = remember(stakeSummary, oneXrdPrice) {
            if (stakeSummary != null && oneXrdPrice != null && stakeSummary.hasReadyToClaimValue) {
                FiatPrice(
                    price = stakeSummary.readyToClaim.times(oneXrdPrice.price),
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
    amount: Decimal192?,
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
                text = amount?.let { "${it.formatted()} ${XrdResource.SYMBOL}" }.orEmpty(),
                style = amountStyle,
                textAlign = TextAlign.End
            )

            if (isLoadingBalance) {
                ShimmeringView(
                    modifier = Modifier
                        .padding(top = RadixTheme.dimensions.paddingXXXSmall)
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
                .padding(RadixTheme.dimensions.paddingXXSmall),
            painter = painterResource(id = DSR.ic_validator),
            tint = RadixTheme.colors.gray2,
            contentDescription = null
        )

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.account_staking_stakedValidators, assetsViewData.validatorsWithStakes.size),
            style = RadixTheme.typography.body1Link,
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
            val stake = validatorWithStakes.liquidStakeUnit?.fungibleResource?.ownedAmount.orZero()
            val claims = validatorWithStakes.stakeClaimNft?.nonFungibleResource?.amount ?: 0L
            if (stake > 0.toDecimal192()) {
                cards += 1
            }
            if (claims > 0) {
                cards += 1
            }
            cards
        }
        val isCollapsed = state.isCollapsed(validatorWithStakes.validator.address.string)
        CollapsibleCommonCard(
            modifier = modifier
                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
            isCollapsed = isCollapsed,
            collapsedItems = collapsedCardsCount
        ) {
            ValidatorHeader(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        action.onCollectionClick(validatorWithStakes.validator.address.string)
                    }
                    .padding(RadixTheme.dimensions.paddingLarge),
                validatorWithStakes = validatorWithStakes
            )
        }

        if (!isCollapsed) {
            if (validatorWithStakes.hasLSU) {
                CommonCard(
                    modifier = Modifier
                        .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                        .padding(top = RadixTheme.dimensions.paddingXXXSmall),
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
                CommonCard(
                    modifier = Modifier
                        .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                        .padding(top = RadixTheme.dimensions.paddingXXXSmall),
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
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
        ) {
            Thumbnail.LSU(
                modifier = Modifier.size(36.dp),
                liquidStakeUnit = validatorWithStakes.liquidStakeUnit
            )

            Text(
                modifier = Modifier.weight(1f),
                text = validatorWithStakes.liquidStakeUnit?.displayTitle().orEmpty(),
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.gray1
            )

            val lsu = validatorWithStakes.liquidStakeUnit
            if (action is AssetsViewAction.Selection && lsu != null) {
                val isSelected = remember(lsu.resourceAddress, action) {
                    action.isSelected(lsu.resourceAddress)
                }
                AssetsViewCheckBox(
                    isSelected = isSelected,
                    onCheckChanged = { isChecked ->
                        action.onFungibleCheckChanged(lsu.fungibleResource, isChecked)
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

        val amount = remember(validatorWithStakes) { validatorWithStakes.stakeValue() }
        WorthXRD(
            amount = amount,
            isLoadingAmount = amount == null,
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
                text = validatorWithStakes.stakeClaimNft?.displayTitle().orEmpty(),
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

        val claim = validatorWithStakes.stakeClaimNft
        if (unstakingItems.isNotEmpty() && claim != null) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = RadixTheme.dimensions.paddingDefault,
                        bottom = RadixTheme.dimensions.paddingSmall
                    ),
                text = stringResource(id = R.string.account_staking_unstaking).uppercase(),
                style = RadixTheme.typography.body2HighImportance,
                color = RadixTheme.colors.gray2
            )

            val stakeClaimPrice = assetsViewData.prices?.get(claim) as? AssetPrice.StakeClaimPrice
            unstakingItems.forEachIndexed { index, item ->
                ClaimWorth(
                    modifier = Modifier.padding(top = if (index != 0) RadixTheme.dimensions.paddingDefault else 0.dp),
                    claimCollection = claim.nonFungibleResource,
                    claimNft = item,
                    stakeClaimPrice = stakeClaimPrice,
                    isLoadingBalance = isLoadingBalance,
                    action = action
                )
            }
        }
        val unstakingItemsEmpty = remember(unstakingItems) {
            unstakingItems.isEmpty()
        }
        if (claimItems.isNotEmpty() && claim != null) {
            Row(
                modifier = Modifier.padding(
                    top = if (action is AssetsViewAction.Selection || unstakingItemsEmpty) {
                        RadixTheme.dimensions.paddingDefault
                    } else {
                        RadixTheme.dimensions.paddingSmall
                    },
                    bottom = RadixTheme.dimensions.paddingSmall
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
                    Text(
                        modifier = Modifier.clickable {
                            action.onClaimClick(listOf(claim))
                        },
                        text = stringResource(id = R.string.account_staking_claim),
                        style = RadixTheme.typography.body2Link,
                        color = RadixTheme.colors.blue1
                    )
                }
            }

            val stakeClaimPrice = assetsViewData.prices?.get(claim) as? AssetPrice.StakeClaimPrice
            claimItems.forEachIndexed { index, item ->
                ClaimWorth(
                    modifier = Modifier.padding(top = if (index != 0) RadixTheme.dimensions.paddingXXSmall else 0.dp),
                    claimCollection = claim.nonFungibleResource,
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
            validator = validatorWithStakes.validator
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
        ) {
            Text(
                text = validatorWithStakes.validator.name,
                style = RadixTheme.typography.secondaryHeader,
                color = RadixTheme.colors.gray1,
                maxLines = 1
            )

            val stakedAmount = remember(validatorWithStakes) {
                if (validatorWithStakes.liquidStakeUnit != null) {
                    validatorWithStakes.stakeValue()
                } else {
                    0.toDecimal192()
                }
            }

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .radixPlaceholder(visible = stakedAmount == null),
                text = stakedAmount?.let {
                    "${stringResource(id = R.string.account_staking_currentStake, it.formatted())} ${XrdResource.SYMBOL}"
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
    val amount = remember(claimNft) { claimNft.claimAmountXrd }
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
                        !action.isSelected(claimNft.globalId)
                    )
                }
            }
        },
        amount = amount,
        isLoadingAmount = amount == null,
        fiatPrice = fiatPrice,
        trailingContent = if (action is AssetsViewAction.Selection) {
            {
                val isSelected = remember(claimNft, action) {
                    action.isSelected(claimNft.globalId)
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
    isLoadingAmount: Boolean,
    amount: Decimal192?,
    fiatPrice: FiatPrice?,
    isLoadingBalance: Boolean,
    iconSize: Dp = 24.dp,
    symbolStyle: TextStyle = RadixTheme.typography.body2HighImportance,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Column(
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
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(id = DSR.ic_xrd_token),
                contentDescription = null,
                modifier = Modifier
                    .size(iconSize)
                    .clip(RadixTheme.shapes.circle),
                tint = Color.Unspecified
            )

            Text(
                modifier = Modifier.padding(
                    horizontal = RadixTheme.dimensions.paddingSmall
                ),
                text = XrdResource.SYMBOL,
                style = symbolStyle,
                color = RadixTheme.colors.gray1,
                maxLines = 1
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        end = if (trailingContent == null || amount == null) RadixTheme.dimensions.paddingDefault else 0.dp
                    )
                    .assetPlaceholder(visible = isLoadingAmount),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = amount?.formatted().orEmpty(),
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.gray1,
                    textAlign = TextAlign.End,
                    maxLines = 2
                )

                if (isLoadingBalance) {
                    ShimmeringView(
                        modifier = Modifier
                            .padding(top = RadixTheme.dimensions.paddingXXXSmall)
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

        if (amount == null && !isLoadingAmount) {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))

            Text(
                text = stringResource(id = R.string.interactionReview_unknown_amount),
                style = RadixTheme.typography.body2HighImportance,
                color = RadixTheme.colors.gray2
            )
        }
    }
}

@Preview(showBackground = true)
@UsesSampleValues
@Composable
fun StakingTabPreview() {
    RadixWalletTheme {
        LazyColumn {
            stakingTab(
                assetsViewData = previewAssetViewData,
                state = AssetsViewState(AssetsTab.Staking, emptyMap(), emptySet()),
                isLoadingBalance = false,
                action = AssetsViewAction.Click(
                    onTabClick = {},
                    onStakesRequest = {},
                    onCollectionClick = {},
                    onLSUClick = {},
                    onClaimClick = {},
                    onNextNFtsPageRequest = {},
                    onFungibleClick = {},
                    onPoolUnitClick = {},
                    onNonFungibleItemClick = { _, _ -> }
                ),
                onInfoClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WorthXRDPreview() {
    RadixWalletPreviewTheme {
        WorthXRD(
            amount = 4362.67.toDecimal192(),
            isLoadingAmount = false,
            fiatPrice = null,
            isLoadingBalance = true,
        )
    }
}
