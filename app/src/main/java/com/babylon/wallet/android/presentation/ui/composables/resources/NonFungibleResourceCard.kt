package com.babylon.wallet.android.presentation.ui.composables.resources

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem
import com.babylon.wallet.android.presentation.ui.composables.ImageSize
import com.babylon.wallet.android.presentation.ui.composables.rememberImageUrl

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun NonFungibleResourceCollectionHeader(
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
            if (collection.items.size >= 2) {
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

            if (collection.items.isNotEmpty()) {
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

        Card(
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
                Image(
                    painter = rememberAsyncImagePainter(
                        model = rememberImageUrl(fromUrl = collection.iconUrl?.toString().orEmpty(), size = ImageSize.SMALL),
                        placeholder = painterResource(id = R.drawable.img_placeholder),
                        error = painterResource(id = R.drawable.img_placeholder)
                    ),
                    contentDescription = "Nft icon",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RadixTheme.shapes.roundedRectSmall)
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

// Not implemented yet
//                    Text(
//                        stringResource(
//                            id = R.string.assetDetails_NFTDetails_ownedOfTotal,
//                            collection.items.size,
//                            circulation
//                        ),
//                        style = RadixTheme.typography.body2HighImportance,
//                        color = RadixTheme.colors.gray2,
//                    )
                }
            }
        }
    }
}

@Composable
fun NonFungibleResourceItemCard(
    modifier: Modifier = Modifier,
    itemIndex: Int,
    allItemsSize: Int,
    content: @Composable () -> Unit
) {
    androidx.compose.material3.Card(
        modifier = modifier,
        shape = nftItemShape(itemIndex = itemIndex, allItemsSize = allItemsSize),
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
private fun nftItemShape(
    itemIndex: Int,
    allItemsSize: Int
) = if (itemIndex == allItemsSize - 1) {
    RadixTheme.shapes.roundedRectBottomMedium
} else {
    RectangleShape
}

@Preview(showBackground = true)
@Composable
fun CollapsableParentItemPreview() {
    RadixWalletTheme {
        NonFungibleResourceCollectionHeader(
            modifier = Modifier.padding(all = 30.dp),
            collection = Resource.NonFungibleResource(
                resourceAddress = "resource_rdx_abcde",
                amount = 1,
                nameMetadataItem = NameMetadataItem(name = "Crypto Punks"),
                items = listOf(
                    Resource.NonFungibleResource.Item(
                        collectionAddress = "resource_rdx_abcde",
                        localId = Resource.NonFungibleResource.Item.ID.from("#1#"),
                        iconMetadataItem = null
                    ),
                    Resource.NonFungibleResource.Item(
                        collectionAddress = "resource_rdx_abcde",
                        localId = Resource.NonFungibleResource.Item.ID.from("#2#"),
                        iconMetadataItem = null
                    ),
                    Resource.NonFungibleResource.Item(
                        collectionAddress = "resource_rdx_abcde",
                        localId = Resource.NonFungibleResource.Item.ID.from("#3#"),
                        iconMetadataItem = null
                    )
                )
            ),
            collapsed = false
        ) { }
    }
}

@Preview(showBackground = true)
@Composable
fun ExpandedParentItemPreview() {
    RadixWalletTheme {
        NonFungibleResourceCollectionHeader(
            modifier = Modifier.padding(all = 16.dp),
            collection = Resource.NonFungibleResource(
                resourceAddress = "resource_rdx_abcde",
                amount = 1,
                nameMetadataItem = NameMetadataItem(name = "Crypto Punks"),
                items = listOf(
                    Resource.NonFungibleResource.Item(
                        collectionAddress = "resource_rdx_abcde",
                        localId = Resource.NonFungibleResource.Item.ID.from("#1#"),
                        iconMetadataItem = null
                    ),
                    Resource.NonFungibleResource.Item(
                        collectionAddress = "resource_rdx_abcde",
                        localId = Resource.NonFungibleResource.Item.ID.from("#2#"),
                        iconMetadataItem = null
                    ),
                    Resource.NonFungibleResource.Item(
                        collectionAddress = "resource_rdx_abcde",
                        localId = Resource.NonFungibleResource.Item.ID.from("#3#"),
                        iconMetadataItem = null
                    )
                )
            ),
            collapsed = true
        ) { }
    }
}

@Preview(showBackground = true)
@Composable
fun ExpandedParentItemPreviewWithTwo() {
    RadixWalletTheme {
        NonFungibleResourceCollectionHeader(
            modifier = Modifier.padding(all = 16.dp),
            collection = Resource.NonFungibleResource(
                resourceAddress = "resource_rdx_abcde",
                amount = 1,
                nameMetadataItem = NameMetadataItem(name = "Crypto Punks"),
                items = listOf(
                    Resource.NonFungibleResource.Item(
                        collectionAddress = "resource_rdx_abcde",
                        localId = Resource.NonFungibleResource.Item.ID.from("#1#"),
                        iconMetadataItem = null
                    ),
                    Resource.NonFungibleResource.Item(
                        collectionAddress = "resource_rdx_abcde",
                        localId = Resource.NonFungibleResource.Item.ID.from("#2#"),
                        iconMetadataItem = null
                    )
                )
            ),
            collapsed = true
        ) { }
    }
}

@Preview(fontScale = 2f, showBackground = true)
@Composable
fun CollapsableParentItemWithLargeFontPreview() {
    RadixWalletTheme {
        NonFungibleResourceCollectionHeader(
            modifier = Modifier.padding(all = 16.dp),
            collection = Resource.NonFungibleResource(
                resourceAddress = "resource_rdx_abcde",
                amount = 1,
                nameMetadataItem = NameMetadataItem(name = "Crypto Punks"),
                items = listOf(
                    Resource.NonFungibleResource.Item(
                        collectionAddress = "resource_rdx_abcde",
                        localId = Resource.NonFungibleResource.Item.ID.from("#1#"),
                        iconMetadataItem = null
                    )
                )
            ),
            collapsed = false
        ) { }
    }
}
