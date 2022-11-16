package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.data.mockdata.mockNftUiList
import com.babylon.wallet.android.presentation.model.NftClassUi

@Suppress("MutableParams", "UnstableCollections")
@Composable
fun CollapsableLazyColumn(
    collapsedState: SnapshotStateList<Boolean>, // TODO use an immutable object!
    sections: List<NftClassUi>,
    modifier: Modifier = Modifier
) {

    LazyColumn(modifier) {
        sections.forEachIndexed { i, dataItem ->
            val collapsed = collapsedState[i]
            item(key = "header_$i") {
                CollapsableParentItemView(
                    nftImageUrl = dataItem.iconUrl,
                    nftName = dataItem.name,
                    nftsInCirculation = dataItem.nftsInCirculation,
                    nftsInPossession = dataItem.nftsInPossession,
                    nftChildCount = dataItem.nft.size,
                    collapsed = collapsed,
                    arrowText = if (collapsed)
                        stringResource(id = R.string.show_plus)
                    else
                        stringResource(id = R.string.hide_minus)
                ) {
                    collapsedState[i] = !collapsed
                }
            }
            if (!collapsed) {
                items(
                    dataItem.nft,
                    key = { nft: NftClassUi.NftUi -> nft.id }
                ) { row ->
                    var bottomCornersRounded = false
                    if (dataItem.nft.last() == row) {
                        bottomCornersRounded = true
                    }
                    CollapsableChildItemView(
                        nftId = row.id,
                        imageUrl = row.imageUrl,
                        bottomCornersRounded = bottomCornersRounded,
                        nftMetadata = row.nftsMetadata
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CollapsableLazyColumnPreview() {
    CollapsableLazyColumn(
        sections = mockNftUiList,
        collapsedState = remember(mockNftUiList) { mockNftUiList.map { true }.toMutableStateList() }
    )
}

@Preview(showBackground = true)
@Composable
fun ExpandedLazyColumnPreview() {
    CollapsableLazyColumn(
        sections = mockNftUiList,
        collapsedState = remember(mockNftUiList) { mockNftUiList.map { false }.toMutableStateList() }
    )
}

@Preview(showBackground = true, fontScale = 2f)
@Composable
fun ExpandedLazyColumnWithLargeFontPreview() {
    CollapsableLazyColumn(
        sections = mockNftUiList,
        collapsedState = remember(mockNftUiList) { mockNftUiList.map { false }.toMutableStateList() }
    )
}
