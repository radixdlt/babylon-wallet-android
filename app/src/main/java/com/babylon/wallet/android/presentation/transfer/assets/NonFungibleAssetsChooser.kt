package com.babylon.wallet.android.presentation.transfer.assets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.presentation.transfer.SpendingAsset
import com.babylon.wallet.android.presentation.ui.composables.ImageSize
import com.babylon.wallet.android.presentation.ui.composables.NftTokenHeaderItem
import com.babylon.wallet.android.presentation.ui.composables.applyImageAspectRatio
import com.babylon.wallet.android.presentation.ui.composables.rememberImageUrl

@Composable
fun NonFungibleAssetsChooser(
    modifier: Modifier = Modifier,
    resources: List<Resource.NonFungibleResource>,
    selectedAssets: Set<SpendingAsset>,
    onAssetSelectionChanged: (SpendingAsset, Boolean) -> Unit
) {
    val collapsedState = remember(resources) { resources.map { true }.toMutableStateList() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = RadixTheme.dimensions.paddingDefault),
        contentPadding = PaddingValues(vertical = RadixTheme.dimensions.paddingDefault)
    ) {
        resources.forEachIndexed { collectionIndex, collection ->
            val collapsed = collapsedState[collectionIndex]
            item(key = collection.resourceAddress) {
                NftTokenHeaderItem(
                    modifier = Modifier.padding(
                        top = if (collectionIndex == 0) 0.dp else RadixTheme.dimensions.paddingDefault,
                        bottom = 1.dp
                    ),
                    nftImageUrl = collection.iconUrl.toString(),
                    nftName = collection.name,
                    nftsInCirculation = collection.amount.toString(),
                    nftsInPossession = collection.items.size.toString(),
                    nftChildCount = collection.items.size,
                    collapsed = collapsed
                ) {
                    collapsedState[collectionIndex] = !collapsed
                }
            }

            itemsIndexed(
                items = collection.items,
                key = { _, item -> item.globalAddress }
            ) { itemIndex, item ->
                AnimatedVisibility(
                    visible = !collapsed,
                    enter = expandVertically(),
                    exit = shrinkVertically(animationSpec = tween(150))
                ) {
                    ItemContainer(
                        modifier = Modifier.padding(vertical = 1.dp),
                        shape = if (itemIndex == collection.items.lastIndex) {
                            RadixTheme.shapes.roundedRectBottomMedium
                        } else {
                            RectangleShape
                        }
                    ) {
                        Item(
                            resource = item,
                            isSelected = selectedAssets.any { it.address == item.globalAddress },
                            onCheckChanged = {
                                val nonFungibleAsset = SpendingAsset.NFT(item = item)
                                onAssetSelectionChanged(nonFungibleAsset, it)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemContainer(
    modifier: Modifier = Modifier,
    shape: Shape,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = shape,
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
private fun Item(
    modifier: Modifier = Modifier,
    resource: Resource.NonFungibleResource.Item,
    isSelected: Boolean,
    onCheckChanged: (Boolean) -> Unit
) {
    Row(
        modifier = modifier
            .clickable {
                onCheckChanged(!isSelected)
            }
            .padding(vertical = RadixTheme.dimensions.paddingDefault)
            .padding(start = RadixTheme.dimensions.paddingDefault, end = RadixTheme.dimensions.paddingSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
        ) {
            if (resource.imageUrl != null) {
                val painter = rememberAsyncImagePainter(
                    model = rememberImageUrl(
                        fromUrl = resource.imageUrl.toString(),
                        size = ImageSize.LARGE
                    ),
                    placeholder = painterResource(id = com.babylon.wallet.android.R.drawable.img_placeholder),
                    error = painterResource(id = com.babylon.wallet.android.R.drawable.img_placeholder)
                )

                AsyncImage(
                    modifier = Modifier
                        .fillMaxWidth()
                        .applyImageAspectRatio(painter = painter)
                        .clip(RadixTheme.shapes.roundedRectMedium)
                        .background(Color.Transparent, RadixTheme.shapes.roundedRectMedium),
                    model = rememberImageUrl(fromUrl = resource.imageUrl.toString(), size = ImageSize.MEDIUM),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(id = com.babylon.wallet.android.R.string.nft_id),
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.gray2
                )

                Text(
                    text = resource.localId,
                    style = RadixTheme.typography.body1HighImportance.copy(
                        textAlign = TextAlign.End
                    ),
                    color = RadixTheme.colors.gray2
                )
            }

//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween
//            ) {
//                Text(
//                    text = "Type",
//                    style = RadixTheme.typography.body1Regular,
//                    color = RadixTheme.colors.gray2
//                )
//
//                Text(
//                    text = "Devin Booker - Dunk",
//                    style = RadixTheme.typography.body1HighImportance.copy(
//                        textAlign = TextAlign.End
//                    ),
//                    color = RadixTheme.colors.gray1
//                )
//            }
        }

        Checkbox(
            checked = isSelected,
            onCheckedChange = onCheckChanged,
            colors = CheckboxDefaults.colors(
                checkedColor = RadixTheme.colors.gray1,
                uncheckedColor = RadixTheme.colors.gray2,
                checkmarkColor = Color.White
            )
        )
    }
}
