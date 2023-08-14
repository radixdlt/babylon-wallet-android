package com.babylon.wallet.android.presentation.ui.composables.resources

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.Resources
import com.babylon.wallet.android.domain.model.ValidatorDetail
import com.babylon.wallet.android.domain.model.ValidatorsWithStakeResources
import com.babylon.wallet.android.presentation.account.composable.EmptyResourcesContent
import com.babylon.wallet.android.presentation.transfer.assets.ResourceTab

@Composable
fun PoolUnitResourcesColumn(
    modifier: Modifier,
    resources: Resources?,
    contentPadding: PaddingValues = PaddingValues(
        start = RadixTheme.dimensions.paddingMedium,
        end = RadixTheme.dimensions.paddingMedium,
        top = RadixTheme.dimensions.paddingLarge,
        bottom = 100.dp
    ),
    poolUnitItem: @Composable (Resource.PoolUnitResource) -> Unit,
    liquidStakeItem: @Composable (Resource.LiquidStakeUnitResource, ValidatorDetail) -> Unit,
    stakeClaimItem: @Composable (Resource.StakeClaimResource, Resource.NonFungibleResource.Item) -> Unit
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
            validatorsWithStakeResources = resources?.validatorsWithStakeResources,
            poolUnits = resources?.poolUnits.orEmpty(),
            parentSectionClick = {
                collapsedStakeState = !collapsedStakeState
            },
            poolUnitItem = poolUnitItem,
            liquidStakeItem = liquidStakeItem,
            stakeClaimItem = stakeClaimItem
        )
    }
}

@Suppress("LongMethod", "CyclomaticComplexMethod", "LongParameterList")
fun LazyListScope.poolUnitsResources(
    modifier: Modifier = Modifier,
    collapsedState: Boolean,
    validatorsWithStakeResources: ValidatorsWithStakeResources?,
    poolUnits: List<Resource.PoolUnitResource>,
    parentSectionClick: () -> Unit,
    poolUnitItem: @Composable (Resource.PoolUnitResource) -> Unit,
    liquidStakeItem: @Composable (Resource.LiquidStakeUnitResource, ValidatorDetail) -> Unit,
    stakeClaimItem: @Composable (Resource.StakeClaimResource, Resource.NonFungibleResource.Item) -> Unit
) {
    if ((validatorsWithStakeResources == null || validatorsWithStakeResources.isEmpty) && poolUnits.isEmpty()) {
        item {
            EmptyResourcesContent(
                modifier = modifier.fillMaxWidth(),
                tab = ResourceTab.PoolUnits
            )
        }
    } else {
        validatorsWithStakeResources?.let { validatorWithStakeResources ->
            item {
                LiquidStakeUnitResourceHeader(
                    modifier = modifier,
                    collection = validatorWithStakeResources,
                    collapsed = collapsedState,
                    parentSectionClick = parentSectionClick
                )
                if (!collapsedState) {
                    Divider(modifier.fillMaxWidth(), color = RadixTheme.colors.gray4)
                }
            }
            if (!collapsedState) {
                val validatorsSize = validatorWithStakeResources.validators.size
                validatorWithStakeResources.validators.forEachIndexed { index, validator ->
                    val lastValidator = validatorsSize - 1 == index
                    item(key = validator.validatorDetail.address) {
                        CardWrapper(modifier) {
                            ValidatorDetailsItem(
                                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                                validator = validator.validatorDetail
                            )
                        }
                    }
                    if (validator.liquidStakeUnits.isNotEmpty()) {
                        val lastCollection = validator.stakeClaimNft == null
                        item {
                            CardWrapper(modifier = modifier) {
                                StakeSectionTitle(
                                    title = stringResource(id = R.string.account_poolUnits_liquidStakeUnits)
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
                                liquidStakeItem(liquidStakeUnit, validator.validatorDetail)
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
                                    title = stringResource(id = R.string.account_poolUnits_stakeClaimNFTs)
                                )
                                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
                            }
                        }
                        val last = validator.stakeClaimNft.nonFungibleResource.items.last()
                        items(items = validator.stakeClaimNft.nonFungibleResource.items) { item ->
                            val lastItem = item == last
                            CardWrapper(
                                modifier = modifier,
                                lastItem = lastItem && lastValidator
                            ) {
                                stakeClaimItem(validator.stakeClaimNft, item)
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
                poolUnitItem(poolUnit)
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            }
        }
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

fun poolName(name: String?): String {
    return name?.ifEmpty { "Unnamed Pool" } ?: "Unnamed Pool"
}
