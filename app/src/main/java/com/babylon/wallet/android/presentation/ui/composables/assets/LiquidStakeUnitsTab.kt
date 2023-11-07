@file:OptIn(ExperimentalMaterial3Api::class)

package com.babylon.wallet.android.presentation.ui.composables.assets

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.assets.Assets
import com.babylon.wallet.android.domain.model.assets.LiquidStakeUnit
import com.babylon.wallet.android.domain.model.assets.ValidatorDetail
import com.babylon.wallet.android.domain.model.assets.ValidatorWithStakes
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.XrdResource
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer
import rdx.works.core.displayableQuantity
import java.math.BigDecimal

fun LazyListScope.liquidStakeUnitsTab(
    assets: Assets,
    stakeUnitCollapsedState: MutableState<Boolean>,
    onLSUClick: (LiquidStakeUnit, ValidatorDetail) -> Unit,
    onNonFungibleClick: (Resource.NonFungibleResource, Resource.NonFungibleResource.Item) -> Unit
) {
    if (assets.validatorsWithStakes.isNotEmpty()) {
        item {
            LiquidStakeUnitHeader(
                modifier = Modifier
                    .padding(bottom = if (stakeUnitCollapsedState.value) RadixTheme.dimensions.paddingDefault else 1.dp)
                    .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                collection = assets.validatorsWithStakes,
                collapsed = stakeUnitCollapsedState.value,
                parentSectionClick = {
                    stakeUnitCollapsedState.value = !stakeUnitCollapsedState.value
                }
            )
        }

        if (!stakeUnitCollapsedState.value) {
            itemsIndexed(
                items = assets.validatorsWithStakes,
                key = { _, item -> item.validatorDetail.address }
            ) { index, item ->
                ValidatorCard(
                    modifier = Modifier
                        .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                    isLast = index == assets.validatorsWithStakes.lastIndex
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

                    val stakeValue = remember(item) {
                        item.liquidStakeUnit.stakeValueInXRD(item.validatorDetail.totalXrdStake)
                    }
                    LiquidStakeUnitItem(
                        modifier = Modifier
                            .throttleClickable {
                                onLSUClick(item.liquidStakeUnit, item.validatorDetail)
                            }
                            .padding(horizontal = RadixTheme.dimensions.paddingLarge)
                            .padding(bottom = RadixTheme.dimensions.paddingDefault),
                        stakeValueInXRD = stakeValue,
                        trailingContent = {}
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
                                    .throttleClickable(enabled = stakeClaimNFT != null) {
                                        if (stakeClaimNFT != null) {
                                            onNonFungibleClick(item.stakeClaimNft.nonFungibleResource, stakeClaimNFT)
                                        }
                                    }
                                    .padding(horizontal = RadixTheme.dimensions.paddingLarge)
                                    .padding(
                                        top = if (index > 0) RadixTheme.dimensions.paddingSmall else 0.dp,
                                        bottom = if (isLast) RadixTheme.dimensions.paddingDefault else 0.dp
                                    ),
                                stakeClaimNft = stakeClaimNFT
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiquidStakeUnitHeader(
    modifier: Modifier = Modifier,
    collection: List<ValidatorWithStakes>,
    cardHeight: Dp = 103.dp,
    collapsed: Boolean = true,
    groupInnerPadding: Dp = 6.dp,
    parentSectionClick: () -> Unit,
) {
    val bottomCorners = if (collapsed) 12.dp else 0.dp
    val cardShape = RoundedCornerShape(12.dp, 12.dp, bottomCorners, bottomCorners)
    BoxWithConstraints(
        modifier = modifier
    ) {
        if (collapsed) {
            if (collection.isNotEmpty()) {
                val scaleFactor = 0.9f
                val topOffset = cardHeight * (1 - scaleFactor) + groupInnerPadding
                Surface(
                    modifier = Modifier
                        .padding(top = topOffset)
                        .fillMaxWidth()
                        .height(cardHeight)
                        .scale(scaleFactor, scaleFactor),
                    shape = RadixTheme.shapes.roundedRectMedium,
                    color = Color.White,
                    elevation = 3.dp,
                    content = {}
                )
            }
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(cardHeight)
                .clickable { parentSectionClick() },
            shape = cardShape,
            colors = CardDefaults.cardColors(
                containerColor = RadixTheme.colors.defaultBackground
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            ),
            onClick = parentSectionClick
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = RadixTheme.dimensions.paddingLarge),
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
                        stringResource(id = R.string.account_poolUnits_numberOfStakes, collection.size),
                        style = RadixTheme.typography.body2HighImportance,
                        color = RadixTheme.colors.gray2,
                        maxLines = 1
                    )
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
private fun ValidatorCard(
    modifier: Modifier = Modifier,
    isLast: Boolean,
    content: @Composable () -> Unit
) {
    val shadowPadding = RadixTheme.dimensions.paddingDefault
    val bottomCorners by animateDpAsState(
        targetValue = if (isLast) 12.dp else 0.dp,
        label = "bottomCorners"
    )

    Card(
        modifier = modifier
            .padding(bottom = if (isLast) RadixTheme.dimensions.paddingDefault else 1.dp)
            .drawWithContent {
                val shadowPaddingPx = shadowPadding.toPx()
                clipRect(
                    top = 0f,
                    left = -shadowPaddingPx,
                    right = size.width + shadowPaddingPx,
                    bottom = size.height + shadowPaddingPx
                ) {
                    this@drawWithContent.drawContent()
                }
            }
            .fillMaxWidth(),
        shape = RoundedCornerShape(
            topStart = 0.dp,
            topEnd = 0.dp,
            bottomEnd = bottomCorners,
            bottomStart = bottomCorners
        ),
        colors = CardDefaults.cardColors(
            containerColor = RadixTheme.colors.defaultBackground
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        content()
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
    stakeValueInXRD: BigDecimal?,
    modifier: Modifier = Modifier,
    trailingContent: @Composable () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
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
        Text(
            modifier = Modifier
                .sizeIn(minWidth = 96.dp)
                .assetPlaceholder(visible = stakeValueInXRD == null),
            text = stakeValueInXRD?.displayableQuantity().orEmpty(),
            style = RadixTheme.typography.secondaryHeader,
            color = RadixTheme.colors.gray1,
            maxLines = 1
        )

        trailingContent()
    }
}

@Composable
private fun StakeClaimNftItem(
    stakeClaimNft: Resource.NonFungibleResource.Item?,
    modifier: Modifier = Modifier,
    trailingContent: @Composable () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .assetOutlineBorder()
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

        trailingContent()
    }
}
