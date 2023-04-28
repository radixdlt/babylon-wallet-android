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
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.presentation.model.NftCollectionUiModel
import com.babylon.wallet.android.presentation.model.toNftUiModel

@Suppress("MutableParams", "UnstableCollections")
@Composable
fun NftListContent(
    collapsedState: SnapshotStateList<Boolean>, // TODO use an immutable object!
    items: List<NftCollectionUiModel>,
    onNftClick: (NftCollectionUiModel, NftCollectionUiModel.NftItemUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier) {
        items.forEachIndexed { i, dataItem ->
            val collapsed = collapsedState[i]
            item(key = "header_$i") {
                NftTokenHeaderItem(
                    nftImageUrl = dataItem.iconUrl,
                    nftName = dataItem.name,
                    nftsInCirculation = "?",
                    nftsInPossession = dataItem.nfts.size.toString(),
                    nftChildCount = dataItem.nfts.size,
                    collapsed = collapsed
                ) {
                    collapsedState[i] = !collapsed
                }
            }
            items(
                dataItem.nfts,
                key = { nft -> nft.displayAddress }
            ) { item ->
                AnimatedVisibility(
                    visible = !collapsed,
                    enter = expandVertically(),
                    exit = shrinkVertically(animationSpec = tween(150))
                ) {
                    var bottomCornersRounded = false
                    if (dataItem.nfts.last() == item) {
                        bottomCornersRounded = true
                    }
                    NftTokenDetailItem(
                        nftId = item.localId,
                        imageUrl = item.nftImage.orEmpty(),
                        bottomCornersRounded = bottomCornersRounded,
                        nftMetadata = item.nftsMetadata,
                        onNftClick = { nftId ->
                            onNftClick(dataItem, dataItem.nfts.first { it.localId == nftId })
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
        val mockNftUiList = SampleDataProvider().mockNftUiList
        NftListContent(
            items = emptyList(),
            collapsedState = remember(mockNftUiList) { mockNftUiList.map { true }.toMutableStateList() },
            onNftClick = { _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NftTokenListPreview() {
    RadixWalletTheme {
        val mockNftUiList = SampleDataProvider().mockNftUiList
        NftListContent(
            items = mockNftUiList.toNftUiModel(),
            collapsedState = remember(mockNftUiList) { mockNftUiList.map { true }.toMutableStateList() },
            onNftClick = { _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NftTokenListExpandedPreview() {
    RadixWalletTheme {
        val mockNftUiList = SampleDataProvider().mockNftUiList
        NftListContent(
            items = mockNftUiList.toNftUiModel(),
            collapsedState = remember(mockNftUiList) { mockNftUiList.map { false }.toMutableStateList() },
            onNftClick = { _, _ -> }
        )
    }
}
