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
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.assets.Assets
import com.babylon.wallet.android.domain.model.assets.PoolUnit
import com.babylon.wallet.android.presentation.account.composable.EmptyResourcesContent
import com.babylon.wallet.android.presentation.transfer.assets.ResourceTab
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.resources.poolName
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import rdx.works.core.displayableQuantity

fun LazyListScope.poolUnitsTab(
    assets: Assets,
    epoch: Long?,
    collapsibleAssetsState: SnapshotStateMap<String, Boolean>,
    action: AssetsViewAction
) {
    if (assets.validatorsWithStakes.isEmpty() && assets.poolUnits.isEmpty()) {
        item {
            EmptyResourcesContent(
                modifier = Modifier.fillMaxWidth(),
                tab = ResourceTab.PoolUnits
            )
        }
    }

    liquidStakeUnitsTab(
        assets = assets,
        epoch = epoch,
        collapsibleAssetsState = collapsibleAssetsState,
        action = action
    )

    if (assets.poolUnits.isNotEmpty()) {
        items(
            items = assets.poolUnits,
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
                poolName(resource.stake.displayTitle),
                style = RadixTheme.typography.secondaryHeader,
                color = RadixTheme.colors.gray1,
                maxLines = 2
            )
        }
        Row(
            modifier = Modifier
                .padding(horizontal = RadixTheme.dimensions.paddingLarge)
                .padding(bottom = RadixTheme.dimensions.paddingLarge),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
        ) {
            PoolResourcesValues(modifier = Modifier.weight(1f), poolUnit = resource)

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
    }
}

@Composable
private fun PoolResourcesValues(poolUnit: PoolUnit, modifier: Modifier = Modifier) {
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
                    poolUnit.resourceRedemptionValue(poolResource)?.displayableQuantity().orEmpty(),
                    style = RadixTheme.typography.secondaryHeader,
                    color = RadixTheme.colors.gray1,
                    maxLines = 1
                )
            }
            if (index != itemsSize - 1) {
                Divider(color = RadixTheme.colors.gray4)
            }
        }
    }
}
