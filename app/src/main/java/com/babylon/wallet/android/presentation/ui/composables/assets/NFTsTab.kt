package com.babylon.wallet.android.presentation.ui.composables.assets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.account.composable.EmptyResourcesContent
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.model.displaySubtitle
import com.babylon.wallet.android.presentation.model.displayTitle
import com.babylon.wallet.android.presentation.transfer.assets.AssetsTab
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.card.CollapsibleCommonCard
import com.babylon.wallet.android.presentation.ui.composables.card.CommonCard
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.formatted
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.samples.sampleMainnet
import rdx.works.core.domain.assets.NonFungibleCollection
import rdx.works.core.domain.resources.Resource

fun LazyListScope.nftsTab(
    assetsViewData: AssetsViewData,
    state: AssetsViewState,
    action: AssetsViewAction,
    onInfoClick: (GlossaryItem) -> Unit
) {
    if (assetsViewData.isNonFungibleCollectionsEmpty) {
        item {
            EmptyResourcesContent(
                modifier = Modifier.fillMaxWidth(),
                tab = AssetsTab.Nfts,
                onInfoClick = onInfoClick
            )
        }
    }

    item {
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))
    }
    assetsViewData.nonFungibleCollections.forEachIndexed { outerIndex, nonFungible ->
        item(
            key = nonFungible.collection.address.string,
            contentType = { "collection" }
        ) {
            NFTHeader(
                collection = nonFungible,
                state = state,
                action = action,
                modifier = Modifier.padding(top = if (outerIndex == 0) 0.dp else RadixTheme.dimensions.paddingLarge)
            )
        }

        items(
            count = if (!state.isCollapsed(nonFungible.collection.address.string)) nonFungible.collection.amount.toInt() else 0,
            key = { index -> "${nonFungible.collection.address}$index" },
            contentType = { "nft" }
        ) { index ->
            NFTItem(index, nonFungible.collection, state, action)
        }
    }
}

@Composable
private fun NFTItem(
    index: Int,
    collection: Resource.NonFungibleResource,
    state: AssetsViewState,
    action: AssetsViewAction
) {
    CommonCard(
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
            LaunchedEffect(collection.address, state.fetchingNFTsPerCollection) {
                if (collection.address !in state.fetchingNFTsPerCollection) {
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
    collection: NonFungibleCollection,
    state: AssetsViewState,
    action: AssetsViewAction,
    modifier: Modifier = Modifier
) {
    val isCollapsed = state.isCollapsed(collection.resource.address.string)
    CollapsibleCommonCard(
        modifier = modifier
            .padding(horizontal = RadixTheme.dimensions.paddingDefault),
        isCollapsed = isCollapsed,
        collapsedItems = collection.resource.amount.toInt()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    action.onCollectionClick(collection.resource.address.string)
                }
                .padding(RadixTheme.dimensions.paddingLarge),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
        ) {
            Thumbnail.NonFungible(
                modifier = Modifier.size(44.dp),
                collection = collection.resource
            )
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = collection.displayTitle(),
                    style = RadixTheme.typography.secondaryHeader,
                    color = RadixTheme.colors.text,
                    maxLines = 2
                )

                Text(
                    text = collection.displaySubtitle(),
                    style = RadixTheme.typography.body2HighImportance,
                    color = RadixTheme.colors.textSecondary,
                )
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
                        action.onNFTCheckChanged(collection, item, !action.isSelected(item.globalId))
                    }
                }
            }
            .padding(horizontal = RadixTheme.dimensions.paddingDefault, vertical = RadixTheme.dimensions.paddingLarge),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Thumbnail.NFT(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = RadixTheme.dimensions.paddingDefault),
                nft = item
            )
            item.name?.let { name ->
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = RadixTheme.dimensions.paddingXXSmall),
                    text = name,
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.text
                )
            }

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = item.localId.formatted(),
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.textSecondary
            )
        }

        if (action is AssetsViewAction.Selection) {
            val isSelected = remember(item, action) {
                action.isSelected(item.globalId)
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
                .radixPlaceholder(
                    visible = true,
                    shape = RoundedCornerShape(Thumbnail.NFTCornerRadius)
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(23.dp)
                .radixPlaceholder(
                    visible = true,
                    shape = RoundedCornerShape(Thumbnail.NFTCornerRadius)
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize(0.5f)
                .height(23.dp)
                .radixPlaceholder(
                    visible = true,
                    shape = RoundedCornerShape(Thumbnail.NFTCornerRadius)
                )
        )
    }
}

@Preview
@UsesSampleValues
@Composable
fun NFTCItemCollapsedPreview() {
    RadixWalletTheme {
        LazyColumn {
            nftsTab(
                assetsViewData = previewAssetViewData,
                state = AssetsViewState(AssetsTab.Nfts, mapOf(ResourceAddress.sampleMainnet().string to true), emptySet()),
                action = AssetsViewAction.Click(
                    onTabClick = {},
                    onCollectionClick = {},
                    onNextNFtsPageRequest = {},
                    onStakesRequest = {},
                    onNonFungibleItemClick = { _, _ -> },
                    onFungibleClick = {},
                    onLSUClick = {},
                    onPoolUnitClick = {},
                    onClaimClick = {}
                ),
                onInfoClick = {}
            )
        }
    }
}

@Preview
@UsesSampleValues
@Composable
fun NFTItemExpandedPreview() {
    RadixWalletTheme {
        LazyColumn {
            nftsTab(
                assetsViewData = previewAssetViewData,
                state = AssetsViewState(AssetsTab.Nfts, mapOf(ResourceAddress.sampleMainnet().string to false), emptySet()),
                action = AssetsViewAction.Click(
                    onTabClick = {},
                    onCollectionClick = {},
                    onNextNFtsPageRequest = {},
                    onStakesRequest = {},
                    onNonFungibleItemClick = { _, _ -> },
                    onFungibleClick = {},
                    onLSUClick = {},
                    onPoolUnitClick = {},
                    onClaimClick = {}
                ),
                onInfoClick = {}
            )
        }
    }
}
