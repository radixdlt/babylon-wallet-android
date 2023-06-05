package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.Resources

@Suppress("MutableParams", "UnstableCollections")
@Composable
fun NonFungibleResourcesContent(
    resources: Resources?,
    onNftClick: (Resource.NonFungibleResource, Resource.NonFungibleResource.Item) -> Unit,
    modifier: Modifier = Modifier,
) {
    val collections = resources?.nonFungibleResources.orEmpty()
    val collapsedState = remember(collections) {
        collections.map { true }.toMutableStateList()
    }

    LazyColumn(modifier) {
        collections.forEachIndexed { i, collection ->
            val collapsed = collapsedState[i]
            item(key = "header_$i") {
                NftTokenHeaderItem(
                    modifier = Modifier
                        .padding(horizontal = 20.dp),
                    nftImageUrl = collection.iconUrl.toString(),
                    nftName = collection.name,
                    nftsInCirculation = "?",
                    nftsInPossession = collection.items.size.toString(),
                    nftChildCount = collection.items.size,
                    collapsed = collapsed
                ) {
                    collapsedState[i] = !collapsed
                }
            }
            items(
                collection.items,
                key = { item -> item.globalAddress }
            ) { item ->
                AnimatedVisibility(
                    visible = !collapsed,
                    enter = expandVertically(),
                    exit = shrinkVertically(animationSpec = tween(150))
                ) {
                    var bottomCornersRounded = false
                    if (collection.items.last() == item) {
                        bottomCornersRounded = true
                    }
                    NftTokenDetailItem(
                        item = item,
                        bottomCornersRounded = bottomCornersRounded,
                        onItemClicked = {
                            onNftClick(collection, item)
                        }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(if (collapsed) 0.dp else RadixTheme.dimensions.paddingDefault)) }
        }
        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NftTokenListEmpty() {
    RadixWalletTheme {
        NonFungibleResourcesContent(
            resources = Resources.EMPTY,
            onNftClick = { _, _ -> }
        )
    }
}
