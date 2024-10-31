package com.babylon.wallet.android.presentation.ui.composables.assets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.account.composable.EmptyResourcesContent
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.model.FungibleAmount
import com.babylon.wallet.android.presentation.model.displaySubtitle
import com.babylon.wallet.android.presentation.model.displayTitle
import com.babylon.wallet.android.presentation.model.displayTitleAsPoolUnit
import com.babylon.wallet.android.presentation.transfer.assets.AssetsTab
import com.babylon.wallet.android.presentation.ui.composables.ShimmeringView
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.formatted
import com.radixdlt.sargon.extensions.string
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableMap
import rdx.works.core.domain.assets.AssetPrice
import rdx.works.core.domain.assets.PoolUnit
import rdx.works.core.domain.resources.Resource

fun LazyListScope.poolUnitsTab(
    assetsViewData: AssetsViewData,
    isLoadingBalance: Boolean,
    action: AssetsViewAction,
    onInfoClick: (GlossaryItem) -> Unit
) {
    if (assetsViewData.isPoolUnitsEmpty) {
        item {
            EmptyResourcesContent(
                modifier = Modifier.fillMaxWidth(),
                tab = AssetsTab.PoolUnits,
                onInfoClick = onInfoClick
            )
        }
    }

    itemsIndexed(
        items = assetsViewData.poolUnits,
        key = { index, poolUnitItem -> poolUnitItem.resourceAddress.string }
    ) { index, poolUnitItem ->
        PoolUnitItem(
            modifier = Modifier
                .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                .padding(top = if (index == 0) RadixTheme.dimensions.paddingSemiLarge else RadixTheme.dimensions.paddingDefault),
            poolUnit = poolUnitItem,
            poolUnitPrice = assetsViewData.prices?.get(poolUnitItem) as? AssetPrice.PoolUnitPrice,
            isLoadingBalance = isLoadingBalance,
            action = action
        )
    }
}

@Composable
private fun PoolUnitItem(
    modifier: Modifier = Modifier,
    poolUnit: PoolUnit,
    poolUnitPrice: AssetPrice.PoolUnitPrice?,
    isLoadingBalance: Boolean,
    action: AssetsViewAction,
) {
    AssetCard(
        modifier = modifier
            .throttleClickable {
                when (action) {
                    is AssetsViewAction.Click -> {
                        action.onPoolUnitClick(poolUnit)
                    }

                    is AssetsViewAction.Selection -> {
                        action.onFungibleCheckChanged(poolUnit.stake, !action.isSelected(poolUnit.resourceAddress))
                    }
                }
            }
    ) {
        Row(
            modifier = Modifier.padding(RadixTheme.dimensions.paddingLarge),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
        ) {
            Thumbnail.PoolUnit(
                modifier = Modifier.size(44.dp),
                poolUnit = poolUnit
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = poolUnit.displayTitle(),
                    style = RadixTheme.typography.secondaryHeader,
                    color = RadixTheme.colors.gray1,
                    maxLines = 2
                )

                Text(
                    text = poolUnit.displaySubtitle(),
                    style = RadixTheme.typography.body2HighImportance,
                    color = RadixTheme.colors.gray2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (action is AssetsViewAction.Selection) {
                val isSelected = remember(poolUnit.stake, action) {
                    action.isSelected(poolUnit.resourceAddress)
                }
                AssetsViewCheckBox(
                    isSelected = isSelected,
                    onCheckChanged = { isChecked ->
                        action.onFungibleCheckChanged(poolUnit.stake, isChecked)
                    }
                )
            }
        }

        Text(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingLarge),
            text = stringResource(id = R.string.account_staking_worth),
            style = RadixTheme.typography.body2HighImportance,
            color = RadixTheme.colors.gray2
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))

        val resourcesWithAmounts = remember(poolUnit) {
            poolUnit.pool?.resources?.associateWith {
                poolUnit.resourceRedemptionValue(it)?.let {
                    FungibleAmount.Exact(it)
                }
            }.orEmpty().toImmutableMap()
        }
        PoolResourcesValues(
            modifier = Modifier
                .padding(horizontal = RadixTheme.dimensions.paddingLarge)
                .padding(bottom = RadixTheme.dimensions.paddingLarge),
            resources = resourcesWithAmounts,
            poolUnitPrice = poolUnitPrice,
            isLoadingBalance = isLoadingBalance
        )
    }
}

@Composable
fun PoolResourcesValues(
    modifier: Modifier = Modifier,
    resources: ImmutableMap<Resource.FungibleResource, FungibleAmount?>,
    poolUnitPrice: AssetPrice.PoolUnitPrice?,
    isLoadingBalance: Boolean,
    isCompact: Boolean = true
) {
    Column(modifier = modifier.assetOutlineBorder()) {
        val itemsSize = resources.size
        resources.entries.forEachIndexed { index, resourceWithAmount ->
            Row(
                modifier = Modifier.padding(
                    horizontal = RadixTheme.dimensions.paddingDefault,
                    vertical = if (isCompact) RadixTheme.dimensions.paddingMedium else RadixTheme.dimensions.paddingLarge
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
            ) {
                Thumbnail.Fungible(
                    modifier = Modifier.size(if (isCompact) 24.dp else 44.dp),
                    token = resourceWithAmount.key
                )
                Text(
                    modifier = Modifier.weight(1f),
                    text = resourceWithAmount.key.displayTitleAsPoolUnit(),
                    style = RadixTheme.typography.body2HighImportance,
                    color = RadixTheme.colors.gray1,
                    maxLines = 2
                )

                Column(horizontalAlignment = Alignment.End) {
                    when (val amount = resourceWithAmount.value) {
                        is FungibleAmount.Exact -> Text(
                            text = amount.amount.formatted(),
                            style = if (isCompact) RadixTheme.typography.body1HighImportance else RadixTheme.typography.secondaryHeader,
                            color = RadixTheme.colors.gray1,
                            maxLines = 1
                        )
                        else -> TODO("Sergiu: update UI to represent all the types of FungibleAmount")
                    }

                    val fiatPrice = remember(poolUnitPrice, resourceWithAmount) {
                        poolUnitPrice?.xrdPrice(resourceWithAmount.key)
                    }

                    if (isLoadingBalance) {
                        ShimmeringView(
                            modifier = Modifier
                                .padding(top = RadixTheme.dimensions.paddingXXSmall)
                                .height(12.dp)
                                .fillMaxWidth(0.3f),
                            isVisible = true
                        )
                    } else if (fiatPrice != null) {
                        FiatBalanceView(fiatPrice = fiatPrice)
                    }
                }
            }
            if (index != itemsSize - 1) {
                HorizontalDivider(color = RadixTheme.colors.gray4)
            }
        }
    }
}

@Preview(showBackground = true)
@UsesSampleValues
@Composable
fun PoolUnitTabPreview() {
    RadixWalletTheme {
        LazyColumn {
            poolUnitsTab(
                assetsViewData = previewAssetViewData,
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
