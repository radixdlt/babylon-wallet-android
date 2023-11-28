package com.babylon.wallet.android.presentation.ui.composables.assets

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.assets.Assets
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.presentation.account.composable.EmptyResourcesContent
import com.babylon.wallet.android.presentation.transfer.assets.ResourceTab
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import rdx.works.core.displayableQuantity

fun LazyListScope.tokensTab(
    assets: Assets,
    action: AssetsViewAction
) {
    if (assets.fungiblesSize() == 0) {
        item {
            EmptyResourcesContent(
                modifier = Modifier.fillMaxWidth(),
                tab = ResourceTab.Tokens
            )
        }
    }

    item {
        val xrdResource = remember(assets.ownedXrd) { assets.ownedXrd }
        if (xrdResource != null) {
            AssetCard(
                modifier = Modifier
                    .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                    .padding(top = RadixTheme.dimensions.paddingSemiLarge)
            ) {
                FungibleResourceItem(
                    resource = xrdResource,
                    action = action
                )
            }
        }
    }

    itemsIndexed(
        items = assets.ownedNonXrdFungibles,
        key = { _, resource -> resource.resourceAddress },
        itemContent = { index, resource ->
            AssetCard(
                modifier = Modifier
                    .padding(
                        top = if (index == 0) RadixTheme.dimensions.paddingSemiLarge else 0.dp
                    )
                    .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                itemIndex = index,
                allItemsSize = assets.ownedNonXrdFungibles.size
            ) {
                FungibleResourceItem(
                    resource = resource,
                    action = action
                )

                if (index != assets.ownedNonXrdFungibles.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = RadixTheme.colors.gray4
                    )
                }
            }
        }
    )
}

@Composable
private fun FungibleResourceItem(
    resource: Resource.FungibleResource,
    modifier: Modifier = Modifier,
    action: AssetsViewAction
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .throttleClickable {
                when (action) {
                    is AssetsViewAction.Click -> {
                        action.onFungibleClick(resource)
                    }

                    is AssetsViewAction.Selection -> {
                        action.onFungibleCheckChanged(resource, !action.isSelected(resource.resourceAddress))
                    }
                }
            }
            .fillMaxWidth()
            .padding(
                horizontal = RadixTheme.dimensions.paddingDefault,
                vertical = RadixTheme.dimensions.paddingLarge
            ),
    ) {
        Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingMedium))
        Thumbnail.Fungible(
            modifier = Modifier.size(44.dp),
            token = resource
        )
        Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingMedium))
        Text(
            modifier = Modifier.weight(1f),
            text = resource.displayTitle,
            style = RadixTheme.typography.body2HighImportance,
            color = RadixTheme.colors.gray1,
            maxLines = 1
        )
        Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingMedium))

        resource.ownedAmount?.let { amount ->
            Text(
                text = amount.displayableQuantity(),
                style = RadixTheme.typography.secondaryHeader,
                color = RadixTheme.colors.gray1,
                maxLines = 2
            )
        }

        if (action is AssetsViewAction.Selection) {
            val isSelected = remember(resource, action) {
                action.isSelected(resource.resourceAddress)
            }
            AssetsViewCheckBox(
                isSelected = isSelected,
                onCheckChanged = { isChecked ->
                    action.onFungibleCheckChanged(resource, isChecked)
                }
            )
        } else {
            Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingMedium))
        }
    }
}
