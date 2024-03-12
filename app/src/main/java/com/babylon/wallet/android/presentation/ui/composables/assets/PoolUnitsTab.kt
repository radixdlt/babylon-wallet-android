package com.babylon.wallet.android.presentation.ui.composables.assets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.assets.AssetPrice
import com.babylon.wallet.android.domain.model.assets.Assets
import com.babylon.wallet.android.domain.model.assets.PoolUnit
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.presentation.account.composable.EmptyResourcesContent
import com.babylon.wallet.android.presentation.transfer.assets.AssetsTab
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.resources.FiatBalance
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableMap
import rdx.works.core.displayableQuantity
import java.math.BigDecimal

fun LazyListScope.poolUnitsTab(
    assets: Assets,
    action: AssetsViewAction
) {
    if (assets.ownedPoolUnits.isEmpty()) {
        item {
            EmptyResourcesContent(
                modifier = Modifier.fillMaxWidth(),
                tab = AssetsTab.PoolUnits
            )
        }
    }

    if (assets.ownedPoolUnits.isNotEmpty()) {
        items(
            items = assets.ownedPoolUnits,
            key = { item -> item.resourceAddress }
        ) { item ->
            PoolUnitItem(
                modifier = Modifier
                    .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                    .padding(top = RadixTheme.dimensions.paddingSemiLarge),
                poolUnit = item,
                action = action
            )
        }
    }
}

@Composable
private fun PoolUnitItem(
    modifier: Modifier = Modifier,
    poolUnit: PoolUnit,
    action: AssetsViewAction
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
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
        ) {
            Thumbnail.PoolUnit(
                modifier = Modifier.size(44.dp),
                poolUnit = poolUnit
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = poolUnit.name(),
                    style = RadixTheme.typography.secondaryHeader,
                    color = RadixTheme.colors.gray1,
                    maxLines = 2
                )

                val associatedDAppName = remember(poolUnit) {
                    poolUnit.pool?.associatedDApp?.name
                }
                if (!associatedDAppName.isNullOrEmpty()) {
                    Text(
                        text = associatedDAppName,
                        style = RadixTheme.typography.body2HighImportance,
                        color = RadixTheme.colors.gray2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
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
                poolUnit.resourceRedemptionValue(it)
            }.orEmpty().toImmutableMap()
        }
        PoolResourcesValues(
            modifier = Modifier
                .padding(horizontal = RadixTheme.dimensions.paddingLarge)
                .padding(bottom = RadixTheme.dimensions.paddingLarge),
            resources = resourcesWithAmounts,
            fiatPrice = null // TODO change that
        )
    }
}

@Composable
fun PoolResourcesValues(
    modifier: Modifier = Modifier,
    resources: ImmutableMap<Resource.FungibleResource, BigDecimal?>,
    fiatPrice: AssetPrice.PoolUnitPrice?,
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
                    text = resourceWithAmount.key.displayTitle,
                    style = RadixTheme.typography.body2HighImportance,
                    color = RadixTheme.colors.gray1,
                    maxLines = 2
                )

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = resourceWithAmount.value?.displayableQuantity().orEmpty(),
                        style = if (isCompact) RadixTheme.typography.body1HighImportance else RadixTheme.typography.secondaryHeader,
                        color = RadixTheme.colors.gray1,
                        maxLines = 1
                    )

                    val fiatPriceFormatted = remember(fiatPrice, resourceWithAmount) {
                        fiatPrice?.priceFormatted(resourceWithAmount.key)
                    }

                    if (fiatPriceFormatted != null) {
                        FiatBalance(
                            fiatPriceFormatted = fiatPriceFormatted,
                            style = RadixTheme.typography.body2HighImportance
                        )
                    }
                }

            }
            if (index != itemsSize - 1) {
                HorizontalDivider(color = RadixTheme.colors.gray4)
            }
        }
    }
}

// Pool units just display the name and have no fallback
@Composable
fun PoolUnit.name() = displayTitle
