package com.babylon.wallet.android.presentation.ui.composables

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
import com.babylon.wallet.android.data.mockdata.mockNftUiList
import com.babylon.wallet.android.designsystem.theme.BabylonWalletTheme
import com.babylon.wallet.android.presentation.model.NftUiModel

@Suppress("MutableParams", "UnstableCollections")
@Composable
fun NftTokenList(
    collapsedState: SnapshotStateList<Boolean>, // TODO use an immutable object!
    item: List<NftUiModel>,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier) {
        item.forEachIndexed { i, dataItem ->
            val collapsed = collapsedState[i]
            item(key = "header_$i") {
                NftTokenHeaderItem(
                    nftImageUrl = dataItem.iconUrl,
                    nftName = dataItem.name,
                    nftsInCirculation = "?",
                    nftsInPossession = "?",
                    nftChildCount = dataItem.nft.size,
                    collapsed = collapsed
                ) {
                    collapsedState[i] = !collapsed
                }
            }
            if (!collapsed) {
                items(
                    dataItem.nft,
                    key = { nft -> nft.id }
                ) { item ->
                    var bottomCornersRounded = false
                    if (dataItem.nft.last() == item) {
                        bottomCornersRounded = true
                    }
                    NftTokenDetailItem(
                        nftId = item.id,
                        imageUrl = item.imageUrl,
                        bottomCornersRounded = bottomCornersRounded,
                        nftMetadata = item.nftsMetadata
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
fun NftTokenListPreview() {
    BabylonWalletTheme {
        NftTokenList(
            item = mockNftUiList,
            collapsedState = remember(mockNftUiList) { mockNftUiList.map { true }.toMutableStateList() }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NftTokenListExpandedPreview() {
    BabylonWalletTheme {
        NftTokenList(
            item = mockNftUiList,
            collapsedState = remember(mockNftUiList) { mockNftUiList.map { false }.toMutableStateList() }
        )
    }
}
