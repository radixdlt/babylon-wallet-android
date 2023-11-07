package com.babylon.wallet.android.presentation.ui.composables.assets

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.assets.Assets
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.presentation.account.composable.EmptyResourcesContent
import com.babylon.wallet.android.presentation.transfer.assets.ResourceTab
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer

fun LazyListScope.nftsTab(
    assets: Assets,
    collapsedState: SnapshotStateList<Boolean>,
    onNonFungibleItemClick: (Resource.NonFungibleResource, Resource.NonFungibleResource.Item) -> Unit
) {
    if (assets.nonFungibles.isEmpty()) {
        item {
            EmptyResourcesContent(
                modifier = Modifier.fillMaxWidth(),
                tab = ResourceTab.Nfts
            )
        }
    }

    assets.nonFungibles.forEachIndexed { collectionIndex, collection ->
        item(
            key = collection.resourceAddress,
            contentType = { "collection" }
        ) {
            val isCollapsed = collapsedState[collectionIndex]
            NonFungibleResourceCollectionHeader(
                modifier = Modifier
                    .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                    .padding(bottom = if (isCollapsed) RadixTheme.dimensions.paddingDefault else 1.dp),
                collection = collection,
                collapsed = isCollapsed,
                parentSectionClick = {
                    collapsedState[collectionIndex] = !isCollapsed
                }
            )
        }

        items(
            count = if (collapsedState[collectionIndex]) 0 else collection.amount.toInt(),
            key = { index -> "${collection.resourceAddress}$index" },
            contentType = { "nft" }
        ) { index ->
            val isLastIndex = index == collection.amount.toInt() - 1

            val bottomCorners by animateDpAsState(
                targetValue = if (isLastIndex) 12.dp else 0.dp,
                label = "bottomCorners"
            )

            Card(
                modifier = Modifier
                    .padding(top = 1.dp, bottom = if (isLastIndex) RadixTheme.dimensions.paddingDefault else 0.dp)
                    .padding(horizontal = RadixTheme.dimensions.paddingDefault)
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
                val nft = collection.items.getOrNull(index)
                if (nft != null) {
                    NonFungibleResourceItem(
                        modifier = Modifier
                            .padding(RadixTheme.dimensions.paddingDefault)
                            .clickable {
                                onNonFungibleItemClick(collection, nft)
                            },
                        item = nft
                    )
                } else {
                    NonFungibleResourcePlaceholder(
                        modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun NonFungibleResourceCollectionHeader(
    modifier: Modifier = Modifier,
    collection: Resource.NonFungibleResource,
    cardHeight: Dp = 103.dp,
    collapsed: Boolean = false,
    groupInnerPadding: Dp = 6.dp,
    parentSectionClick: () -> Unit,
) {
    val bottomCorners = if (collapsed) 12.dp else 0.dp
    val cardShape = RoundedCornerShape(12.dp, 12.dp, bottomCorners, bottomCorners)
    Box(
        modifier = modifier
    ) {
        if (collapsed) {
            if (collection.amount >= 2) {
                val scaleFactor = 0.8f
                val topOffset = cardHeight * (1 - scaleFactor) + groupInnerPadding + groupInnerPadding * scaleFactor
                Surface(
                    modifier = Modifier
                        .padding(top = topOffset)
                        .fillMaxWidth()
                        .height(cardHeight)
                        .scale(scaleFactor, scaleFactor),
                    shape = RadixTheme.shapes.roundedRectMedium,
                    color = Color.White,
                    elevation = 2.dp,
                    content = {}
                )
            }

            if (collection.amount > 0) {
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

        androidx.compose.material.Card(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(cardHeight)
                .clickable { parentSectionClick() },
            shape = cardShape,
            backgroundColor = Color.White,
            elevation = 4.dp,
            onClick = parentSectionClick
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = RadixTheme.dimensions.paddingLarge),
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
}

@Composable
private fun NonFungibleResourceItem(
    modifier: Modifier = Modifier,
    item: Resource.NonFungibleResource.Item,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
    ) {
        Thumbnail.NFT(
            modifier = Modifier.fillMaxWidth(),
            nft = item
        )
        item.nameMetadataItem?.name?.let { name ->
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
