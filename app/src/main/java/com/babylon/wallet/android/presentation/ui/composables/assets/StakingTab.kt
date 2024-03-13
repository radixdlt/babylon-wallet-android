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
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.assets.AssetPrice
import com.babylon.wallet.android.domain.model.assets.ValidatorWithStakes
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.XrdResource
import com.babylon.wallet.android.presentation.account.composable.EmptyResourcesContent
import com.babylon.wallet.android.presentation.transfer.assets.AssetsTab
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.resources.FiatBalance
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import rdx.works.core.displayableQuantity
import java.math.BigDecimal

fun LazyListScope.stakingTab(
    data: AssetsViewData,
    state: AssetsViewState,
    action: AssetsViewAction
) {
    if (data.validatorsWithStakes.isEmpty()) {
        item {
            EmptyResourcesContent(
                modifier = Modifier.fillMaxWidth(),
                tab = AssetsTab.Staking
            )
        }
    } else {
        item {
            StakingSummary(
                data = data,
                action = action
            )
        }

        item {
            ValidatorsSize(data = data)
        }

        data.validatorsWithStakes.forEachIndexed { index, validatorWithStakes ->
            item(
                key = validatorWithStakes.validatorDetail.address,
                contentType = { "validator-header" }
            ) {
                ValidatorDetails(
                    modifier = Modifier.padding(top = if (index != 0) RadixTheme.dimensions.paddingDefault else 0.dp),
                    validatorWithStakes = validatorWithStakes,
                    state = state,
                    epoch = data.epoch,
                    action = action
                )
            }
        }
    }
}

@Composable
private fun StakingSummary(
    modifier: Modifier = Modifier,
    data: AssetsViewData,
    action: AssetsViewAction
) {
    val summary = data.stakeSummary
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

        LaunchedEffect(summary) {
            if (summary == null) {
                action.onStakesRequest()
            }
        }

        StakeAmount(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingLarge),
            label = stringResource(id = R.string.account_staking_staked),
            amount = summary?.staked,
            amountStyle = RadixTheme.typography.body2HighImportance.copy(
                color = if (data.stakeSummary?.hasStakedValue == true) RadixTheme.colors.gray1 else RadixTheme.colors.gray2
            )
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
        StakeAmount(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingLarge),
            label = stringResource(id = R.string.account_staking_unstaking),
            amount = summary?.unstaking
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
        StakeAmount(
            modifier = Modifier
                .padding(horizontal = RadixTheme.dimensions.paddingLarge)
                .clickable(enabled = summary?.hasReadyToClaimValue == true && action is AssetsViewAction.Click) {
                    if (action is AssetsViewAction.Click) {
                        val claims = data.validatorsWithStakes
                            .filter { it.hasClaims }
                            .mapNotNull { it.stakeClaimNft }

                        action.onClaimClick(claims)
                    }
                },
            label = stringResource(id = R.string.account_staking_readyToClaim),
            amount = summary?.readyToClaim,
            labelStyle = if (summary?.hasReadyToClaimValue == true) {
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
    amount: BigDecimal?,
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

        Text(
            modifier = Modifier
                .weight(1f)
                .radixPlaceholder(visible = amount == null),
            text = amount?.let { "${it.displayableQuantity()} ${XrdResource.SYMBOL}" }.orEmpty(),
            style = amountStyle,
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun ValidatorsSize(
    modifier: Modifier = Modifier,
    data: AssetsViewData
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
            text = stringResource(id = R.string.account_staking_stakedValidators, data.validatorsWithStakes.size),
            style = RadixTheme.typography.body1HighImportance,
            color = RadixTheme.colors.gray2
        )
    }
}

@Composable
fun ValidatorDetails(
    modifier: Modifier = Modifier,
    validatorWithStakes: ValidatorWithStakes,
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
                        validatorWithStakes = validatorWithStakes,
                        epoch = epoch,
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
    validatorWithStakes: ValidatorWithStakes,
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

        WorthXRD(
            amount = remember(validatorWithStakes) { validatorWithStakes.stakeValue() },
            fiatPriceFormatted = null // TODO change that
        )
    }
}

@Composable
private fun StakeClaims(
    modifier: Modifier = Modifier,
    validatorWithStakes: ValidatorWithStakes,
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

            unstakingItems.forEachIndexed { index, item ->
                ClaimWorth(
                    modifier = Modifier.padding(top = if (index != 0) RadixTheme.dimensions.paddingSmall else 0.dp),
                    claimCollection = validatorWithStakes.stakeClaimNft.nonFungibleResource,
                    claimNft = item,
                    price = null, // TODO change that
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

            claimItems.forEachIndexed { index, item ->
                ClaimWorth(
                    modifier = Modifier.padding(top = if (index != 0) RadixTheme.dimensions.paddingSmall else 0.dp),
                    claimCollection = validatorWithStakes.stakeClaimNft.nonFungibleResource,
                    claimNft = item,
                    price = null, // TODO change that
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
    price: AssetPrice.StakeClaimPrice?,
    action: AssetsViewAction
) {
    val fiatPriceFormatted = remember(price, claimNft) {
        price?.xrdPriceFormatted(claimNft)
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
        fiatPriceFormatted = fiatPriceFormatted,
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
        }
    )
}

@Composable
fun WorthXRD(
    modifier: Modifier = Modifier,
    amount: BigDecimal?,
    fiatPriceFormatted: String?,
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

            if (fiatPriceFormatted != null) {
                FiatBalance(
                    fiatPriceFormatted = fiatPriceFormatted,
                    style = RadixTheme.typography.body2HighImportance
                )
            }
        }

        trailingContent?.invoke()
    }
}
