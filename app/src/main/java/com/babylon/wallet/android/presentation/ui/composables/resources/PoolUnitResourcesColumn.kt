package com.babylon.wallet.android.presentation.ui.composables.resources

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.AccountValidatorsWithStakeResources
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.Resources
import com.babylon.wallet.android.domain.model.ValidatorWithStakeResources
import com.babylon.wallet.android.presentation.account.composable.EmptyResourcesContent
import com.babylon.wallet.android.presentation.transfer.assets.ResourceTab
import rdx.works.core.displayableQuantity
import java.math.BigDecimal

@Composable
fun PoolUnitResourcesColumn(
    modifier: Modifier,
    resources: Resources?,
    contentPadding: PaddingValues = PaddingValues(
        start = RadixTheme.dimensions.paddingMedium,
        end = RadixTheme.dimensions.paddingMedium,
        top = RadixTheme.dimensions.paddingLarge,
        bottom = 100.dp
    )
) {
    var collapsedStakeState by remember(resources) {
        mutableStateOf(true)
    }
    LazyColumn(
        contentPadding = contentPadding,
        modifier = modifier,
    ) {
        poolUnitsResources(
            collapsedState = collapsedStakeState,
            accountValidatorsWithStakeResources = resources?.accountValidatorsWithStakeResources,
            poolUnits = resources?.poolUnits.orEmpty()
        ) {
            collapsedStakeState = !collapsedStakeState
        }
    }
}

@Suppress("LongMethod", "CyclomaticComplexMethod")
fun LazyListScope.poolUnitsResources(
    modifier: Modifier = Modifier,
    collapsedState: Boolean,
    accountValidatorsWithStakeResources: AccountValidatorsWithStakeResources?,
    poolUnits: List<Resource.PoolUnitResource>,
    parentSectionClick: () -> Unit
) {
    if (accountValidatorsWithStakeResources == null && poolUnits.isEmpty()) {
        item {
            EmptyResourcesContent(
                modifier = modifier.fillMaxWidth(),
                tab = ResourceTab.PoolUnits
            )
        }
    } else {
        accountValidatorsWithStakeResources?.let { validatorsWithStakeResources ->
            item {
                LiquidStakeUnitResourceHeader(
                    modifier = modifier,
                    collection = validatorsWithStakeResources,
                    collapsed = collapsedState,
                    parentSectionClick = parentSectionClick
                )
            }
            if (!collapsedState) {
                val validatorsSize = validatorsWithStakeResources.validators.size
                validatorsWithStakeResources.validators.forEachIndexed { index, validator ->
                    val lastValidator = validatorsSize - 1 == index
                    item(key = validator.address) {
                        CardWrapper(modifier) {
                            ValidatorDetailsItem(validator)
                        }
                    }
                    if (validator.liquidStakeUnits.isNotEmpty()) {
                        val lastCollection = validator.stakeClaimNft == null
                        item {
                            CardWrapper(modifier = modifier) {
                                StakeSectionTitle(
                                    title = stringResource(id = R.string.assetDetails_poolUnitDetails_liquidStakeUnits)
                                )
                                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
                            }
                        }
                        val last = validator.liquidStakeUnits.last()
                        items(items = validator.liquidStakeUnits, key = { it.fungibleResource.resourceAddress }) { liquidStakeUnit ->
                            val lastItem = liquidStakeUnit == last
                            CardWrapper(
                                modifier = modifier,
                                lastItem = lastCollection && lastItem && lastValidator
                            ) {
                                LiquidStakeUnitItem(liquidStakeUnit, validator.totalXrdStake)
                                ItemSpacer(lastItem)
                                if (lastCollection && !lastValidator) {
                                    Divider(Modifier.fillMaxWidth(), color = RadixTheme.colors.gray4)
                                }
                            }
                        }
                    }
                    if (validator.stakeClaimNft != null) {
                        item {
                            CardWrapper(modifier = modifier) {
                                StakeSectionTitle(
                                    title = stringResource(id = R.string.assetDetails_poolUnitDetails_stakeClaimNfts)
                                )
                                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
                            }
                        }
                        val last = validator.stakeClaimNft.nonFungibleResource.items.last()
                        items(items = validator.stakeClaimNft.nonFungibleResource.items) { stakeClaimNft ->
                            val lastItem = stakeClaimNft == last
                            CardWrapper(
                                modifier = modifier,
                                lastItem = lastItem && lastValidator
                            ) {
                                StakeClaimNftItem(stakeClaimNft)
                                ItemSpacer(lastItem)
                                if (lastItem && !lastValidator) {
                                    Divider(Modifier.fillMaxWidth(), color = RadixTheme.colors.gray4)
                                }
                            }
                        }
                    }
                }
            }
        }
        if (poolUnits.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            }
            items(poolUnits, key = { it.resourceAddress }) { poolUnit ->
                PoolUnitItem(
                    modifier = modifier,
                    resource = poolUnit
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
            }
        }
    }
}

@Composable
private fun ValidatorDetailsItem(validator: ValidatorWithStakeResources, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(RadixTheme.dimensions.paddingLarge),
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        AsyncImage(
            model = validator.url,
            placeholder = painterResource(id = R.drawable.img_placeholder),
            error = painterResource(id = R.drawable.img_placeholder),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(24.dp)
                .clip(RadixTheme.shapes.roundedRectSmall)
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
private fun ItemSpacer(lastItem: Boolean) {
    val spacerHeight = if (lastItem) {
        RadixTheme.dimensions.paddingDefault
    } else {
        RadixTheme.dimensions.paddingSmall
    }
    Spacer(modifier = Modifier.height(spacerHeight))
}

@Composable
private fun CardWrapper(modifier: Modifier = Modifier, lastItem: Boolean = false, content: @Composable () -> Unit) {
    val shadowPadding = 12.dp
    Card(
        modifier = modifier
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
            bottomEnd = if (lastItem) 12.dp else 0.dp,
            bottomStart = if (lastItem) 12.dp else 0.dp
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
        modifier = modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
        text = title,
        style = RadixTheme.typography.body1HighImportance,
        color = RadixTheme.colors.gray2,
        maxLines = 1
    )
}

@Composable
private fun StakeClaimNftItem(stakeClaimNft: Resource.NonFungibleResource.Item, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = RadixTheme.dimensions.paddingDefault)
            .border(
                width = 1.dp,
                color = RadixTheme.colors.gray4,
                shape = RadixTheme.shapes.roundedRectMedium
            )
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
            text = if (stakeClaimNft.readyToClaim) {
                stringResource(id = R.string.assetDetails_poolUnitDetails_readyToClaim)
            } else {
                stringResource(id = R.string.assetDetails_poolUnitDetails_unstaking)
            },
            modifier = Modifier.weight(1f),
            style = RadixTheme.typography.body2HighImportance,
            color = if (stakeClaimNft.readyToClaim) RadixTheme.colors.green1 else RadixTheme.colors.gray1,
            maxLines = 1
        )
        stakeClaimNft.claimAmountXrd?.let {
            Text(
                it.displayableQuantity(),
                style = RadixTheme.typography.secondaryHeader,
                color = RadixTheme.colors.gray1,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun LiquidStakeUnitItem(
    liquidStakeUnit: Resource.LiquidStakeUnitResource,
    totalXrdStake: BigDecimal?,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(horizontal = RadixTheme.dimensions.paddingDefault)
            .border(
                width = 1.dp,
                color = RadixTheme.colors.gray4,
                shape = RadixTheme.shapes.roundedRectMedium
            )
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
                stringResource(id = R.string.assetDetails_poolUnitDetails_xrdSymbol),
                style = RadixTheme.typography.body2HighImportance,
                color = RadixTheme.colors.gray1,
                maxLines = 2
            )
            Text(
                stringResource(id = R.string.assetDetails_poolUnitDetails_staked),
                style = RadixTheme.typography.body2HighImportance,
                color = RadixTheme.colors.gray2,
                maxLines = 1
            )
        }
        Text(
            liquidStakeUnit.percentageOwned?.multiply(totalXrdStake)?.displayableQuantity().orEmpty(),
            style = RadixTheme.typography.secondaryHeader,
            color = RadixTheme.colors.gray1,
            maxLines = 1
        )
    }
}

@Composable
private fun PoolUnitItem(
    resource: Resource.PoolUnitResource,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RadixTheme.shapes.roundedRectMedium,
        colors = CardDefaults.cardColors(
            containerColor = RadixTheme.colors.defaultBackground
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RadixTheme.dimensions.paddingLarge)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
            ) {
                AsyncImage(
                    model = resource.fungibleResource.iconUrl,
                    placeholder = painterResource(id = R.drawable.img_placeholder),
                    error = painterResource(id = R.drawable.img_placeholder),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RadixTheme.shapes.roundedRectSmall)
                )
                Text(
                    poolName(resource),
                    style = RadixTheme.typography.secondaryHeader,
                    color = RadixTheme.colors.gray1,
                    maxLines = 2
                )
            }
        }
    }
}

private fun poolName(poolUnit: Resource.PoolUnitResource): String {
    return poolUnit.fungibleResource.displayTitle.ifEmpty { "Unnamed Pool" }
}
