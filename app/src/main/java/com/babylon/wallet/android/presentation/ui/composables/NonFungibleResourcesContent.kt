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
    onNftClick: (Resource.NonFungibleResource, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val collapsedState = remember(items) { items.map { true }.toMutableStateList() }
    LazyColumn(modifier) {
        items.forEachIndexed { i, dataItem ->
            val collapsed = collapsedState[i]
            item(key = "header_$i") {
//                NftTokenHeaderItem(
//                    nftImageUrl = dataItem.iconUrl,
//                    nftName = dataItem.name,
//                    nftsInCirculation = "?",
//                    nftsInPossession = dataItem.nftIds.size.toString(),
//                    nftChildCount = dataItem.nftIds.size,
//                    collapsed = collapsed
//                ) {
//                    collapsedState[i] = !collapsed
//                }
            }
            items(
                dataItem.nftIds,
                key = { address -> address }
            ) { nftId ->
                AnimatedVisibility(
                    visible = !collapsed,
                    enter = expandVertically(),
                    exit = shrinkVertically(animationSpec = tween(150))
                ) {
                    var bottomCornersRounded = false
                    if (dataItem.nftIds.last() == nftId) {
                        bottomCornersRounded = true
                    }
//                    NftTokenDetailItem(
//                        nftId = nftId,
//                        imageUrl = nftId.nftImage.orEmpty(),
//                        bottomCornersRounded = bottomCornersRounded,
//                        nftMetadata = nftId.nftsMetadata,
//                        onNftClick = {
//                            onNftClick(dataItem, nftId)
//                        }
//                    )
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
