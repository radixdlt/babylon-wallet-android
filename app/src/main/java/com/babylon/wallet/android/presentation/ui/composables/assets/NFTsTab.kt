package com.babylon.wallet.android.presentation.ui.composables.assets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.assets.Assets
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.presentation.account.composable.EmptyResourcesContent
import com.babylon.wallet.android.presentation.transfer.assets.ResourceTab
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer

fun LazyListScope.nftsTab(
    assets: Assets,
    collapsibleAssetsState: SnapshotStateMap<String, Boolean>,
    action: AssetsViewAction
) {
    if (assets.nftsSize() == 0) {
        item {
            EmptyResourcesContent(
                modifier = Modifier.fillMaxWidth(),
                tab = ResourceTab.Nfts
            )
        }
    }

    assets.ownedNonFungibles.forEach { collection ->
        item(
            key = collection.resourceAddress,
            contentType = { "collection" }
        ) {
            NFTHeader(collapsibleAssetsState, collection)
        }

        items(
            count = if (collapsibleAssetsState[collection.resourceAddress] == false) collection.amount.toInt() else 0,
            key = { index -> "${collection.resourceAddress}$index" },
            contentType = { "nft" }
        ) { index ->
            NFTItem(index, collection, action)
        }
    }
}

@Composable
private fun NFTItem(
    index: Int,
    collection: Resource.NonFungibleResource,
    action: AssetsViewAction
) {
    AssetCard(
        modifier = Modifier
            .padding(top = 1.dp)
            .padding(horizontal = RadixTheme.dimensions.paddingDefault),
        itemIndex = index,
        allItemsSize = collection.amount.toInt(),
        roundTopCorners = false
    ) {
        val nft = collection.items.getOrNull(index)
        if (nft != null) {
            NonFungibleResourceItem(
                collection = collection,
                item = nft,
                action = action
            )
        } else {
            LaunchedEffect(index, collection.items.size) {
                // First shimmering item
                if (index == collection.items.size) {
                    action.onNextNFtsPageRequest(collection)
                }
            }

            NonFungibleResourcePlaceholder(
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault)
            )
        }
    }
}

@Composable
private fun NFTHeader(
    collapsibleAssetsState: SnapshotStateMap<String, Boolean>,
    collection: Resource.NonFungibleResource
) {
    val isCollapsed = collapsibleAssetsState[collection.resourceAddress] ?: true
    CollapsibleAssetCard(
        modifier = Modifier
            .padding(horizontal = RadixTheme.dimensions.paddingDefault)
            .padding(top = RadixTheme.dimensions.paddingSemiLarge),
        isCollapsed = isCollapsed,
        collapsedItems = collection.amount.toInt()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    collapsibleAssetsState[collection.resourceAddress] = !isCollapsed
                }
                .padding(RadixTheme.dimensions.paddingLarge),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
        ) {
            Thumbnail.NonFungible(
                modifier = Modifier.size(44.dp),
                collection = collection,
                shape = RadixTheme.shapes.roundedRectSmall
            )
            Column(verticalArrangement = Arrangement.Center) {
                if (collection.name.isNotEmpty()) {
                    Text(
                        collection.name,
                        style = RadixTheme.typography.secondaryHeader,
                        color = RadixTheme.colors.gray1,
                        maxLines = 2
                    )
                }

                collection.currentSupply?.let { currentSupply ->
                    Text(
                        stringResource(
                            id = R.string.assetDetails_NFTDetails_ownedOfTotal,
                            collection.items.size,
                            currentSupply
                        ),
                        style = RadixTheme.typography.body2HighImportance,
                        color = RadixTheme.colors.gray2,
                    )
                }
            }
        }
    }
}

@Composable
private fun NonFungibleResourceItem(
    modifier: Modifier = Modifier,
    collection: Resource.NonFungibleResource,
    item: Resource.NonFungibleResource.Item,
    action: AssetsViewAction
) {
    Row(
        modifier = modifier
            .throttleClickable {
                when (action) {
                    is AssetsViewAction.Click -> {
                        action.onNonFungibleItemClick(collection, item)
                    }

                    is AssetsViewAction.Selection -> {
                        action.onNFTCheckChanged(collection, item, !action.isSelected(item.globalAddress))
                    }
                }
            }
            .padding(RadixTheme.dimensions.paddingDefault),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
        ) {
            Thumbnail.NFT(
                modifier = Modifier.fillMaxWidth(),
                nft = item
            )
            item.name?.let { name ->
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = name,
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.gray1
                )
            }

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = item.localId.displayable,
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.gray2
            )
        }

        if (action is AssetsViewAction.Selection) {
            val isSelected = remember(item, action) {
                action.isSelected(item.globalAddress)
            }
            AssetsViewCheckBox(
                isSelected = isSelected,
                onCheckChanged = { isChecked ->
                    action.onNFTCheckChanged(collection, item, isChecked)
                }
            )
        }
    }
}

@Composable
private fun NonFungibleResourcePlaceholder(
    modifier: Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(Thumbnail.NFTAspectRatio)
                .placeholder(
                    visible = true,
                    color = RadixTheme.colors.gray4,
                    shape = RoundedCornerShape(Thumbnail.NFTCornerRadius),
                    highlight = PlaceholderHighlight.shimmer(highlightColor = Color.White)
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(23.dp)
                .placeholder(
                    visible = true,
                    color = RadixTheme.colors.gray4,
                    shape = RoundedCornerShape(Thumbnail.NFTCornerRadius),
                    highlight = PlaceholderHighlight.shimmer(highlightColor = Color.White)
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize(0.5f)
                .height(23.dp)
                .placeholder(
                    visible = true,
                    color = RadixTheme.colors.gray4,
                    shape = RoundedCornerShape(Thumbnail.NFTCornerRadius),
                    highlight = PlaceholderHighlight.shimmer(highlightColor = Color.White)
                )
        )
    }
}
