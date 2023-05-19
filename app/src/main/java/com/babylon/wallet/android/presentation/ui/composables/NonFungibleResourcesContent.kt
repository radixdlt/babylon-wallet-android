package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.Resource

@Suppress("MutableParams", "UnstableCollections")
@Composable
fun NonFungibleResourcesContent(
    items: List<Resource.NonFungibleResource>,
    onNftClick: (Resource.NonFungibleResource, Resource.NonFungibleResource.Item) -> Unit,
    modifier: Modifier = Modifier,
) {
    val collapsedState = remember(items) { items.map { true }.toMutableStateList() }
    LazyColumn(modifier) {
        items.forEachIndexed { i, nft ->
            val collapsed = collapsedState[i]
            item(key = "header_$i") {
                NftTokenHeaderItem(
                    nftImageUrl = nft.iconUrl.toString(),
                    nftName = nft.name,
                    nftsInCirculation = "?",
                    nftsInPossession = nft.items.size.toString(),
                    nftChildCount = nft.items.size,
                    collapsed = collapsed
                ) {
                    collapsedState[i] = !collapsed
                }
            }
            items(
                nft.items,
                key = { item -> item.globalAddress }
            ) { item ->
                AnimatedVisibility(
                    visible = !collapsed,
                    enter = expandVertically(),
                    exit = shrinkVertically(animationSpec = tween(150))
                ) {
                    var bottomCornersRounded = false
                    if (nft.items.last() == item) {
                        bottomCornersRounded = true
                    }
                    NftTokenDetailItem(
                        item = item,
                        bottomCornersRounded = bottomCornersRounded,
                        onItemClicked = {
                            onNftClick(nft, item)
                        }
                    )
                }
            }
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
            items = emptyList(),
            onNftClick = { _, _ -> }
        )
    }
}
