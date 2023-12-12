package com.babylon.wallet.android.presentation.ui.composables.assets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.assets.Assets
import com.babylon.wallet.android.domain.model.assets.PoolUnit
import com.babylon.wallet.android.presentation.account.composable.EmptyResourcesContent
import com.babylon.wallet.android.presentation.transfer.assets.ResourceTab
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import rdx.works.core.displayableQuantity

fun LazyListScope.poolUnitsTab(
    assets: Assets,
    action: AssetsViewAction
) {
    if (assets.ownedPoolUnits.isEmpty()) {
        item {
            EmptyResourcesContent(
                modifier = Modifier.fillMaxWidth(),
                tab = ResourceTab.PoolUnits
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
                resource = item,
                action = action
            )
        }
    }
}

@Composable
private fun PoolUnitItem(
    modifier: Modifier = Modifier,
    resource: PoolUnit,
    action: AssetsViewAction
) {
    AssetCard(
        modifier = modifier
            .throttleClickable {
                when (action) {
                    is AssetsViewAction.Click -> {
                        action.onPoolUnitClick(resource)
                    }
                    is AssetsViewAction.Selection -> {
                        action.onFungibleCheckChanged(resource.stake, !action.isSelected(resource.resourceAddress))
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
                poolUnit = resource
            )
            Text(
                modifier = Modifier.weight(1f),
                text = poolName(resource.stake.displayTitle),
                style = RadixTheme.typography.secondaryHeader,
                color = RadixTheme.colors.gray1,
                maxLines = 2
            )

            if (action is AssetsViewAction.Selection) {
                val isSelected = remember(resource.stake, action) {
                    action.isSelected(resource.resourceAddress)
                }
                AssetsViewCheckBox(
                    isSelected = isSelected,
                    onCheckChanged = { isChecked ->
                        action.onFungibleCheckChanged(resource.stake, isChecked)
                    }
                )
            }
        }

        PoolResourcesValues(
            modifier = Modifier
                .padding(horizontal = RadixTheme.dimensions.paddingLarge)
                .padding(bottom = RadixTheme.dimensions.paddingLarge),
            poolUnit = resource
        )
    }
}

@Composable
fun PoolResourcesValues(poolUnit: PoolUnit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.assetOutlineBorder()) {
        val itemsSize = poolUnit.pool?.resources?.size ?: 0
        poolUnit.pool?.resources?.forEachIndexed { index, poolResource ->
            Row(
                modifier = Modifier.padding(
                    horizontal = RadixTheme.dimensions.paddingDefault,
                    vertical = RadixTheme.dimensions.paddingLarge
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
            ) {
                Thumbnail.Fungible(
                    modifier = Modifier.size(44.dp),
                    token = poolResource
                )
                Text(
                    modifier = Modifier.weight(1f),
                    text = poolResource.displayTitle,
                    style = RadixTheme.typography.body2HighImportance,
                    color = RadixTheme.colors.gray1,
                    maxLines = 2
                )
                Text(
                    text = poolUnit.resourceRedemptionValue(poolResource)?.displayableQuantity().orEmpty(),
                    style = RadixTheme.typography.secondaryHeader,
                    color = RadixTheme.colors.gray1,
                    maxLines = 1
                )
            }
            if (index != itemsSize - 1) {
                HorizontalDivider(color = RadixTheme.colors.gray4)
            }
        }
    }
}

@Composable
fun poolName(name: String?): String {
    return name?.ifEmpty {
        stringResource(id = R.string.account_poolUnits_unknownPoolUnitName)
    } ?: stringResource(id = R.string.account_poolUnits_unknownPoolUnitName)
}
